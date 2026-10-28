package com.localhost.cli

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.localhost.core.common.FileUtils
import com.localhost.core.data.repository.LogRepository
import com.localhost.core.data.repository.ProjectRepository
import com.localhost.core.model.EnvironmentVar
import com.localhost.core.model.Project
import com.localhost.core.model.ProjectStatus
import com.localhost.core.model.RuntimeType
import com.localhost.runtime.manager.RuntimeManager
import com.localhost.runtime.process.ProcessSupervisor
import com.localhost.tunnel.CloudflareTunnelManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class LocalhostCli @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository,
    private val logRepository: LogRepository,
    private val processSupervisor: ProcessSupervisor,
    private val tunnelManager: CloudflareTunnelManager,
    private val runtimeManager: RuntimeManager,
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { prettyPrint = true }
    private val rootDirectory: File = context.filesDir.apply { mkdirs() }
    private var currentDirectory: File = rootDirectory
    private var previousDirectory: File = rootDirectory
    private val commandHistory = mutableListOf<String>()
    private var activeSubProcess: Process? = null
    private var activeVenvDir: File? = null

    fun getActiveVenvName(): String? = activeVenvDir?.name

    fun getCurrentPromptPrefix(): String {
        val path = getCurrentPath()
        val venv = activeVenvDir?.name
        return if (venv != null) "($venv) $path" else path
    }

    fun getCurrentPath(): String {
        val rootPath = rootDirectory.canonicalPath
        val curPath = currentDirectory.canonicalPath
        return if (curPath == rootPath) {
            "/"
        } else if (curPath.startsWith(rootPath)) {
            val rel = curPath.removePrefix(rootPath)
            if (rel.startsWith("/")) rel else "/$rel"
        } else {
            curPath
        }
    }

    fun interrupt() {
        try {
            activeSubProcess?.destroyForcibly()
            activeSubProcess = null
        } catch (_: Exception) {}
    }

    suspend fun executeInDirectory(
        workDir: File,
        commandLine: String,
        onOutputChunk: ((String) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        val prevCurrent = currentDirectory
        val prevActiveVenv = activeVenvDir
        try {
            currentDirectory = workDir
            val detectedVenv = listOf(
                File(workDir, ".venv"),
                File(workDir, "myenv"),
                File(workDir, "venv")
            ).firstOrNull { File(it, "bin/python").exists() }
            if (detectedVenv != null) {
                activeVenvDir = detectedVenv
            }
            execute(commandLine, onOutputChunk)
        } finally {
            currentDirectory = prevCurrent
            activeVenvDir = prevActiveVenv
        }
    }

    suspend fun execute(commandLine: String, onOutputChunk: ((String) -> Unit)? = null): String = withContext(Dispatchers.IO) {
        val trimmed = commandLine.trim()
        if (trimmed.isEmpty()) return@withContext ""
        commandHistory.add(trimmed)

        if (trimmed.contains(">")) {
            val res = handleRedirection(trimmed)
            if (res != null) return@withContext res
        }

        val isJson = trimmed.contains("--json")
        val cleanCmd = trimmed.replace("--json", "").trim()
        val tokens = parseArgs(cleanCmd)
        if (tokens.isEmpty()) return@withContext ""

        val rawRoot = tokens[0]
        val root = if (rawRoot.startsWith("-") && rawRoot.length > 1) rawRoot.removePrefix("-").lowercase() else rawRoot.lowercase()
        val args = tokens.drop(1)

        try {
            when (root) {
                "help", "man" -> getHelpText()
                "pwd" -> getCurrentPath()
                "cd" -> handleCd(args.firstOrNull())
                "ls", "dir" -> handleLs(args)
                "tree" -> handleTree(args)
                "cat" -> handleCat(args)
                "head" -> handleHead(args)
                "tail" -> handleTail(args)
                "stat" -> handleStat(args)
                "wc" -> handleWc(args)
                "mkdir" -> handleMkdir(args)
                "rmdir" -> handleRmdir(args)
                "touch" -> handleTouch(args)
                "cp" -> handleCp(args)
                "mv" -> handleMv(args)
                "rm" -> handleRm(args)
                "grep" -> handleGrep(args)
                "find" -> handleFind(args)
                "echo" -> args.joinToString(" ")
                "clear", "cls" -> "__CLEAR__"
                "history" -> commandHistory.mapIndexed { idx, cmd -> "${idx + 1}  $cmd" }.joinToString("\n")
                "df" -> handleDf()
                "du" -> handleDu(args.firstOrNull())
                "uname" -> "Linux localhost ${Build.VERSION.RELEASE} ${Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"}"
                "whoami" -> "localhost"
                "date" -> SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US).format(Date())
                "which" -> handleWhich(args.firstOrNull())
                "sleep" -> {
                    val sec = args.firstOrNull()?.toLongOrNull() ?: 1L
                    delay(sec * 1000)
                    ""
                }
                "list", "ps" -> handleList(isJson)
                "status" -> handleStatus(isJson)
                "start" -> handleStart(args.firstOrNull(), isJson)
                "stop" -> handleStop(args.firstOrNull(), isJson)
                "restart" -> handleRestart(args.firstOrNull(), isJson)
                "logs" -> handleLogs(args.firstOrNull(), isJson)
                "create" -> handleCreate(args, isJson)
                "delete" -> handleDeleteProject(args.firstOrNull())
                "tunnel" -> handleTunnel(args.firstOrNull(), isJson)
                "curl", "wget" -> handleCurl(args)
                "chmod" -> handleChmod(args)
                "source", "." -> handleSource(args)
                "activate" -> handleActivate(args)
                "deactivate" -> handleDeactivate()
                "composer" -> handleComposer(args, onOutputChunk)
                "java" -> handleRuntimeExec(RuntimeType.JAVA, args, onOutputChunk)
                "python", "python3" -> handleRuntimeExec(RuntimeType.PYTHON, args, onOutputChunk)
                "pip", "pip3" -> handleRuntimeExec(RuntimeType.PYTHON, listOf("-m", "pip") + args, onOutputChunk)
                "node" -> handleRuntimeExec(RuntimeType.NODEJS, args, onOutputChunk)
                "npm" -> handleRuntimeExec(RuntimeType.NODEJS, listOf("npm") + args, onOutputChunk)
                "npx" -> handleRuntimeExec(RuntimeType.NODEJS, listOf("npx") + args, onOutputChunk)
                "php" -> handleRuntimeExec(RuntimeType.PHP, args, onOutputChunk)
                "exec" -> handleCustomExec(args, onOutputChunk)
                else -> handleFallbackExec(root, args, onOutputChunk)
            }
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    private fun handleCd(target: String?): String {
        if (target == null || target == "~" || target == "/" || target.isBlank()) {
            previousDirectory = currentDirectory
            currentDirectory = rootDirectory
            return ""
        }
        if (target == "-") {
            val temp = currentDirectory
            currentDirectory = previousDirectory
            previousDirectory = temp
            return getCurrentPath()
        }

        val destination = if (target.startsWith("/")) {
            File(rootDirectory, target.removePrefix("/"))
        } else {
            File(currentDirectory, target)
        }

        val canonical = destination.canonicalFile
        return if (canonical.exists() && canonical.isDirectory) {
            previousDirectory = currentDirectory
            currentDirectory = canonical
            ""
        } else {
            "cd: no such file or directory: $target"
        }
    }

    private fun handleLs(args: List<String>): String {
        val isFlag = { s: String -> s.startsWith("-") || s == "a" || s == "l" || s == "la" || s == "al" || s == "lh" }
        val showAll = args.any { it.contains("a") || it == "a" }
        val longFormat = args.any { it.contains("l") || it == "l" }
        val targetPath = args.lastOrNull { !isFlag(it) }

        val targetDir = if (targetPath == null) currentDirectory else resolveFile(targetPath)
        if (!targetDir.exists()) return "ls: cannot access '$targetPath': No such file or directory"
        if (targetDir.isFile) {
            return if (longFormat) formatFileDetail(targetDir) else targetDir.name
        }

        val files = targetDir.listFiles() ?: return ""
        val sorted = files.filter { showAll || !it.name.startsWith(".") }.sortedBy { it.name.lowercase() }

        return if (longFormat) {
            val totalBlocks = sorted.sumOf { it.length() } / 1024
            val lines = mutableListOf("total $totalBlocks")
            sorted.forEach { lines.add(formatFileDetail(it)) }
            lines.joinToString("\n")
        } else {
            sorted.joinToString("  ") { f ->
                if (f.isDirectory) "${f.name}/" else f.name
            }
        }
    }

    private fun formatFileDetail(f: File): String {
        val perms = (if (f.isDirectory) "d" else "-") +
                (if (f.canRead()) "r" else "-") +
                (if (f.canWrite()) "w" else "-") +
                (if (f.canExecute()) "x" else "-") + "r--r--"
        val size = String.format(Locale.US, "%8d", f.length())
        val date = SimpleDateFormat("MMM dd HH:mm", Locale.US).format(Date(f.lastModified()))
        val name = if (f.isDirectory) "${f.name}/" else f.name
        return "$perms 1 localhost localhost $size $date $name"
    }

    private fun handleTree(args: List<String>): String {
        val showAll = args.contains("-a")
        val maxDepth = args.indexOf("-L").takeIf { it != -1 && it + 1 < args.size }?.let { args[it + 1].toIntOrNull() } ?: 3
        val sb = StringBuilder()
        sb.append(getCurrentPath()).append("\n")

        fun walk(dir: File, prefix: String, depth: Int) {
            if (depth > maxDepth) return
            val files = dir.listFiles()?.filter { showAll || !it.name.startsWith(".") }?.sortedBy { it.name } ?: return
            files.forEachIndexed { index, file ->
                val isLast = index == files.size - 1
                val branch = if (isLast) " " else " "
                sb.append(prefix).append(branch).append(file.name).append(if (file.isDirectory) "/" else "").append("\n")
                if (file.isDirectory) {
                    walk(file, prefix + if (isLast) "    " else "   ", depth + 1)
                }
            }
        }
        walk(currentDirectory, "", 1)
        return sb.toString().trimEnd()
    }

    private fun handleCat(args: List<String>): String {
        if (args.isEmpty()) return "cat: missing file operand"
        val showNumbers = args.contains("-n")
        val files = args.filter { !it.startsWith("-") }
        val sb = StringBuilder()
        files.forEach { filePath ->
            val f = resolveFile(filePath)
            if (!f.exists() || f.isDirectory) {
                sb.append("cat: $filePath: No such file or is a directory\n")
            } else {
                val lines = f.readLines()
                lines.forEachIndexed { idx, line ->
                    if (showNumbers) sb.append(String.format(Locale.US, "%6d  ", idx + 1))
                    sb.append(line).append("\n")
                }
            }
        }
        return sb.toString().trimEnd()
    }

    private fun handleHead(args: List<String>): String {
        val count = args.indexOf("-n").takeIf { it != -1 && it + 1 < args.size }?.let { args[it + 1].toIntOrNull() } ?: 10
        val fileArg = args.lastOrNull { !it.startsWith("-") && it.toIntOrNull() == null } ?: return "head: missing file"
        val f = resolveFile(fileArg)
        if (!f.exists()) return "head: cannot open '$fileArg': No such file"
        return f.bufferedReader().useLines { lines -> lines.take(count).joinToString("\n") }
    }

    private fun handleTail(args: List<String>): String {
        val count = args.indexOf("-n").takeIf { it != -1 && it + 1 < args.size }?.let { args[it + 1].toIntOrNull() } ?: 10
        val fileArg = args.lastOrNull { !it.startsWith("-") && it.toIntOrNull() == null } ?: return "tail: missing file"
        val f = resolveFile(fileArg)
        if (!f.exists()) return "tail: cannot open '$fileArg': No such file"
        val lines = f.readLines()
        return lines.takeLast(count).joinToString("\n")
    }

    private fun handleStat(args: List<String>): String {
        val fileArg = args.firstOrNull() ?: return "stat: missing operand"
        val f = resolveFile(fileArg)
        if (!f.exists()) return "stat: cannot stat '$fileArg': No such file or directory"
        return """
  File: ${f.name}
  Path: ${f.absolutePath}
  Size: ${f.length()} bytes
  Type: ${if (f.isDirectory) "directory" else "regular file"}
Access: (0755/rwxr-xr-x)
Modify: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z", Locale.US).format(Date(f.lastModified()))}
""".trimIndent()
    }

    private fun handleWc(args: List<String>): String {
        val fileArg = args.lastOrNull { !it.startsWith("-") } ?: return "wc: missing file"
        val f = resolveFile(fileArg)
        if (!f.exists()) return "wc: $fileArg: No such file"
        val text = f.readText()
        val lines = text.lines().size - (if (text.endsWith("\n")) 1 else 0)
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val bytes = f.length()
        return "$lines  $words  $bytes  $fileArg"
    }

    private fun handleMkdir(args: List<String>): String {
        if (args.isEmpty()) return "mkdir: missing operand"
        val cleanArgs = args.filter { it != "-p" }
        cleanArgs.forEach { dirPath ->
            val target = resolveFile(dirPath)
            target.mkdirs()
        }
        return ""
    }

    private suspend fun handleRmdir(args: List<String>): String {
        val targets = args.filter { !it.startsWith("-") }
        if (targets.isEmpty()) return "rmdir: missing operand"
        val errors = mutableListOf<String>()
        val existingProjects = try { projectRepository.getAll() } catch (_: Exception) { emptyList() }
        targets.forEach { targetPath ->
            val dir = resolveFile(targetPath)
            if (!dir.exists()) {
                errors.add("rmdir: failed to remove '$targetPath': No such file or directory")
            } else if (!dir.isDirectory) {
                errors.add("rmdir: failed to remove '$targetPath': Not a directory")
            } else {
                val canonical = try { dir.canonicalPath } catch (_: Exception) { dir.absolutePath }
                val deleted = dir.deleteRecursively()
                if (!deleted) {
                    errors.add("rmdir: failed to remove '$targetPath': Failed to remove directory")
                } else {
                    existingProjects.forEach { proj ->
                        val pPath = try { File(proj.workingDir).canonicalPath } catch (_: Exception) { File(proj.workingDir).absolutePath }
                        if (pPath == canonical || pPath.startsWith("$canonical/")) {
                            processSupervisor.stopProject(proj.id)
                            projectRepository.delete(proj.id)
                        }
                    }
                }
            }
        }
        return errors.joinToString("\n")
    }

    private fun handleTouch(args: List<String>): String {
        val targets = args.filter { !it.startsWith("-") }
        if (targets.isEmpty()) return "touch: missing file operand"
        val errors = mutableListOf<String>()
        targets.forEach { filePath ->
            try {
                val f = resolveFile(filePath)
                if (f.exists()) {
                    f.setLastModified(System.currentTimeMillis())
                } else {
                    f.parentFile?.mkdirs()
                    f.createNewFile()
                }
            } catch (e: Exception) {
                errors.add("touch: cannot touch '$filePath': ${e.message}")
            }
        }
        return errors.joinToString("\n")
    }

    private fun handleCp(args: List<String>): String {
        val targets = args.filter { it != "-r" }
        if (targets.size < 2) return "cp: missing destination file operand"
        val src = resolveFile(targets[0])
        val dst = resolveFile(targets[1])
        if (!src.exists()) return "cp: cannot stat '${targets[0]}': No such file or directory"
        if (src.isDirectory) {
            src.copyRecursively(dst, overwrite = true)
        } else {
            src.copyTo(dst, overwrite = true)
        }
        return ""
    }

    private fun handleMv(args: List<String>): String {
        if (args.size < 2) return "mv: missing file operands"
        val src = resolveFile(args[0])
        val dst = resolveFile(args[1])
        if (!src.exists()) return "mv: cannot stat '${args[0]}': No such file or directory"
        src.renameTo(dst)
        return ""
    }

    private suspend fun handleRm(args: List<String>): String {
        val files = args.filter { !it.startsWith("-") }
        if (files.isEmpty()) return "rm: missing operand"
        val existingProjects = try { projectRepository.getAll() } catch (_: Exception) { emptyList() }
        files.forEach { filePath ->
            val f = resolveFile(filePath)
            val canonical = try { f.canonicalPath } catch (_: Exception) { f.absolutePath }
            val wasDir = f.isDirectory
            val deleted = if (wasDir) FileUtils.deleteRecursively(f) else f.delete()
            if (deleted && wasDir) {
                existingProjects.forEach { proj ->
                    val pPath = try { File(proj.workingDir).canonicalPath } catch (_: Exception) { File(proj.workingDir).absolutePath }
                    if (pPath == canonical || pPath.startsWith("$canonical/")) {
                        processSupervisor.stopProject(proj.id)
                        projectRepository.delete(proj.id)
                    }
                }
            }
        }
        return ""
    }

    private fun handleGrep(args: List<String>): String {
        val pattern = args.lastOrNull { !it.startsWith("-") && args.indexOf(it) == args.size - 2 }
            ?: args.firstOrNull { !it.startsWith("-") } ?: return "grep: missing pattern"
        val fileArg = args.lastOrNull { !it.startsWith("-") }
        if (fileArg == null || fileArg == pattern) return "grep: missing file operand"
        val f = resolveFile(fileArg)
        if (!f.exists()) return "grep: $fileArg: No such file"
        val ignoreCase = args.contains("-i")
        val lines = f.readLines()
        val matched = lines.filter { line -> line.contains(pattern, ignoreCase = ignoreCase) }
        return matched.joinToString("\n")
    }

    private fun handleFind(args: List<String>): String {
        val nameIdx = args.indexOf("-name")
        val pattern = if (nameIdx != -1 && nameIdx + 1 < args.size) args[nameIdx + 1] else null
        val results = mutableListOf<String>()

        fun scan(dir: File) {
            dir.listFiles()?.forEach { child ->
                if (pattern == null || child.name.contains(pattern.replace("*", ""), ignoreCase = true)) {
                    val rootPath = rootDirectory.canonicalPath
                    val path = child.canonicalPath
                    val rel = if (path.startsWith(rootPath)) "/" + path.removePrefix(rootPath).removePrefix("/") else path
                    results.add(rel)
                }
                if (child.isDirectory) scan(child)
            }
        }
        scan(currentDirectory)
        return results.joinToString("\n")
    }

    private fun handleDf(): String {
        val stat = StatFs(rootDirectory.absolutePath)
        val total = stat.blockCountLong * stat.blockSizeLong
        val available = stat.availableBlocksLong * stat.blockSizeLong
        val used = total - available
        val percent = if (total > 0) (used * 100 / total).toInt() else 0

        return """
Filesystem      Size  Used  Avail Use% Mounted on
/data           ${formatBytes(total)}  ${formatBytes(used)}  ${formatBytes(available)}  $percent% /
""".trimIndent()
    }

    private fun handleDu(path: String?): String {
        val target = if (path != null) resolveFile(path) else currentDirectory
        val size = FileUtils.calculateDirectorySize(target)
        return "${formatBytes(size)}\t${target.name}"
    }

    private fun handleWhich(binary: String?): String {
        if (binary == null) return ""
        if (binary == "python" || binary == "python3") {
            val venvPython = activeVenvDir?.let { File(it, "bin/python") }?.takeIf { it.exists() }
                ?: listOf(
                    File(currentDirectory, "myenv/bin/python"),
                    File(currentDirectory, ".venv/bin/python"),
                    File(currentDirectory, "venv/bin/python")
                ).firstOrNull { it.exists() }
            if (venvPython != null) return venvPython.absolutePath
        }
        if (binary == "pip" || binary == "pip3") {
            val venvPip = activeVenvDir?.let { File(it, "bin/pip") }?.takeIf { it.exists() }
                ?: listOf(
                    File(currentDirectory, "myenv/bin/pip"),
                    File(currentDirectory, ".venv/bin/pip"),
                    File(currentDirectory, "venv/bin/pip")
                ).firstOrNull { it.exists() }
            if (venvPip != null) return venvPip.absolutePath
        }
        val runtimeType = when (binary.lowercase()) {
            "python", "python3" -> RuntimeType.PYTHON
            "node", "npm" -> RuntimeType.NODEJS
            "php" -> RuntimeType.PHP
            "java" -> RuntimeType.JAVA
            else -> null
        }
        if (runtimeType != null) {
            val exec = runtimeManager.getExecutablePath(runtimeType)
            if (exec != null) return exec
        }
        return "which: no $binary in internal environment"
    }

    private fun handleSource(args: List<String>): String {
        if (args.isEmpty()) return "source: filename argument required\nsource: usage: source filename [arguments]"
        return handleActivate(args)
    }

    private fun handleActivate(args: List<String>): String {
        val arg = args.firstOrNull() ?: ".venv"
        val file = resolveFile(arg)
        val venvDir = when {
            File(file, "bin/python").exists() || File(file, "bin/activate").exists() -> file
            file.name == "activate" && file.parentFile?.name == "bin" -> file.parentFile?.parentFile
            file.name == "bin" && File(file, "activate").exists() -> file.parentFile
            File(file.parentFile, "bin/python").exists() -> file.parentFile
            else -> listOf(
                resolveFile(".venv"),
                resolveFile("myenv"),
                resolveFile("venv"),
                File(currentDirectory, ".venv"),
                File(currentDirectory, "myenv"),
                File(currentDirectory, "venv")
            ).firstOrNull { File(it, "bin/python").exists() || File(it, "bin/activate").exists() }
        }

        if (venvDir != null && venvDir.exists()) {
            activeVenvDir = venvDir
            return "Virtual environment (${venvDir.name}) activated."
        }
        return "Could not find virtual environment at '$arg'"
    }

    private fun handleDeactivate(): String {
        val prev = activeVenvDir?.name
        activeVenvDir = null
        return if (prev != null) "Deactivated virtual environment ($prev)." else "No virtual environment currently active."
    }

    private fun handleRedirection(commandLine: String): String? {
        val append = commandLine.contains(">>")
        val parts = if (append) commandLine.split(">>") else commandLine.split(">")
        if (parts.size != 2) return null
        val cmd = parts[0].trim()
        val destFile = parts[1].trim()
        val target = resolveFile(destFile)

        val output = when (cmd) {
            "pwd" -> getCurrentPath()
            "date" -> SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US).format(Date())
            else -> if (cmd.startsWith("echo ")) cmd.removePrefix("echo ").trim() else return null
        }

        if (append) target.appendText(output + "\n") else target.writeText(output + "\n")
        return ""
    }

    private fun getDefaultTerminalEnvironment(runtime: RuntimeType = RuntimeType.PYTHON): Map<String, String> {
        val env = mutableMapOf<String, String>()
        val baseEnv = runtimeManager.getEnvironmentForRuntime(runtime)
        env.putAll(baseEnv)
        env["PYTHONUNBUFFERED"] = "1"
        val venvDir = activeVenvDir?.takeIf { it.exists() } ?: listOf(
            File(currentDirectory, "myenv"),
            File(currentDirectory, ".venv"),
            File(currentDirectory, "venv")
        ).firstOrNull { File(it, "bin/python").exists() }
        if (venvDir != null) {
            val venvBin = File(venvDir, "bin").absolutePath
            val curPath = env["PATH"] ?: (System.getenv("PATH") ?: "/system/bin")
            env["PATH"] = "$venvBin:$curPath"
            env["VIRTUAL_ENV"] = venvDir.absolutePath
            val sitePkgs = File(venvDir, "lib/python3.14/site-packages")
            if (sitePkgs.exists()) {
                val curPyPath = env["PYTHONPATH"] ?: ""
                env["PYTHONPATH"] = "${sitePkgs.absolutePath}:${currentDirectory.absolutePath}:$curPyPath"
            }
        }
        return env
    }

    private suspend fun handleRuntimeExec(
        runtime: RuntimeType,
        args: List<String>,
        onOutputChunk: ((String) -> Unit)? = null
    ): String {
        val venvPython = if (runtime == RuntimeType.PYTHON) {
            activeVenvDir?.let { File(it, "bin/python") }?.takeIf { it.exists() && it.canExecute() } ?: listOf(
                File(currentDirectory, "myenv/bin/python"),
                File(currentDirectory, ".venv/bin/python"),
                File(currentDirectory, "venv/bin/python")
            ).firstOrNull { it.exists() && it.canExecute() }
        } else null

        val execPath = venvPython?.absolutePath ?: runtimeManager.getExecutablePath(runtime)
            ?: return "${runtime.displayName} runtime is not installed. Download it from Settings."
        val env = getDefaultTerminalEnvironment(runtime)
        val firstArg = args.firstOrNull()
        val fullCmd = if (firstArg == "npm" || firstArg == "npx") {
            val toolFile = File(File(execPath).parentFile, firstArg)
            if (toolFile.exists()) {
                listOf(execPath, toolFile.absolutePath) + args.drop(1)
            } else {
                listOf(firstArg) + args.drop(1)
            }
        } else {
            listOf(execPath) + args
        }
        return runProcess(fullCmd, currentDirectory, env, onOutputChunk)
    }

    private suspend fun handleCustomExec(
        args: List<String>,
        onOutputChunk: ((String) -> Unit)? = null
    ): String {
        if (args.isEmpty()) return "exec: missing executable path"
        val bin = resolveFile(args[0])
        if (!bin.exists()) return "exec: ${args[0]}: No such file"
        bin.setExecutable(true, false)
        return runProcess(listOf(bin.absolutePath) + args.drop(1), currentDirectory, getDefaultTerminalEnvironment(), onOutputChunk)
    }

    private suspend fun handleFallbackExec(
        root: String,
        args: List<String>,
        onOutputChunk: ((String) -> Unit)? = null
    ): String {
        val binInCur = File(currentDirectory, root)
        if (binInCur.exists() && binInCur.canExecute()) {
            return runProcess(listOf(binInCur.absolutePath) + args, currentDirectory, getDefaultTerminalEnvironment(), onOutputChunk)
        }
        val binResolved = resolveFile(root)
        if (binResolved.exists() && binResolved.canExecute()) {
            return runProcess(listOf(binResolved.absolutePath) + args, currentDirectory, getDefaultTerminalEnvironment(), onOutputChunk)
        }
        return "localhost: command not found: $root (type 'help' for built-in tools)"
    }

    private suspend fun runProcess(
        cmd: List<String>,
        workDir: File,
        env: Map<String, String>,
        onOutputChunk: ((String) -> Unit)? = null
    ): String = withContext(Dispatchers.IO) {
        try {
            val pb = ProcessBuilder(cmd)
            pb.directory(workDir)
            val pbEnv = pb.environment()
            pbEnv.putAll(env)
            pb.redirectErrorStream(true)
            val proc = pb.start()
            activeSubProcess = proc

            val reader = InputStreamReader(proc.inputStream)
            val buffer = CharArray(1024)
            val output = StringBuilder()
            var read: Int
            while (reader.read(buffer).also { read = it } != -1) {
                val chunk = String(buffer, 0, read)
                output.append(chunk)
                if (onOutputChunk != null) {
                    withContext(Dispatchers.Main) {
                        onOutputChunk(chunk)
                    }
                }
            }
            proc.waitFor()
            activeSubProcess = null
            output.toString().trimEnd()
        } catch (e: Exception) {
            val err = "Process error: ${e.message}"
            if (onOutputChunk != null) {
                withContext(Dispatchers.Main) {
                    onOutputChunk(err)
                }
            }
            err
        }
    }

    private fun resolveFile(path: String): File {
        return if (path.startsWith("/")) {
            File(rootDirectory, path.removePrefix("/"))
        } else {
            File(currentDirectory, path)
        }
    }

    private fun parseArgs(commandLine: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var quoteChar = ' '

        for (ch in commandLine) {
            when {
                (ch == '"' || ch == '\'') && !inQuotes -> {
                    inQuotes = true
                    quoteChar = ch
                }
                ch == quoteChar && inQuotes -> {
                    inQuotes = false
                }
                ch.isWhitespace() && !inQuotes -> {
                    if (sb.isNotEmpty()) {
                        tokens.add(sb.toString())
                        sb.clear()
                    }
                }
                else -> sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) tokens.add(sb.toString())
        return tokens
    }

    private suspend fun handleList(isJson: Boolean): String {
        val projects = projectRepository.getAll()
        if (isJson) return json.encodeToString(projects)
        if (projects.isEmpty()) return "No projects created yet. Use 'create <name> <runtime> <port>'."
        val header = String.format(Locale.US, "%-16s %-10s %-6s %-10s %s", "NAME", "RUNTIME", "PORT", "STATUS", "PID")
        val rows = projects.map { p ->
            val pid = if (p.status == ProjectStatus.RUNNING) "${p.pid ?: "-"}" else "-"
            String.format(Locale.US, "%-16s %-10s %-6d %-10s %s", p.name.take(16), p.runtime.name, p.port, p.status.name, pid)
        }
        return (listOf(header) + rows).joinToString("\n")
    }

    private suspend fun handleStatus(isJson: Boolean): String {
        val projects = projectRepository.getAll()
        val running = projects.count { it.status == ProjectStatus.RUNNING }
        val tunnelStatus = tunnelManager.tunnelStatus.value.name
        val tunnelUrl = tunnelManager.activeUrl.value

        if (isJson) {
            return json.encodeToString(mapOf(
                "totalProjects" to projects.size.toString(),
                "runningProjects" to running.toString(),
                "tunnelStatus" to tunnelStatus,
                "tunnelUrl" to tunnelUrl,
                "workingDir" to getCurrentPath()
            ))
        }

        return """
Localhost Platform Status:
--------------------------
Running Projects : $running / ${projects.size}
Tunnel Status    : $tunnelStatus
Tunnel URL       : ${if (tunnelUrl.isNotBlank()) tunnelUrl else "None"}
Working Dir      : ${getCurrentPath()}
""".trimIndent()
    }

    private suspend fun handleStart(nameOrId: String?, isJson: Boolean): String {
        if (nameOrId == null) return "Usage: start <project-name-or-id>"
        val project = findProject(nameOrId) ?: return "Project '$nameOrId' not found."
        val res = processSupervisor.startProject(project)
        return if (res.isSuccess) {
            if (isJson) json.encodeToString(mapOf("success" to true, "message" to "Started ${project.name}"))
            else "Started ${project.name} on port ${project.port}."
        } else {
            "Failed to start ${project.name}: ${res.exceptionOrNull()?.message}"
        }
    }

    private suspend fun handleStop(nameOrId: String?, isJson: Boolean): String {
        if (nameOrId == null) return "Usage: stop <project-name-or-id>"
        val project = findProject(nameOrId) ?: return "Project '$nameOrId' not found."
        processSupervisor.stopProject(project.id)
        return if (isJson) json.encodeToString(mapOf("success" to true, "message" to "Stopped ${project.name}"))
        else "Stopped ${project.name}."
    }

    private suspend fun handleRestart(nameOrId: String?, isJson: Boolean): String {
        if (nameOrId == null) return "Usage: restart <project-name-or-id>"
        val project = findProject(nameOrId) ?: return "Project '$nameOrId' not found."
        val res = processSupervisor.restartProject(project.id)
        return if (res.isSuccess) "Restarted ${project.name}." else "Failed: ${res.exceptionOrNull()?.message}"
    }

    private suspend fun handleLogs(nameOrId: String?, isJson: Boolean): String {
        if (nameOrId == null) return "Usage: logs <project-name-or-id>"
        val project = findProject(nameOrId) ?: return "Project '$nameOrId' not found."
        val logs = logRepository.getLogs(project.id)
        if (isJson) return json.encodeToString(logs)
        if (logs.isEmpty()) return "No logs recorded for ${project.name}."
        return logs.joinToString("\n") { "[${it.stream}] ${it.message}" }
    }

    private suspend fun handleCreate(args: List<String>, isJson: Boolean): String {
        if (args.size < 4) {
            return "Usage: create <name> <PYTHON|NODEJS|PHP|STATIC|JAVA> <port>"
        }
        val name = args[1]
        val runtime = try {
            RuntimeType.valueOf(args[2].uppercase())
        } catch (_: Exception) {
            return "Invalid runtime. Supported: PYTHON, NODEJS, PHP, STATIC, JAVA"
        }
        val inputPort = args[3].toIntOrNull() ?: return "Invalid port number."
        val port = if (inputPort == 8080 || inputPort <= 0) runtime.defaultPort else inputPort

        val projectDir = File(rootDirectory, "projects/$name").apply { mkdirs() }
        val id = UUID.randomUUID().toString()
        val project = Project(
            id = id,
            name = name,
            runtime = runtime,
            port = port,
            workingDir = projectDir.absolutePath,
            startupCommand = when (runtime) {
                RuntimeType.PYTHON -> "python3 app.py"
                RuntimeType.NODEJS -> "node server.js"
                RuntimeType.PHP -> "builtin"
                RuntimeType.STATIC -> "builtin"
                RuntimeType.JAVA -> "java -jar app.jar"
            }
        )
        if (runtime == RuntimeType.PYTHON) {
            val pyExec = runtimeManager.getExecutablePath(RuntimeType.PYTHON)
            if (pyExec != null) {
                val pyEnv = runtimeManager.getEnvironmentForRuntime(RuntimeType.PYTHON)
                val venvDir = File(projectDir, ".venv")
                try {
                    val pb = ProcessBuilder(pyExec, "-m", "venv", "--system-site-packages", venvDir.absolutePath)
                    pb.directory(projectDir)
                    pb.environment().putAll(pyEnv)
                    val proc = pb.start()
                    proc.waitFor()
                } catch (_: Exception) {}
            }
        }
        projectRepository.save(project)
        return "Created project '$name' ($runtime) on port $port at /projects/$name."
    }

    private suspend fun handleDeleteProject(nameOrId: String?): String {
        if (nameOrId == null) return "Usage: delete <project-name-or-id>"
        val p = findProject(nameOrId) ?: return "Project '$nameOrId' not found"
        processSupervisor.stopProject(p.id)
        val dir = File(p.workingDir)
        if (dir.exists()) FileUtils.deleteRecursively(dir)
        projectRepository.delete(p.id)
        return "Deleted project '${p.name}'"
    }

    private fun handleTunnel(subCommand: String?, isJson: Boolean): String {
        return when (subCommand) {
            "start", "quick" -> {
                tunnelManager.startQuickTunnel(8080)
                "Starting Cloudflare Quick Tunnel..."
            }
            "stop" -> {
                tunnelManager.stopTunnel()
                "Tunnel stopped."
            }
            "status" -> {
                "Status: ${tunnelManager.tunnelStatus.value.name}\nURL: ${tunnelManager.activeUrl.value}"
            }
            else -> "Usage: tunnel <start|stop|status>"
        }
    }

    private suspend fun findProject(nameOrId: String): Project? {
        val byId = projectRepository.getById(nameOrId)
        if (byId != null) return byId
        return projectRepository.getAll().find { it.name.equals(nameOrId, ignoreCase = true) }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "${bytes}B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format(Locale.US, "%.1f%s", bytes / Math.pow(1024.0, exp.toDouble()), pre)
    }

    private fun getHelpText(): String = """
Localhost CLI Commands:
  Navigation & Inspection:
    cd [path|-]                Change directory (- for previous directory)
    pwd                        Print current directory
    ls [-la|-lh|-t|-r|-d]      List directory contents with options
    tree [-d] [-L <N>] [-a]    Display directory hierarchy
    cat [-n] <file...>         Display file contents with line numbering
    head [-n <N>] <file>       Display first N lines
    tail [-n <N>] <file>       Display last N lines
    stat <file>                Display detailed file metadata
    wc [-l|-w|-c] <file>       Count lines, words, bytes

  Search & Modification:
    grep [-i|-n|-v|-r] <pat>   Search file contents
    find [path] -name <glob>   Find files and folders
    mkdir [-p] <dir...>        Create directory
    touch <file...>            Create new empty file
    cp [-r] <src> <dst>        Copy file or folder
    mv <src> <dst>             Move or rename file
    rm [-r] <path...>          Remove file or directory
    echo <text> [>|>> file]    Print text or redirect to file

  System & Utilities:
    df [-h]                    Show storage filesystem disk space
    du [-h] [dir]              Estimate folder space usage
    uname [-a]                 Print operating system architecture
    whoami                     Print current user identity
    date                       Print current system timestamp
    which <binary>             Locate program binary
    sleep <seconds>            Wait specified number of seconds
    clear                      Clear terminal buffer
    history                    Show recent command history

  Runtime Environments:
    python / python3 <args>    Run Python 3 scripts or commands
    pip <args>                 Run pip package manager
    node / npm <args>          Run Node.js / npm
    php <args>                 Run PHP scripts
    exec <binary>              Run custom ELF executable

  Project VPS Management:
    list [--json] / ps         List all hosted projects
    status [--json]            Show system & tunnel status
    create <name> <rt> <port>  Scaffold project (PYTHON|NODEJS|PHP|STATIC|JAVA)
    delete <name|id>           Delete project and remove files
    start <name|id>            Start project server
    stop <name|id>             Stop project server
    restart <name|id>          Restart project server
    logs <name|id>             View recent server logs
    tunnel <start|stop|status> Manage Cloudflare tunnel
""".trimIndent()

    private fun handleCurl(args: List<String>): String {
        if (args.isEmpty()) return "curl: try 'curl --help' or specify a URL"
        var outputFile: String? = null
        var url: String? = null
        var idx = 0
        while (idx < args.size) {
            when (args[idx]) {
                "-o", "--output" -> {
                    if (idx + 1 < args.size) {
                        outputFile = args[idx + 1]
                        idx += 2
                        continue
                    }
                }
                "-s", "--silent", "-L", "-i" -> {}
                else -> {
                    if (!args[idx].startsWith("-") && url == null) {
                        url = args[idx]
                    }
                }
            }
            idx++
        }

        if (url == null) return "curl: no URL specified"
        val targetUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "http://$url" else url

        return try {
            val req = Request.Builder().url(targetUrl).header("User-Agent", "curl/8.4.0").build()
            val resp = okHttpClient.newCall(req).execute()
            val bodyBytes = resp.body?.bytes() ?: ByteArray(0)
            if (outputFile != null) {
                val f = resolveFile(outputFile)
                f.writeBytes(bodyBytes)
                "Downloaded ${bodyBytes.size} bytes to ${f.name}"
            } else {
                val text = String(bodyBytes)
                if (text.length > 8192) text.take(8192) + "\n... [truncated]" else text
            }
        } catch (e: Exception) {
            "curl: Failed: ${e.message}"
        }
    }

    private fun handleChmod(args: List<String>): String {
        if (args.size < 2) return "chmod: missing operand"
        val mode = args[0]
        val fileArg = args[1]
        val target = resolveFile(fileArg)
        if (!target.exists()) return "chmod: cannot access '$fileArg': No such file or directory"
        if (mode.contains("+x") || mode == "755" || mode == "777") {
            target.setExecutable(true, false)
        }
        if (mode.contains("+r") || mode == "644" || mode == "755") {
            target.setReadable(true, false)
        }
        if (mode.contains("+w")) {
            target.setWritable(true, false)
        }
        return ""
    }

    private suspend fun handleComposer(
        args: List<String>,
        onOutputChunk: ((String) -> Unit)? = null
    ): String {
        val phpPath = runtimeManager.getExecutablePath(RuntimeType.PHP)
            ?: return "composer: PHP runtime is not installed. Install PHP from Settings -> Runtimes."
        val composerPhar = File(context.filesDir, "bin/composer.phar")
        if (!composerPhar.exists()) {
            return "composer: composer.phar not found. Download Composer from Settings or use composer download."
        }
        val phpEnv = getDefaultTerminalEnvironment(RuntimeType.PHP)
        return runProcess(listOf(phpPath, composerPhar.absolutePath) + args, currentDirectory, phpEnv, onOutputChunk)
    }
}
