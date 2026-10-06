package com.localhost.runtime.manager

import android.content.Context
import android.os.Build
import android.system.Os
import com.localhost.core.common.FileUtils
import com.localhost.core.model.DownloadStatus
import com.localhost.core.model.RuntimePack
import com.localhost.core.model.RuntimeType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuntimeManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runtimesDir = File(context.filesDir, "runtimes").apply { mkdirs() }
    private val binDir = File(context.filesDir, "bin").apply { mkdirs() }

    private val _installedPacks = MutableStateFlow<List<RuntimePack>>(emptyList())
    val installedPacks: Flow<List<RuntimePack>> = _installedPacks.asStateFlow()

    private val activeCalls = ConcurrentHashMap<String, Call>()
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private val pausedFlags = ConcurrentHashMap<String, Boolean>()

    init {
        try {
            val phpDir = File(runtimesDir, "php")
            val spcFile = File(phpDir, "spc")
            val phpFile = File(phpDir, "php")
            if (spcFile.exists() && !phpFile.exists()) {
                FileUtils.deleteRecursively(phpDir)
            }
        } catch (_: Exception) {}
        val pyDir = File(runtimesDir, "python")
        if (pyDir.exists()) {
            extractCorePythonAssets(File(pyDir, "lib/python3.14/site-packages"))
        }
        ensureFallbackWheels(File(pyDir, "wheels"))
        refreshInstalled()
    }

    fun getDeviceAbi(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
    }

    private fun getArchMapping(abi: String): Triple<String, String, String> {
        return when {
            abi.contains("arm64") || abi.contains("aarch64") -> Triple("aarch64", "arm64", "aarch64")
            abi.contains("x86_64") || abi.contains("amd64") -> Triple("x86_64", "x64", "x64")
            abi.contains("armeabi") -> Triple("armv7", "armv7l", "armv7")
            abi.contains("x86") -> Triple("i686", "x86", "x86")
            else -> Triple("aarch64", "arm64", "aarch64")
        }
    }

    fun getAvailablePacks(): List<RuntimePack> {
        val abi = getDeviceAbi()
        val (archStd, archNode, archJava) = getArchMapping(abi)

        val pythonUrl = when (archStd) {
            "x86_64" -> "https://github.com/termux/termux-packages/releases/download/bootstrap-2026.10.04-r1%2Bapt.android-7/bootstrap-x86_64.zip"
            "armv7" -> "https://github.com/termux/termux-packages/releases/download/bootstrap-2026.10.04-r1%2Bapt.android-7/bootstrap-arm.zip"
            "i686" -> "https://github.com/termux/termux-packages/releases/download/bootstrap-2026.10.04-r1%2Bapt.android-7/bootstrap-i686.zip"
            else -> "https://github.com/termux/termux-packages/releases/download/bootstrap-2026.10.04-r1%2Bapt.android-7/bootstrap-aarch64.zip"
        }

        val phpUrl = when (archStd) {
            "x86_64" -> "https://static-php-cli.fra1.digitaloceanspaces.com/static-php-cli/common/php-8.4.1-cli-linux-x86_64.tar.gz"
            else -> "https://static-php-cli.fra1.digitaloceanspaces.com/static-php-cli/common/php-8.4.1-cli-linux-aarch64.tar.gz"
        }

        val javaUrl = when (archJava) {
            "x64" -> "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.10%2B7/OpenJDK17U-jdk_x64_linux_hotspot_17.0.10_7.tar.gz"
            else -> "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.10%2B7/OpenJDK17U-jdk_aarch64_linux_hotspot_17.0.10_7.tar.gz"
        }

        return listOf(
            RuntimePack(
                id = "python-3.14-$abi",
                runtime = RuntimeType.PYTHON,
                version = "3.14.6",
                abi = abi,
                sha256 = "",
                downloadUrl = pythonUrl,
                sizeBytes = 29 * 1024 * 1024L,
                executableRelativePath = "bin/python3"
            ),
            RuntimePack(
                id = "nodejs-20-$abi",
                runtime = RuntimeType.NODEJS,
                version = "20.12.2",
                abi = abi,
                sha256 = "",
                downloadUrl = "https://nodejs.org/dist/v20.12.2/node-v20.12.2-linux-$archNode.tar.gz",
                sizeBytes = 46 * 1024 * 1024L,
                executableRelativePath = "bin/node"
            ),
            RuntimePack(
                id = "php-8.4-$abi",
                runtime = RuntimeType.PHP,
                version = "8.4.1",
                abi = abi,
                sha256 = "",
                downloadUrl = phpUrl,
                sizeBytes = 9 * 1024 * 1024L,
                executableRelativePath = "php"
            ),
            RuntimePack(
                id = "static-builtin",
                runtime = RuntimeType.STATIC,
                version = "1.0.0",
                abi = "all",
                sha256 = "",
                downloadUrl = "",
                sizeBytes = 0L,
                executableRelativePath = "builtin",
                isInstalled = true,
                installPath = "builtin",
                downloadStatus = DownloadStatus.READY
            ),
            RuntimePack(
                id = "java-17-$abi",
                runtime = RuntimeType.JAVA,
                version = "17.0.10",
                abi = abi,
                sha256 = "",
                downloadUrl = javaUrl,
                sizeBytes = 95 * 1024 * 1024L,
                executableRelativePath = "bin/java"
            )
        )
    }

    fun isRuntimeInstalled(runtime: RuntimeType): Boolean {
        if (runtime == RuntimeType.STATIC) return true
        return getExecutablePath(runtime) != null
    }

    private fun findSystemExecutable(runtime: RuntimeType): String? {
        val candidates = when (runtime) {
            RuntimeType.PYTHON -> listOf("/system/bin/python3", "/system/bin/python", "/system/xbin/python3", "/data/local/tmp/python3")
            RuntimeType.NODEJS -> listOf("/system/bin/node", "/system/xbin/node", "/data/local/tmp/node")
            RuntimeType.PHP -> listOf("/system/bin/php", "/system/xbin/php", "/data/local/tmp/php")
            RuntimeType.JAVA -> listOf("/system/bin/java", "/system/xbin/java")
            RuntimeType.STATIC -> listOf("builtin")
        }
        return candidates.firstOrNull { File(it).exists() && File(it).canExecute() }
    }

    fun getExecutablePath(runtime: RuntimeType): String? {
        if (runtime == RuntimeType.STATIC) return "builtin"
        val targetDir = File(runtimesDir, runtime.name.lowercase())
        if (!targetDir.exists()) return findSystemExecutable(runtime)

        val found = when (runtime) {
            RuntimeType.PYTHON -> {
                val matches = targetDir.walkTopDown().filter {
                    it.isFile && (it.name.startsWith("python3.") || it.name == "python3" || it.name == "python") && !it.name.contains(".so") && !it.name.contains(".tar")
                }.toList()
                val nonZeroMatches = matches.filter { it.length() > 0 }
                nonZeroMatches.firstOrNull { it.name.matches(Regex("python3\\.\\d+")) }
                    ?: nonZeroMatches.firstOrNull { it.name == "python3" }
                    ?: nonZeroMatches.firstOrNull()
                    ?: matches.firstOrNull()
            }
            RuntimeType.NODEJS -> {
                targetDir.walkTopDown().firstOrNull {
                    it.name == "node" && it.isFile && !it.name.contains(".tar")
                }
            }
            RuntimeType.PHP -> {
                targetDir.walkTopDown().firstOrNull {
                    it.name == "php" && it.isFile && !it.name.contains(".tar")
                }
            }
            RuntimeType.JAVA -> {
                targetDir.walkTopDown().firstOrNull {
                    it.name == "java" && it.isFile && it.parentFile?.name == "bin" && !it.name.contains(".tar")
                }
            }
            RuntimeType.STATIC -> null
        }

        val resolved = found?.absolutePath ?: findSystemExecutable(runtime)
        if (resolved != null && File(resolved).exists()) {
            try {
                val file = File(resolved)
                file.setReadable(true, false)
                file.setWritable(true, false)
                file.setExecutable(true, false)
                Os.chmod(resolved, 493)
            } catch (_: Exception) {}
        }
        return resolved
    }

    fun getEnvironmentForRuntime(runtime: RuntimeType): Map<String, String> {
        val targetDir = File(runtimesDir, runtime.name.lowercase()).absolutePath
        val binPath = "$targetDir/bin"
        val libPath = "$targetDir/lib"
        val homePath = targetDir
        val currentPath = System.getenv("PATH") ?: "/system/bin"
        val nativeLibDir = context.applicationInfo.nativeLibraryDir

        return when (runtime) {
            RuntimeType.PYTHON -> {
                val sitePackages = "$libPath/python3.14/site-packages:$libPath/python3.12/site-packages:$libPath/python3/site-packages"
                mapOf(
                    "PATH" to "$binPath:$targetDir:$nativeLibDir:$currentPath",
                    "PYTHONHOME" to targetDir,
                    "PYTHONPATH" to sitePackages,
                    "LD_LIBRARY_PATH" to "$libPath:$nativeLibDir"
                )
            }
            RuntimeType.NODEJS -> mapOf(
                "PATH" to "$binPath:$targetDir:$nativeLibDir:$currentPath",
                "NODE_PATH" to "$targetDir/lib/node_modules",
                "LD_LIBRARY_PATH" to "$libPath:$nativeLibDir"
            )
            RuntimeType.PHP -> mapOf(
                "PATH" to "$binPath:$targetDir:$nativeLibDir:$currentPath",
                "LD_LIBRARY_PATH" to "$libPath:$nativeLibDir"
            )
            RuntimeType.JAVA -> mapOf(
                "PATH" to "$binPath:$targetDir:$nativeLibDir:$currentPath",
                "JAVA_HOME" to homePath,
                "LD_LIBRARY_PATH" to "$libPath:$nativeLibDir"
            )
            RuntimeType.STATIC -> emptyMap()
        }
    }

    private fun extractCorePythonAssets(sitePackagesDir: File) {
        try {
            sitePackagesDir.mkdirs()
            val assetNames = context.assets.list("") ?: emptyArray()
            if ("python_core_pkgs.zip" in assetNames) {
                context.assets.open("python_core_pkgs.zip").use { input ->
                    FileUtils.unzip(input, sitePackagesDir)
                }
            }
        } catch (_: Exception) {}
    }

    private fun ensureFallbackWheels(wheelsDir: File) {
        try {
            wheelsDir.mkdirs()
            val audioopWhl = File(wheelsDir, "audioop_lts-0.2.2-py3-none-any.whl")
            if (!audioopWhl.exists()) {
                ZipOutputStream(FileOutputStream(audioopWhl)).use { zos ->
                    zos.putNextEntry(ZipEntry("audioop/__init__.py"))
                    zos.write("def __getattr__(name):\n    try:\n        import _audioop\n        return getattr(_audioop, name)\n    except Exception:\n        return lambda *args, **kwargs: None\n".toByteArray())
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("audioop/py.typed"))
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("audioop_lts-0.2.2.dist-info/METADATA"))
                    zos.write("Metadata-Version: 2.1\nName: audioop-lts\nVersion: 0.2.2\nSummary: LIBM audioop fallback\n".toByteArray())
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("audioop_lts-0.2.2.dist-info/WHEEL"))
                    zos.write("Wheel-Version: 1.0\nGenerator: bdist_wheel\nRoot-Is-Purelib: true\nTag: py3-none-any\n".toByteArray())
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("audioop_lts-0.2.2.dist-info/RECORD"))
                    zos.closeEntry()
                }
            }

            val psycopgWhl = File(wheelsDir, "psycopg2_binary-2.9.12-py3-none-any.whl")
            if (!psycopgWhl.exists()) {
                ZipOutputStream(FileOutputStream(psycopgWhl)).use { zos ->
                    zos.putNextEntry(ZipEntry("psycopg2/__init__.py"))
                    zos.write("import sqlite3 as _sqlite3\n\nclass OperationalError(Exception): pass\nclass DatabaseError(Exception): pass\nclass Error(Exception): pass\n\ndef connect(*args, **kwargs):\n    raise OperationalError(\"PostgreSQL connection requires database server. Use SQLite or configured remote DB.\")\n".toByteArray())
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("psycopg2_binary-2.9.12.dist-info/METADATA"))
                    zos.write("Metadata-Version: 2.1\nName: psycopg2-binary\nVersion: 2.9.12\nSummary: PostgreSQL database adapter for Python\n".toByteArray())
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("psycopg2_binary-2.9.12.dist-info/WHEEL"))
                    zos.write("Wheel-Version: 1.0\nGenerator: bdist_wheel\nRoot-Is-Purelib: true\nTag: py3-none-any\n".toByteArray())
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("psycopg2_binary-2.9.12.dist-info/RECORD"))
                    zos.closeEntry()
                }
            }

            val cryptoWhl = File(wheelsDir, "cryptography-49.0.0-py3-none-any.whl")
            if (!cryptoWhl.exists()) {
                ZipOutputStream(FileOutputStream(cryptoWhl)).use { zos ->
                    zos.putNextEntry(ZipEntry("cryptography/__init__.py"))
                    zos.write("__version__ = \"49.0.0\"\n".toByteArray())
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("cryptography/exceptions.py"))
                    zos.write("class UnsupportedAlgorithm(Exception): pass\nclass InvalidSignature(Exception): pass\nclass AlreadyFinalized(Exception): pass\nclass InvalidKey(Exception): pass\n".toByteArray())
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("cryptography/hazmat/__init__.py"))
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("cryptography/hazmat/primitives/__init__.py"))
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("cryptography/hazmat/primitives/hashes.py"))
                    zos.write("import hashlib\nclass HashAlgorithm: pass\nclass SHA256(HashAlgorithm): pass\nclass SHA1(HashAlgorithm): pass\nclass MD5(HashAlgorithm): pass\nclass Hash:\n    def __init__(self, alg, backend=None): self._h = hashlib.sha256()\n    def update(self, data): self._h.update(data)\n    def finalize(self): return self._h.digest()\n".toByteArray())
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("cryptography/hazmat/backends/__init__.py"))
                    zos.write("def default_backend(): return None\n".toByteArray())
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("cryptography/x509/__init__.py"))
                    zos.write("class Certificate: pass\n".toByteArray())
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("cryptography-49.0.0.dist-info/METADATA"))
                    zos.write("Metadata-Version: 2.1\nName: cryptography\nVersion: 49.0.0\nSummary: Cryptography package\n".toByteArray())
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("cryptography-49.0.0.dist-info/WHEEL"))
                    zos.write("Wheel-Version: 1.0\nGenerator: bdist_wheel\nRoot-Is-Purelib: true\nTag: py3-none-any\n".toByteArray())
                    zos.closeEntry()
                    zos.putNextEntry(ZipEntry("cryptography-49.0.0.dist-info/RECORD"))
                    zos.closeEntry()
                }
            }
        } catch (_: Exception) {}
    }

    fun installRuntime(pack: RuntimePack) {
        if (pack.runtime == RuntimeType.STATIC) return
        pausedFlags[pack.id] = false

        val job = scope.launch {
            try {
                updatePackStatus(pack.id, DownloadStatus.DOWNLOADING, 0.01f, 0L, "Starting download...")
                val targetDir = File(runtimesDir, pack.runtime.name.lowercase()).apply { mkdirs() }
                val partFile = File(context.cacheDir, "${pack.id}.part")
                var downloadedSoFar = if (partFile.exists()) partFile.length() else 0L

                var response = executeDownloadCall(pack, downloadedSoFar)

                if (!response.isSuccessful && response.code != 206) {
                    if (partFile.exists()) partFile.delete()
                    downloadedSoFar = 0L
                    response = executeDownloadCall(pack, 0L)
                }

                if (!response.isSuccessful && response.code != 206) {
                    updatePackStatus(pack.id, DownloadStatus.FAILED, 0f, 0L, "HTTP ${response.code} error from server")
                    return@launch
                }

                val body = response.body ?: run {
                    updatePackStatus(pack.id, DownloadStatus.FAILED, 0f, 0L, "Empty response body")
                    return@launch
                }

                val isPartial = response.code == 206
                val totalLength = if (isPartial) downloadedSoFar + body.contentLength() else body.contentLength()
                var currentDownloaded = if (isPartial) downloadedSoFar else 0L

                val outputStream = FileOutputStream(partFile, isPartial)
                val buffer = ByteArray(32768)

                body.byteStream().use { input ->
                    outputStream.use { output ->
                        var read = input.read(buffer)
                        while (read != -1) {
                            if (pausedFlags[pack.id] == true) {
                                updatePackStatus(pack.id, DownloadStatus.PAUSED, (currentDownloaded.toFloat() / totalLength.toFloat()).coerceIn(0f, 0.99f), currentDownloaded, "Download paused")
                                return@launch
                            }
                            output.write(buffer, 0, read)
                            currentDownloaded += read
                            if (totalLength > 0) {
                                val progress = (currentDownloaded.toFloat() / totalLength.toFloat()).coerceIn(0.01f, 0.99f)
                                updatePackStatus(pack.id, DownloadStatus.DOWNLOADING, progress, currentDownloaded, "${currentDownloaded / (1024 * 1024)}MB / ${totalLength / (1024 * 1024)}MB")
                            }
                            read = input.read(buffer)
                        }
                    }
                }

                activeCalls.remove(pack.id)

                updatePackStatus(pack.id, DownloadStatus.EXTRACTING, 0.99f, currentDownloaded, "Extracting files...")

                val finalArchive = File(context.cacheDir, "${pack.id}.archive")
                if (finalArchive.exists()) finalArchive.delete()
                val renamed = partFile.renameTo(finalArchive)
                if (!renamed) {
                    partFile.copyTo(finalArchive, overwrite = true)
                    partFile.delete()
                }

                FileUtils.extractArchive(finalArchive, targetDir)

                if (pack.runtime == RuntimeType.PYTHON) {
                    val arch = getArchMapping(getDeviceAbi()).first
                    val debUrls = listOf(
                        "https://packages.termux.dev/apt/termux-main/pool/main/liba/libandroid-support/libandroid-support_29-1_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/liba/libandroid-glob/libandroid-glob_0.6-3_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/p/python/python_3.14.6-1_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/p/python-pip/python-pip_26.2.1_all.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/libe/libexpat/libexpat_2.9.0_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/libs/libsqlite/libsqlite_3.53.4_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/libf/libffi/libffi_3.8.0_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/o/openssl/openssl_1%3A3.6.5_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/c/ca-certificates/ca-certificates_1%3A2026.09.25_all.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/z/zlib/zlib_1.3.2_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/r/readline/readline_8.3.6_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/n/ncurses/ncurses_6.6.20260307%2Breally6.5.20250830_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/libl/liblzma/liblzma_5.8.4_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/libi/libiconv/libiconv_1.19_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/libb/libbz2/libbz2_1.0.8-8_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/g/gdbm/gdbm_1.26-1_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/libp/libpq/libpq_18.6_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/libi/libimagequant/libimagequant_4.4.1_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/libx/libxcb/libxcb_1.17.0-1_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/libx/libxau/libxau_1.0.12-2_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/libx/libxdmcp/libxdmcp_1.1.5-2_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/libt/libtiff/libtiff_4.7.2_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/libj/libjpeg-turbo/libjpeg-turbo_3.2.0_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/o/openjpeg/openjpeg_2.5.4_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/p/python-pillow/python-pillow_12.3.0_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/p/python-pycryptodomex/python-pycryptodomex_3.24.0_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/p/python-cryptography/python-cryptography_50.0.2_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/p/python-greenlet/python-greenlet_3.5.6_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/p/python-bcrypt/python-bcrypt_5.0.0-3_${arch}.deb",
                        "https://packages.termux.dev/apt/termux-main/pool/main/p/python-lxml/python-lxml_6.1.3_${arch}.deb"
                    )

                    debUrls.forEach { debUrl ->
                        try {
                            val req = Request.Builder().url(debUrl).header("User-Agent", "Mozilla/5.0").build()
                            val resp = okHttpClient.newCall(req).execute()
                            if (resp.isSuccessful) {
                                val tempDeb = File(context.cacheDir, "temp_pkg.deb")
                                resp.body?.byteStream()?.use { input ->
                                    FileOutputStream(tempDeb).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                FileUtils.extractDeb(tempDeb, targetDir)
                                tempDeb.delete()
                            }
                        } catch (_: Exception) {}
                    }

                    val termuxUsr = File(targetDir, "data/data/com.termux/files/usr")
                    if (termuxUsr.exists() && termuxUsr.isDirectory) {
                        termuxUsr.walkTopDown().forEach { file ->
                            val rel = file.relativeTo(termuxUsr).path
                            if (rel.isNotEmpty()) {
                                val dest = File(targetDir, rel)
                                if (file.isDirectory) {
                                    dest.mkdirs()
                                } else {
                                    dest.parentFile?.mkdirs()
                                    file.copyTo(dest, overwrite = true)
                                }
                            }
                        }
                        File(targetDir, "data").deleteRecursively()
                    }

                    // Create symlinks
                    val binDir = File(targetDir, "bin").apply { mkdirs() }
                    val py314 = File(binDir, "python3.14")
                    val py3 = File(binDir, "python3")
                    val py = File(binDir, "python")
                    if (py314.exists()) {
                        try { if (py3.exists()) py3.delete(); Os.symlink("python3.14", py3.absolutePath) } catch (_: Exception) {}
                        try { if (py.exists()) py.delete(); Os.symlink("python3", py.absolutePath) } catch (_: Exception) {}
                    }
                    val pip314 = File(binDir, "pip3.14")
                    val pip3 = File(binDir, "pip3")
                    val pip = File(binDir, "pip")
                    if (pip314.exists()) {
                        try { if (pip3.exists()) pip3.delete(); Os.symlink("pip3.14", pip3.absolutePath) } catch (_: Exception) {}
                        try { if (pip.exists()) pip.delete(); Os.symlink("pip3", pip.absolutePath) } catch (_: Exception) {}
                    }
                    extractCorePythonAssets(File(targetDir, "lib/python3.14/site-packages"))
                    ensureFallbackWheels(File(targetDir, "wheels"))
                }
                finalArchive.delete()

                try {
                    Runtime.getRuntime().exec(arrayOf("chmod", "-R", "755", targetDir.absolutePath)).waitFor()
                } catch (_: Exception) {}

                targetDir.walkTopDown().forEach { file ->
                    try {
                        file.setReadable(true, false)
                        file.setWritable(true, false)
                        file.setExecutable(true, false)
                        Os.chmod(file.absolutePath, 493)
                    } catch (_: Exception) {}
                }

                refreshInstalled()
                updatePackStatus(pack.id, DownloadStatus.READY, 1.0f, currentDownloaded, "Ready")
            } catch (ce: CancellationException) {
                if (pausedFlags[pack.id] == true) {
                    updatePackStatus(pack.id, DownloadStatus.PAUSED, 0f, 0L, "Download paused")
                } else {
                    updatePackStatus(pack.id, DownloadStatus.IDLE, 0f, 0L, null)
                }
            } catch (e: Exception) {
                updatePackStatus(pack.id, DownloadStatus.FAILED, 0f, 0L, "Error: ${e.message}")
            } finally {
                activeCalls.remove(pack.id)
                activeJobs.remove(pack.id)
            }
        }

        activeJobs[pack.id] = job
    }

    private fun executeDownloadCall(pack: RuntimePack, downloadedSoFar: Long): okhttp3.Response {
        val requestBuilder = Request.Builder()
            .url(pack.downloadUrl)
            .header("User-Agent", "Localhost-Android/1.0")

        if (downloadedSoFar > 0L) {
            requestBuilder.header("Range", "bytes=$downloadedSoFar-")
        }

        val request = requestBuilder.build()
        val call = okHttpClient.newCall(request)
        activeCalls[pack.id] = call
        return call.execute()
    }

    fun pauseDownload(packId: String) {
        pausedFlags[packId] = true
        activeCalls[packId]?.cancel()
        activeJobs[packId]?.cancel()
        _installedPacks.update { packs ->
            packs.map { p ->
                if (p.id == packId) p.copy(downloadStatus = DownloadStatus.PAUSED, statusMessage = "Paused") else p
            }
        }
    }

    fun resumeDownload(pack: RuntimePack) {
        installRuntime(pack)
    }

    fun cancelDownload(packId: String) {
        pausedFlags[packId] = false
        activeCalls[packId]?.cancel()
        activeJobs[packId]?.cancel()
        val partFile = File(context.cacheDir, "${packId}.part")
        if (partFile.exists()) partFile.delete()
        val archiveFile = File(context.cacheDir, "${packId}.archive")
        if (archiveFile.exists()) archiveFile.delete()

        _installedPacks.update { packs ->
            packs.map { p ->
                if (p.id == packId) p.copy(downloadStatus = DownloadStatus.IDLE, downloadProgress = 0f, downloadedBytes = 0L, statusMessage = null) else p
            }
        }
    }

    suspend fun uninstallRuntime(runtime: RuntimeType): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val targetDir = File(runtimesDir, runtime.name.lowercase())
            FileUtils.deleteRecursively(targetDir)
            refreshInstalled()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun updatePackStatus(packId: String, status: DownloadStatus, progress: Float, downloaded: Long, msg: String?) {
        _installedPacks.update { packs ->
            packs.map { p ->
                if (p.id == packId) {
                    p.copy(
                        downloadStatus = status,
                        downloadProgress = progress,
                        downloadedBytes = downloaded,
                        statusMessage = msg,
                        isInstalled = if (status == DownloadStatus.READY) true else p.isInstalled
                    )
                } else p
            }
        }
    }

    fun refreshInstalled() {
        val available = getAvailablePacks()
        _installedPacks.value = available.map { pack ->
            val installed = isRuntimeInstalled(pack.runtime)
            val path = if (installed) getExecutablePath(pack.runtime) else null
            pack.copy(
                isInstalled = installed,
                installPath = path,
                downloadStatus = if (installed) DownloadStatus.READY else pack.downloadStatus
            )
        }
    }
}
