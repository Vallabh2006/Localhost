package com.localhost.runtime.manager

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ComposerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val runtimeManager: RuntimeManager
) {
    val binDir: File
        get() = File(context.filesDir, "bin").apply { mkdirs() }

    val composerFile: File
        get() = File(binDir, "composer.phar")

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress = _downloadProgress.asStateFlow()

    fun isInstalled(): Boolean = composerFile.exists() && composerFile.length() > 100_000L

    suspend fun downloadComposer(): Result<File> = withContext(Dispatchers.IO) {
        if (isInstalled()) return@withContext Result.success(composerFile)

        _isDownloading.value = true
        _downloadProgress.value = 0f

        val url = "https://getcomposer.org/download/latest-stable/composer.phar"
        val tempFile = File(binDir, "composer.phar.tmp")

        try {
            val req = Request.Builder().url(url).build()
            val resp = okHttpClient.newCall(req).execute()

            if (!resp.isSuccessful) {
                _isDownloading.value = false
                return@withContext Result.failure(Exception("HTTP ${resp.code} downloading composer.phar"))
            }

            val body = resp.body ?: throw Exception("Empty response body")
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L

            body.byteStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            _downloadProgress.value = downloadedBytes.toFloat() / totalBytes
                        }
                    }
                }
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                if (composerFile.exists()) composerFile.delete()
                tempFile.renameTo(composerFile)
                composerFile.setReadable(true, false)
                composerFile.setExecutable(true, false)
                _isDownloading.value = false
                _downloadProgress.value = 1f
                Result.success(composerFile)
            } else {
                _isDownloading.value = false
                Result.failure(Exception("Downloaded file is empty"))
            }
        } catch (e: Exception) {
            _isDownloading.value = false
            Result.failure(e)
        }
    }

    fun buildComposerCommand(projectDir: File, vararg args: String): List<String>? {
        val phpExec = runtimeManager.getExecutablePath(com.localhost.core.model.RuntimeType.PHP) ?: return null
        if (!isInstalled()) return null
        return listOf(phpExec, composerFile.absolutePath) + args.toList()
    }
}
