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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalhostCli @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository,
    private val logRepository: LogRepository,
    private val processSupervisor: ProcessSupervisor,
    private val tunnelManager: CloudflareTunnelManager,
    private val runtimeManager: RuntimeManager
) {
    private val json = Json { prettyPrint = true }
    private val rootDirectory: File = context.filesDir.apply { mkdirs() }
    private var currentDirectory: File = rootDirectory
    private var previousDirectory: File = rootDirectory
    private val commandHistory = mutableListOf<String>()
    private var activeSubProcess: Process? = null

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

    suspend fun execute(commandLine: String): String = withContext(Dispatchers.IO) {
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

        val root = tokens[0].lowercase()
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
                "python", "python3" -> handleRuntimeExec(RuntimeType.PYTHON, args)
                "pip", "pip3" -> handleRuntimeExec(RuntimeType.PYTHON, listOf("-m", "pip") + args)
                "node" -> handleRuntimeExec(RuntimeType.NODEJS, args)
                "npm" -> handleRuntimeExec(RuntimeType.NODEJS, listOf("npm") + args)
                "php" -> handleRuntimeExec(RuntimeType.PHP, args)
                "exec" -> handleCustomExec(args)
                else -> handleFallbackExec(root, args)
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
        val showAll = args.any { it.contains("a") }
        val longFormat = args.any { it.contains("l") }
        val targetPath = args.lastOrNull { !it.startsWith("-") }

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

    private fun handleTouch(args: List<String>): String {
        if (args.isEmpty()) return "touch: missing file operand"
        args.forEach { filePath ->
            val f = resolveFile(filePath)
            if (f.exists()) {
                f.setLastModified(System.currentTimeMillis())
            } else {
                f.parentFile?.mkdirs()
                f.createNewFile()
            }
        }
        return ""
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

    private fun handleRm(args: List<String>): String {
        val files = args.filter { !it.startsWith("-") }
        if (files.isEmpty()) return "rm: missing operand"
        files.forEach { filePath ->
            val f = resolveFile(filePath)
            if (f.isDirectory) FileUtils.deleteRecursively(f) else f.delete()
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

    private suspend fun handleRuntimeExec(runtime: RuntimeType, args: List<String>): String {
        val execPath = runtimeManager.getExecutablePath(runtime)
            ?: return "${runtime.displayName} runtime is not installed. Download it from Settings."
        val env = runtimeManager.getEnvironmentForRuntime(runtime)
        val fullCmd = if (args.firstOrNull() == "npm") {
            val npmFile = File(File(execPath).parentFile, "npm")
            listOf(npmFile.absolutePath) + args.drop(1)
        } else {
            listOf(execPath) + args
        }
        return runProcess(fullCmd, currentDirectory, env)
    }

    private suspend fun handleCustomExec(args: List<String>): String {
        if (args.isEmpty()) return "exec: missing executable path"
        val bin = resolveFile(args[0])
        if (!bin.exists()) return "exec: ${args[0]}: No such file"
        bin.setExecutable(true, false)
        return runProcess(listOf(bin.absolutePath) + args.drop(1), currentDirectory, emptyMap())
    }

    private suspend fun handleFallbackExec(root: String, args: List<String>): String {
        val binInCur = File(currentDirectory, root)
        if (binInCur.exists() && binInCur.canExecute()) {
            return runProcess(listOf(binInCur.absolutePath) + args, currentDirectory, emptyMap())
        }
        return "localhost: command not found: $root (type 'help' for built-in tools)"
    }

    private suspend fun runProcess(cmd: List<String>, workDir: File, env: Map<String, String>): String = withContext(Dispatchers.IO) {
        try {
            val pb = ProcessBuilder(cmd)
            pb.directory(workDir)
            val pbEnv = pb.environment()
            pbEnv.putAll(env)
            pb.redirectErrorStream(true)
            val proc = pb.start()
            activeSubProcess = proc

            val reader = BufferedReader(InputStreamReader(proc.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            proc.waitFor()
            activeSubProcess = null
            output.toString().trimEnd()
        } catch (e: Exception) {
            "Process error: ${e.message}"
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
}
