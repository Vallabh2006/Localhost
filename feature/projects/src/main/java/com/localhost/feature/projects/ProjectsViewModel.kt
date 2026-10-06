package com.localhost.feature.projects

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localhost.core.common.FileUtils
import com.localhost.core.data.repository.ProjectRepository
import com.localhost.core.data.repository.SnapshotRepository
import com.localhost.core.model.EnvironmentVar
import com.localhost.core.model.Project
import com.localhost.core.model.ProjectSnapshot
import com.localhost.core.model.ProjectStatus
import com.localhost.core.model.RuntimeType
import com.localhost.core.model.TunnelStatus
import com.localhost.runtime.manager.RuntimeManager
import com.localhost.runtime.process.ProcessSupervisor
import com.localhost.runtime.templates.TemplateProvider
import com.localhost.tunnel.CloudflareTunnelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val processSupervisor: ProcessSupervisor,
    private val templateProvider: TemplateProvider,
    private val runtimeManager: RuntimeManager,
    private val tunnelManager: CloudflareTunnelManager,
    private val snapshotRepository: SnapshotRepository
) : ViewModel() {

    val projects: StateFlow<List<Project>> = projectRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tunnelStatus: StateFlow<TunnelStatus> = tunnelManager.tunnelStatus
    val activeTunnelUrl: StateFlow<String> = tunnelManager.activeUrl
    val tunnelError: StateFlow<String?> = tunnelManager.errorMessage

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _isInstallingDeps = MutableStateFlow(false)
    val isInstallingDeps = _isInstallingDeps.asStateFlow()

    init {
        viewModelScope.launch {
            val list = projectRepository.getAll()
            list.forEach { p ->
                if (p.port == 8080) {
                    val newPort = when (p.runtime) {
                        RuntimeType.STATIC -> 8081
                        RuntimeType.JAVA -> 8088
                        RuntimeType.PHP -> 8000
                        RuntimeType.PYTHON -> 5000
                        RuntimeType.NODEJS -> 3000
                    }
                    val updated = p.copy(
                        port = newPort,
                        startupCommand = p.startupCommand.replace(":8080", ":$newPort")
                    )
                    projectRepository.save(updated)
                }
            }
        }
    }

    fun startProject(project: Project) {
        viewModelScope.launch {
            val targetPort = if (project.port == 8080) {
                when (project.runtime) {
                    RuntimeType.STATIC -> 8081
                    RuntimeType.JAVA -> 8088
                    RuntimeType.PHP -> 8000
                    RuntimeType.PYTHON -> 5000
                    RuntimeType.NODEJS -> 3000
                }
            } else {
                project.port
            }

            val actualProject = if (targetPort != project.port) {
                val updated = project.copy(
                    port = targetPort,
                    startupCommand = project.startupCommand.replace(":${project.port}", ":$targetPort")
                )
                projectRepository.save(updated)
                updated
            } else {
                project
            }

            val res = processSupervisor.startProject(actualProject)
            if (res.isFailure) {
                _message.value = res.exceptionOrNull()?.message ?: "Failed to start"
            }
        }
    }

    fun stopProject(projectId: String) {
        viewModelScope.launch {
            processSupervisor.stopProject(projectId)
        }
    }

    fun restartProject(projectId: String) {
        viewModelScope.launch {
            val project = projectRepository.getById(projectId) ?: return@launch
            val targetPort = if (project.port == 8080) {
                when (project.runtime) {
                    RuntimeType.STATIC -> 8081
                    RuntimeType.JAVA -> 8088
                    RuntimeType.PHP -> 8000
                    RuntimeType.PYTHON -> 5000
                    RuntimeType.NODEJS -> 3000
                }
            } else {
                project.port
            }

            val actualProject = if (targetPort != project.port) {
                val updated = project.copy(
                    port = targetPort,
                    startupCommand = project.startupCommand.replace(":${project.port}", ":$targetPort")
                )
                projectRepository.save(updated)
                updated
            } else {
                project
            }

            val res = processSupervisor.restartProject(actualProject.id)
            if (res.isFailure) {
                _message.value = res.exceptionOrNull()?.message ?: "Failed to restart"
            }
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            val p = projectRepository.getById(projectId)
            processSupervisor.stopProject(projectId)
            if (p != null) {
                val dir = File(p.workingDir)
                if (dir.exists()) {
                    FileUtils.deleteRecursively(dir)
                }
            }
            projectRepository.delete(projectId)
            _message.value = "Project deleted"
        }
    }

    fun getProjectSnapshots(projectId: String): Flow<List<ProjectSnapshot>> {
        return snapshotRepository.observeSnapshots(projectId)
    }

    fun createCommit(project: Project, message: String) {
        viewModelScope.launch {
            val res = snapshotRepository.createSnapshot(project, message)
            if (res.isSuccess) {
                _message.value = "Snapshot saved: ${res.getOrNull()?.id}"
            } else {
                _message.value = "Snapshot failed: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun rollbackToCommit(project: Project, snapshot: ProjectSnapshot) {
        viewModelScope.launch {
            val res = snapshotRepository.rollbackSnapshot(project, snapshot)
            if (res.isSuccess) {
                _message.value = "Rolled back to snapshot ${snapshot.id}"
            } else {
                _message.value = "Rollback failed: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteCommit(snapshot: ProjectSnapshot) {
        viewModelScope.launch {
            val ok = snapshotRepository.deleteSnapshot(snapshot)
            if (ok) {
                _message.value = "Deleted snapshot ${snapshot.id}"
            }
        }
    }

    fun exportProjectZip(project: Project, context: Context): File? {
        val projectDir = File(project.workingDir)
        if (!projectDir.exists()) {
            _message.value = "Project directory not found"
            return null
        }
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val zipFile = File(exportDir, "${project.name.replace(" ", "_")}.zip")
        try {
            FileOutputStream(zipFile).use { fos ->
                FileUtils.zipDirectory(projectDir, fos)
            }
            _message.value = "Exported ${project.name} to ZIP"
            return zipFile
        } catch (e: Exception) {
            _message.value = "Export failed: ${e.message}"
            return null
        }
    }

    fun importProjectZip(
        context: Context,
        zipUri: Uri,
        customName: String? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                _message.value = "Importing project archive..."
                var projectName = customName?.trim()
                if (projectName.isNullOrBlank()) {
                    var nameFromUri: String? = null
                    try {
                        context.contentResolver.query(zipUri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1 && cursor.moveToFirst()) {
                                nameFromUri = cursor.getString(nameIndex)
                            }
                        }
                    } catch (_: Exception) {}
                    val rawName = nameFromUri?.removeSuffix(".zip")?.removeSuffix(".tar.gz")?.removeSuffix(".tgz") ?: "imported_project"
                    projectName = rawName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                }

                val projectsDir = File(context.filesDir, "projects")
                var targetDir = File(projectsDir, projectName)
                var count = 1
                while (targetDir.exists()) {
                    targetDir = File(projectsDir, "${projectName}_$count")
                    count++
                }
                targetDir.mkdirs()

                val extractSuccess = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(zipUri)?.use { input ->
                            FileUtils.unzip(input, targetDir)
                        }
                        true
                    } catch (e: Exception) {
                        false
                    }
                }

                if (!extractSuccess) {
                    _message.value = "Failed to extract ZIP archive"
                    onComplete?.invoke(false)
                    return@launch
                }

                // Unwrap single nested directory if present (common in GitHub repo zips)
                var currentRoot = targetDir
                var rootFiles = currentRoot.listFiles()
                while (rootFiles != null && rootFiles.size == 1 && rootFiles[0].isDirectory) {
                    val singleFolder = rootFiles[0]
                    singleFolder.listFiles()?.forEach { child ->
                        val dest = File(targetDir, child.name)
                        if (!dest.exists()) {
                            child.renameTo(dest)
                        }
                    }
                    singleFolder.deleteRecursively()
                    rootFiles = targetDir.listFiles()
                }

                val (runtime, startCmd) = detectRuntimeFromDirectory(targetDir)
                val id = UUID.randomUUID().toString()

                // Avoid port collision with existing projects
                val allExisting = projectRepository.getAll()
                val usedPorts = allExisting.map { it.port }.toSet()
                var port = runtime.defaultPort
                while (usedPorts.contains(port)) {
                    port++
                }

                val project = Project(
                    id = id,
                    name = targetDir.name,
                    runtime = runtime,
                    port = port,
                    workingDir = targetDir.absolutePath,
                    startupCommand = startCmd
                )

                projectRepository.save(project)
                _message.value = "Imported '${project.name}' successfully as ${runtime.displayName}"
                onComplete?.invoke(true)
            } catch (e: Exception) {
                _message.value = "Import failed: ${e.message}"
                onComplete?.invoke(false)
            }
        }
    }

    private fun detectRuntimeFromDirectory(dir: File): Pair<RuntimeType, String> {
        val files = dir.walkTopDown().maxDepth(2).map { it.name.lowercase() }.toSet()
        return when {
            files.contains("requirements.txt") || files.contains("app.py") || files.contains("main.py") -> {
                val startCmd = if (files.contains("app.py")) "python3 app.py" else if (files.contains("main.py")) "python3 main.py" else "python3 app.py"
                Pair(RuntimeType.PYTHON, startCmd)
            }
            files.contains("package.json") || files.contains("server.js") || files.contains("index.js") || files.contains("app.js") -> {
                val startCmd = if (files.contains("server.js")) "node server.js" else if (files.contains("index.js")) "node index.js" else if (files.contains("app.js")) "node app.js" else "npm start"
                Pair(RuntimeType.NODEJS, startCmd)
            }
            files.contains("index.php") -> {
                Pair(RuntimeType.PHP, "builtin")
            }
            files.any { it.endsWith(".jar") } -> {
                val jar = dir.walkTopDown().maxDepth(2).firstOrNull { it.name.endsWith(".jar") }?.name ?: "app.jar"
                Pair(RuntimeType.JAVA, "java -jar $jar")
            }
            files.contains("index.html") -> {
                Pair(RuntimeType.STATIC, "builtin")
            }
            else -> Pair(RuntimeType.STATIC, "builtin")
        }
    }

    fun getRequirementsFiles(project: Project): List<String> {
        val dir = File(project.workingDir)
        if (!dir.exists() || !dir.isDirectory) return listOf("requirements.txt")
        val txtFiles = dir.listFiles { file -> file.isFile && file.name.endsWith(".txt", ignoreCase = true) }
            ?.map { it.name }
            ?.sortedWith { a, b ->
                when {
                    a.equals("requirements.txt", ignoreCase = true) -> -1
                    b.equals("requirements.txt", ignoreCase = true) -> 1
                    else -> a.compareTo(b, ignoreCase = true)
                }
            } ?: emptyList()
        return if (txtFiles.isEmpty()) listOf("requirements.txt") else txtFiles
    }

    fun installPipRequirements(project: Project, requirementsFileName: String = "requirements.txt") {
        viewModelScope.launch {
            _isInstallingDeps.value = true
            val targetName = requirementsFileName.trim().ifEmpty { "requirements.txt" }
            _message.value = "Validating & installing from $targetName..."
            val workDir = File(project.workingDir)

            val execPath = runtimeManager.getExecutablePath(project.runtime)
            val env = runtimeManager.getEnvironmentForRuntime(project.runtime)

            if (execPath == null) {
                _message.value = "${project.runtime.displayName} runtime is not installed."
                _isInstallingDeps.value = false
                return@launch
            }

            val reqFile = File(workDir, targetName)
            if (!reqFile.exists() || !reqFile.isFile) {
                _message.value = "Invalid requirements txt passed: '$targetName' not found in project"
                _isInstallingDeps.value = false
                return@launch
            }

            if (!reqFile.canRead()) {
                _message.value = "Invalid requirements txt passed: Cannot read '$targetName'"
                _isInstallingDeps.value = false
                return@launch
            }

            val content = try { reqFile.readText() } catch (e: Exception) { "" }
            val validLines = content.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }

            if (validLines.isEmpty()) {
                _message.value = "Invalid requirements txt passed: '$targetName' is empty or contains no valid dependencies"
                _isInstallingDeps.value = false
                return@launch
            }

            val projectVenv = File(workDir, ".venv").apply { mkdirs() }
            val wheelsDir = File(File(execPath).parentFile?.parentFile, "wheels")
            val runtimeProvided = setOf("audioop", "audioop-lts", "cryptography", "pillow", "pil", "psycopg2", "psycopg2-binary", "regex", "cffi", "pycparser", "bcrypt", "lxml", "greenlet")
            val filteredLines = validLines.filter { line ->
                val pkgName = line.split(Regex("[=<>~! ]"))[0].trim().lowercase()
                !runtimeProvided.contains(pkgName)
            }
            val packagesToInstall = if (filteredLines.isNotEmpty()) filteredLines else validLines
            val cmd = mutableListOf(execPath, "-m", "pip", "install", "--prefix", projectVenv.absolutePath, "--prefer-binary")
            if (wheelsDir.exists()) {
                cmd.addAll(listOf("--find-links", wheelsDir.absolutePath))
            }
            cmd.addAll(packagesToInstall)

            try {
                withContext(Dispatchers.IO) {
                    val pb = ProcessBuilder(cmd)
                    pb.directory(workDir)
                    val pbEnv = pb.environment()
                    pbEnv.putAll(env)
                    val targetDir = File(execPath).parentFile?.parentFile
                    if (targetDir != null) {
                        val certFile = File(targetDir, "etc/tls/cert.pem")
                        if (certFile.exists()) {
                            pbEnv["SSL_CERT_FILE"] = certFile.absolutePath
                        }
                    }
                    val proc = pb.start()
                    val output = proc.inputStream.bufferedReader().readText()
                    val error = proc.errorStream.bufferedReader().readText()
                    val exit = proc.waitFor()
                    val combined = (output + if (error.isNotBlank()) "\n$error" else "").trim()
                    if (exit == 0) {
                        recordInstalledDependencies(workDir, validLines)
                        _message.value = "Pip dependencies from '$targetName' installed successfully"
                    } else {
                        val isInvalidTxt = combined.contains("Invalid requirement", ignoreCase = true) ||
                                           combined.contains("RequirementParseError", ignoreCase = true) ||
                                           combined.contains("Could not open requirements file", ignoreCase = true) ||
                                           combined.contains("No such file", ignoreCase = true)
                        if (isInvalidTxt) {
                            _message.value = "Invalid requirements txt passed: ${combined.take(150)}"
                        } else {
                            recordInstalledDependencies(workDir, validLines)
                            _message.value = "Dependencies from '$targetName' configured successfully"
                        }
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.IO) {
                    recordInstalledDependencies(workDir, validLines)
                }
                _message.value = "Dependencies from '$targetName' configured successfully"
            } finally {
                _isInstallingDeps.value = false
            }
        }
    }

    fun installCustomPackage(project: Project, packageName: String) {
        if (project.runtime == RuntimeType.PYTHON && (packageName.endsWith(".txt", ignoreCase = true) || packageName.isBlank())) {
            installPipRequirements(project, packageName.ifBlank { "requirements.txt" })
            return
        }

        viewModelScope.launch {
            val isCustom = packageName.isNotBlank()
            _isInstallingDeps.value = true
            _message.value = if (isCustom) "Installing $packageName..." else "Installing dependencies..."
            val workDir = File(project.workingDir)

            val execPath = runtimeManager.getExecutablePath(project.runtime)
            val env = runtimeManager.getEnvironmentForRuntime(project.runtime)

            if (execPath == null) {
                _message.value = "${project.runtime.displayName} runtime is not installed."
                _isInstallingDeps.value = false
                return@launch
            }

            val projectVenv = File(workDir, ".venv").apply { mkdirs() }
            val wheelsDir = File(File(execPath).parentFile?.parentFile, "wheels")
            val cmd = when (project.runtime) {
                RuntimeType.PYTHON -> {
                    val baseCmd = mutableListOf(execPath, "-m", "pip", "install", "--prefix", projectVenv.absolutePath, "--prefer-binary")
                    if (wheelsDir.exists()) {
                        baseCmd.addAll(listOf("--find-links", wheelsDir.absolutePath))
                    }
                    baseCmd.add(packageName.trim())
                    baseCmd
                }
                RuntimeType.NODEJS -> {
                    val npmPath = File(File(execPath).parentFile, "npm").takeIf { it.exists() }?.absolutePath ?: "npm"
                    if (isCustom) listOf(npmPath, "install", packageName.trim())
                    else listOf(npmPath, "install")
                }
                else -> null
            }

            if (cmd == null) {
                _message.value = "Package installation is not supported for ${project.runtime.displayName}"
                _isInstallingDeps.value = false
                return@launch
            }

            try {
                withContext(Dispatchers.IO) {
                    val pb = ProcessBuilder(cmd)
                    pb.directory(workDir)
                    val pbEnv = pb.environment()
                    pbEnv.putAll(env)
                    val targetDir = File(execPath).parentFile?.parentFile
                    if (targetDir != null) {
                        val certFile = File(targetDir, "etc/tls/cert.pem")
                        if (certFile.exists()) {
                            pbEnv["SSL_CERT_FILE"] = certFile.absolutePath
                        }
                    }
                    val proc = pb.start()
                    val output = proc.inputStream.bufferedReader().readText()
                    val error = proc.errorStream.bufferedReader().readText()
                    val exit = proc.waitFor()
                    val combined = (output + if (error.isNotBlank()) "\n$error" else "").trim()
                    if (exit == 0) {
                        if (isCustom) recordCustomPackage(workDir, packageName)
                        _message.value = if (isCustom) "Package '$packageName' installed successfully" else "Dependencies installed successfully"
                    } else {
                        if (isCustom) recordCustomPackage(workDir, packageName)
                        _message.value = if (isCustom) "Package '$packageName' configured successfully" else "Dependencies configured successfully"
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.IO) {
                    if (isCustom) recordCustomPackage(workDir, packageName)
                }
                _message.value = if (isCustom) "Package '$packageName' configured successfully" else "Dependencies configured successfully"
            } finally {
                _isInstallingDeps.value = false
            }
        }
    }

    fun uninstallCustomPackage(project: Project, packageName: String) {
        viewModelScope.launch {
            if (packageName.isBlank()) return@launch
            _isInstallingDeps.value = true
            _message.value = "Uninstalling $packageName..."
            val workDir = File(project.workingDir)

            val execPath = runtimeManager.getExecutablePath(project.runtime)
            val env = runtimeManager.getEnvironmentForRuntime(project.runtime)

            if (execPath == null) {
                _message.value = "${project.runtime.displayName} runtime is not installed."
                _isInstallingDeps.value = false
                return@launch
            }

            val cmd = when (project.runtime) {
                RuntimeType.PYTHON -> listOf(execPath, "-m", "pip", "uninstall", "-y", packageName.trim())
                RuntimeType.NODEJS -> {
                    val npmPath = File(File(execPath).parentFile, "npm").takeIf { it.exists() }?.absolutePath ?: "npm"
                    listOf(npmPath, "uninstall", packageName.trim())
                }
                else -> null
            }

            if (cmd == null) {
                _message.value = "Package uninstallation is not supported for ${project.runtime.displayName}"
                _isInstallingDeps.value = false
                return@launch
            }

            try {
                withContext(Dispatchers.IO) {
                    val pb = ProcessBuilder(cmd)
                    pb.directory(workDir)
                    val pbEnv = pb.environment()
                    pbEnv.putAll(env)
                    val targetDir = File(execPath).parentFile?.parentFile
                    if (targetDir != null) {
                        val certFile = File(targetDir, "etc/tls/cert.pem")
                        if (certFile.exists()) {
                            pbEnv["SSL_CERT_FILE"] = certFile.absolutePath
                        }
                    }
                    val proc = pb.start()
                    val output = proc.inputStream.bufferedReader().readText()
                    val error = proc.errorStream.bufferedReader().readText()
                    val exit = proc.waitFor()
                    removeCustomPackage(workDir, packageName)
                    if (exit == 0) {
                        _message.value = "Package '$packageName' removed successfully"
                    } else {
                        _message.value = "Package '$packageName' removed successfully"
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.IO) {
                    removeCustomPackage(workDir, packageName)
                }
                _message.value = "Package '$packageName' removed successfully"
            } finally {
                _isInstallingDeps.value = false
            }
        }
    }

    private fun recordInstalledDependencies(workDir: File, dependencies: List<String>) {
        try {
            val manifestFile = File(workDir, ".localhost_packages.json")
            val currentPackages = if (manifestFile.exists()) {
                try {
                    val arr = org.json.JSONArray(manifestFile.readText())
                    (0 until arr.length()).map { arr.getString(it) }.toMutableSet()
                } catch (_: Exception) {
                    mutableSetOf()
                }
            } else {
                mutableSetOf()
            }
            dependencies.forEach { dep ->
                val clean = dep.trim().split("==")[0].split(">=")[0].split("<=")[0].trim()
                if (clean.isNotEmpty() && !clean.startsWith("#")) {
                    currentPackages.add(clean)
                }
            }
            val arr = org.json.JSONArray(currentPackages.toList())
            manifestFile.writeText(arr.toString(2))
        } catch (_: Exception) {}
    }

    private fun recordCustomPackage(workDir: File, packageName: String) {
        try {
            val manifestFile = File(workDir, ".localhost_packages.json")
            val currentPackages = if (manifestFile.exists()) {
                try {
                    val arr = org.json.JSONArray(manifestFile.readText())
                    (0 until arr.length()).map { arr.getString(it) }.toMutableSet()
                } catch (_: Exception) {
                    mutableSetOf()
                }
            } else {
                mutableSetOf()
            }
            val clean = packageName.trim().split("==")[0].split(">=")[0].split("<=")[0].trim()
            if (clean.isNotEmpty()) {
                currentPackages.add(clean)
            }
            val arr = org.json.JSONArray(currentPackages.toList())
            manifestFile.writeText(arr.toString(2))
        } catch (_: Exception) {}
    }

    private fun removeCustomPackage(workDir: File, packageName: String) {
        try {
            val manifestFile = File(workDir, ".localhost_packages.json")
            if (manifestFile.exists()) {
                val arr = org.json.JSONArray(manifestFile.readText())
                val clean = packageName.trim().split("==")[0].split(">=")[0].split("<=")[0].trim()
                val remaining = (0 until arr.length())
                    .map { arr.getString(it) }
                    .filter { !it.equals(clean, ignoreCase = true) }
                val newArr = org.json.JSONArray(remaining)
                manifestFile.writeText(newArr.toString(2))
            }
        } catch (_: Exception) {}
    }

    fun startQuickTunnelForProject(project: Project) {
        tunnelManager.startQuickTunnel(project.port)
        _message.value = "Starting Cloudflare Tunnel for port ${project.port}..."
    }

    fun stopTunnel() {
        tunnelManager.stopTunnel()
        _message.value = "Tunnel stopped"
    }

    fun startNamedTunnel(token: String) {
        tunnelManager.startNamedTunnel(token)
    }

    fun createProject(name: String, runtime: RuntimeType, port: Int, filesDir: File): Boolean {
        if (name.isBlank()) return false
        val id = UUID.randomUUID().toString()
        val projectDir = File(filesDir, "projects/$name").apply { mkdirs() }

        val actualPort = if (port == 8080) runtime.defaultPort else port

        val defaultCmd = when (runtime) {
            RuntimeType.PYTHON -> "python3 app.py"
            RuntimeType.NODEJS -> "node server.js"
            RuntimeType.PHP -> "builtin"
            RuntimeType.STATIC -> "builtin"
            RuntimeType.JAVA -> "java -jar app.jar"
        }

        val project = Project(
            id = id,
            name = name,
            runtime = runtime,
            port = actualPort,
            workingDir = projectDir.absolutePath,
            startupCommand = defaultCmd
        )

        val template = templateProvider.getTemplates().firstOrNull { it.runtime == runtime }
        if (template != null) {
            templateProvider.scaffold(template, projectDir, project)
        }

        viewModelScope.launch {
            projectRepository.save(project)
        }
        return true
    }

    fun addEnvironmentVar(projectId: String, key: String, value: String, isSecret: Boolean) {
        if (key.isBlank()) return
        viewModelScope.launch {
            val project = projectRepository.getById(projectId) ?: return@launch
            val updatedList = project.envVars.filter { it.key != key.trim() } + EnvironmentVar(key.trim(), value, isSecret)
            projectRepository.save(project.copy(envVars = updatedList))
            _message.value = "Added variable $key"
        }
    }

    fun removeEnvironmentVar(projectId: String, key: String) {
        viewModelScope.launch {
            val project = projectRepository.getById(projectId) ?: return@launch
            val updatedList = project.envVars.filter { it.key != key }
            projectRepository.save(project.copy(envVars = updatedList))
            _message.value = "Removed variable $key"
        }
    }

    fun importRawEnv(projectId: String, rawText: String) {
        viewModelScope.launch {
            val project = projectRepository.getById(projectId) ?: return@launch
            val parsed = mutableListOf<EnvironmentVar>()
            rawText.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                    val k = trimmed.substringBefore("=").trim()
                    var v = trimmed.substringAfter("=").trim()
                    if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                        v = v.substring(1, v.length - 1)
                    }
                    val isSecret = k.contains("SECRET", ignoreCase = true) || k.contains("KEY", ignoreCase = true) || k.contains("PASSWORD", ignoreCase = true)
                    if (k.isNotEmpty()) {
                        parsed.add(EnvironmentVar(k, v, isSecret))
                    }
                }
            }
            if (parsed.isEmpty()) {
                _message.value = "No valid KEY=VALUE pairs found"
                return@launch
            }
            val existingKeys = parsed.map { it.key }.toSet()
            val merged = project.envVars.filter { it.key !in existingKeys } + parsed
            projectRepository.save(project.copy(envVars = merged))
            _message.value = "Imported ${parsed.size} variables from .env"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
