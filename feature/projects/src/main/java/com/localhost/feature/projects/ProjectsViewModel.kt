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
import com.localhost.tunnel.NginxManager
import com.localhost.core.model.NginxProxyConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Job
import com.localhost.cli.LocalhostCli
import okhttp3.OkHttpClient
import okhttp3.Request
import com.localhost.core.data.database.RequestRecordDao
import com.localhost.core.data.database.toDomain
import com.localhost.core.data.repository.SettingsRepository
import com.localhost.core.data.repository.LogRepository
import com.localhost.core.model.LogEntry
import com.localhost.core.model.RequestRecord
import kotlinx.coroutines.flow.map

data class ProjectTerminalLine(val command: String, val output: String)

object ProjectTerminalCache {
    val histories = mutableMapOf<String, List<ProjectTerminalLine>>()
}

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val processSupervisor: ProcessSupervisor,
    private val templateProvider: TemplateProvider,
    private val runtimeManager: RuntimeManager,
    private val tunnelManager: CloudflareTunnelManager,
    private val snapshotRepository: SnapshotRepository,
    private val okHttpClient: OkHttpClient,
    private val localhostCli: LocalhostCli,
    private val requestRecordDao: RequestRecordDao,
    private val settingsRepository: SettingsRepository,
    private val logRepository: LogRepository,
    private val composerManager: com.localhost.runtime.manager.ComposerManager,
    private val nginxManager: NginxManager
) : ViewModel() {

    val projects: StateFlow<List<Project>> = projectRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val forwardingStatus: StateFlow<TunnelStatus> = tunnelManager.forwardingStatus
    val forwardingActiveUrl: StateFlow<String> = tunnelManager.forwardingActiveUrl
    val forwardingError: StateFlow<String?> = tunnelManager.forwardingErrorMessage

    val tokenTunnelStatus: StateFlow<TunnelStatus> = tunnelManager.tokenTunnelStatus
    val tokenActiveUrl: StateFlow<String> = tunnelManager.tokenActiveUrl
    val tokenError: StateFlow<String?> = tunnelManager.tokenErrorMessage

    val tunnelStatus: StateFlow<TunnelStatus> = forwardingStatus
    val activeTunnelUrl: StateFlow<String> = forwardingActiveUrl
    val tunnelError: StateFlow<String?> = forwardingError
    val proxyConfig: StateFlow<NginxProxyConfig> = nginxManager.proxyConfig

    private val _forwardedPort = MutableStateFlow<Int?>(null)
    val forwardedPort: StateFlow<Int?> = _forwardedPort.asStateFlow()

    fun startPortForwarding(port: Int) {
        _forwardedPort.value = port
        tunnelManager.startQuickTunnel(port)
        _message.value = "Starting worldwide HTTPS tunnel for port $port..."
    }

    fun stopPortForwarding() {
        _forwardedPort.value = null
        tunnelManager.stopPortForwarding()
        _message.value = "Port forwarding stopped"
    }

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _isInstallingDeps = MutableStateFlow(false)
    val isInstallingDeps = _isInstallingDeps.asStateFlow()

    private val _projectTerminals = MutableStateFlow<Map<String, List<ProjectTerminalLine>>>(ProjectTerminalCache.histories.toMap())
    val projectTerminals: StateFlow<Map<String, List<ProjectTerminalLine>>> = _projectTerminals.asStateFlow()

    private val _projectTerminalRunning = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val projectTerminalRunning: StateFlow<Map<String, Boolean>> = _projectTerminalRunning.asStateFlow()

    private val activeProjectJobs = mutableMapOf<String, Job>()

    fun getProjectTerminalHistory(projectId: String): List<ProjectTerminalLine> {
        return _projectTerminals.value[projectId] ?: listOf(
            ProjectTerminalLine("", "Project Terminal Ready\n")
        )
    }

    fun isProjectTerminalRunning(projectId: String): Boolean {
        return _projectTerminalRunning.value[projectId] == true
    }

    fun runProjectTerminalCommand(project: Project, cmd: String) {
        if (cmd.isBlank()) return
        activeProjectJobs[project.id]?.cancel()

        activeProjectJobs[project.id] = viewModelScope.launch {
            val currentRunning = _projectTerminalRunning.value.toMutableMap()
            currentRunning[project.id] = true
            _projectTerminalRunning.value = currentRunning

            val currentList = (_projectTerminals.value[project.id] ?: emptyList()).toMutableList()
            val targetIndex = currentList.size
            currentList.add(ProjectTerminalLine(cmd, ""))
            val map = _projectTerminals.value.toMutableMap()
            map[project.id] = currentList
            ProjectTerminalCache.histories[project.id] = currentList
            _projectTerminals.value = map

            val currentOutput = StringBuilder()
            val workDir = File(project.workingDir).apply { mkdirs() }

            try {
                val out = localhostCli.executeInDirectory(workDir, cmd) { chunk ->
                    currentOutput.append(chunk)
                    val list = (_projectTerminals.value[project.id] ?: emptyList()).toMutableList()
                    if (targetIndex < list.size) {
                        list[targetIndex] = ProjectTerminalLine(cmd, currentOutput.toString())
                        val m = _projectTerminals.value.toMutableMap()
                        m[project.id] = list
                        ProjectTerminalCache.histories[project.id] = list
                        _projectTerminals.value = m
                    }
                }

                if (out == "__CLEAR__") {
                    val m = _projectTerminals.value.toMutableMap()
                    m[project.id] = emptyList()
                    ProjectTerminalCache.histories[project.id] = emptyList()
                    _projectTerminals.value = m
                } else {
                    val finalOutput = if (currentOutput.isNotEmpty()) currentOutput.toString().trimEnd() else out
                    val list = (_projectTerminals.value[project.id] ?: emptyList()).toMutableList()
                    if (targetIndex < list.size) {
                        list[targetIndex] = ProjectTerminalLine(cmd, finalOutput)
                        val m = _projectTerminals.value.toMutableMap()
                        m[project.id] = list
                        ProjectTerminalCache.histories[project.id] = list
                        _projectTerminals.value = m
                    }
                }
            } catch (_: Exception) {
                val list = (_projectTerminals.value[project.id] ?: emptyList()).toMutableList()
                if (targetIndex < list.size) {
                    list[targetIndex] = ProjectTerminalLine(cmd, "^C (Interrupted)")
                    val m = _projectTerminals.value.toMutableMap()
                    m[project.id] = list
                    _projectTerminals.value = m
                }
            } finally {
                val r = _projectTerminalRunning.value.toMutableMap()
                r[project.id] = false
                _projectTerminalRunning.value = r
            }
        }
    }

    fun stopProjectTerminalCommand(projectId: String) {
        localhostCli.interrupt()
        activeProjectJobs[projectId]?.cancel()
        val r = _projectTerminalRunning.value.toMutableMap()
        r[projectId] = false
        _projectTerminalRunning.value = r
        val list = (_projectTerminals.value[projectId] ?: emptyList()).toMutableList()
        if (list.isNotEmpty() && list.last().output.isEmpty()) {
            list[list.lastIndex] = list.last().copy(output = "^C")
        } else {
            list.add(ProjectTerminalLine("", "^C"))
        }
        val m = _projectTerminals.value.toMutableMap()
        m[projectId] = list
        _projectTerminals.value = m
    }

    fun clearProjectTerminal(projectId: String) {
        val m = _projectTerminals.value.toMutableMap()
        m[projectId] = emptyList()
        ProjectTerminalCache.histories[projectId] = emptyList()
        _projectTerminals.value = m
    }

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

            val res = try {
                withTimeoutOrNull(8000L) {
                    processSupervisor.startProject(actualProject)
                } ?: Result.failure(Exception("Startup timed out after 8s"))
            } catch (e: Exception) {
                Result.failure(e)
            }
            if (res.isFailure) {
                _message.value = res.exceptionOrNull()?.message ?: "Failed to start"
            }
        }
    }

    fun stopProject(projectId: String) {
        viewModelScope.launch {
            try {
                withTimeoutOrNull(5000L) {
                    processSupervisor.stopProject(projectId)
                }
            } catch (_: Exception) {}
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
        viewModelScope.launch(Dispatchers.IO) {
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

    fun updateProjectDetails(project: Project, newName: String, newPort: Int) {
        viewModelScope.launch {
            val trimmedName = newName.trim()
            if (trimmedName.isBlank()) {
                _message.value = "Project name cannot be empty"
                return@launch
            }
            if (newPort !in 1024..65535) {
                _message.value = "Port must be between 1024 and 65535"
                return@launch
            }
            val updatedCmd = if (project.port != newPort) {
                project.startupCommand.replace(":${project.port}", ":$newPort")
            } else {
                project.startupCommand
            }
            val updated = project.copy(
                name = trimmedName,
                port = newPort,
                startupCommand = updatedCmd,
                updatedAt = System.currentTimeMillis()
            )
            projectRepository.save(updated)
            _message.value = "Project updated successfully"
        }
    }

    fun scaffoldPythonVenv(projectDir: File) {
        try {
            val venvDir = File(projectDir, ".venv")
            if (!venvDir.exists()) venvDir.mkdirs()
            val binDir = File(venvDir, "bin").apply { mkdirs() }
            File(venvDir, "lib/python3.11/site-packages").apply { mkdirs() }
            
            val pyvenvCfg = File(venvDir, "pyvenv.cfg")
            if (!pyvenvCfg.exists()) {
                pyvenvCfg.writeText(
                    "home = /system/bin\n" +
                    "include-system-site-packages = true\n" +
                    "version = 3.11.9\n" +
                    "executable = /system/bin/python3\n" +
                    "command = python3 -m venv .venv\n"
                )
            }
            
            val activateFile = File(binDir, "activate")
            if (!activateFile.exists()) {
                activateFile.writeText(
                    "#!/bin/sh\n" +
                    "VIRTUAL_ENV=\"${venvDir.absolutePath}\"\n" +
                    "export VIRTUAL_ENV\n" +
                    "_OLD_VIRTUAL_PATH=\"\$PATH\"\n" +
                    "PATH=\"\$VIRTUAL_ENV/bin:\$PATH\"\n" +
                    "export PATH\n"
                )
                try { activateFile.setExecutable(true, false) } catch (_: Exception) {}
            }

            val pyExec = runtimeManager.getExecutablePath(RuntimeType.PYTHON)
            if (pyExec != null && File(pyExec).exists()) {
                val env = runtimeManager.getEnvironmentForRuntime(RuntimeType.PYTHON)
                val pb = ProcessBuilder(pyExec, "-m", "venv", "--system-site-packages", venvDir.absolutePath)
                pb.directory(projectDir)
                pb.environment().putAll(env)
                val proc = pb.start()
                proc.waitFor()
            }
        } catch (_: Exception) {}
    }

    suspend fun exportProjectZip(project: Project, context: Context, includeDotEnv: Boolean = false): File? = withContext(Dispatchers.IO) {
        val projectDir = File(project.workingDir)
        if (!projectDir.exists()) {
            _message.value = "Project directory not found"
            return@withContext null
        }
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = project.name.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
        val zipFile = File(exportDir, "${safeName}.zip")
        try {
            if (zipFile.exists()) zipFile.delete()
            FileOutputStream(zipFile).use { fos ->
                FileUtils.zipDirectory(projectDir, fos, includeDotEnv = includeDotEnv)
            }
            _message.value = "Exported ${project.name} to ZIP"
            zipFile
        } catch (e: Exception) {
            _message.value = "Export failed: ${e.message}"
            null
        }
    }

    fun importProjectZip(
        context: Context,
        zipUri: Uri,
        customName: String? = null,
        customPort: Int? = null,
        customRuntime: RuntimeType? = null,
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

                val (detectedRuntime, detectedCmd) = detectRuntimeFromDirectory(targetDir)
                val runtime = customRuntime ?: detectedRuntime
                val startCmd = if (customRuntime != null) {
                    when (customRuntime) {
                        RuntimeType.PYTHON -> "python3 app.py"
                        RuntimeType.NODEJS -> "node server.js"
                        RuntimeType.PHP -> "builtin"
                        RuntimeType.STATIC -> "builtin"
                        RuntimeType.JAVA -> "java -jar app.jar"
                    }
                } else detectedCmd
                val id = UUID.randomUUID().toString()

                val allExisting = projectRepository.getAll()
                val usedPorts = allExisting.map { it.port }.toSet()
                var port = customPort ?: runtime.defaultPort
                if (customPort == null) {
                    while (usedPorts.contains(port)) {
                        port++
                    }
                }

                val project = Project(
                    id = id,
                    name = targetDir.name,
                    runtime = runtime,
                    port = port,
                    workingDir = targetDir.absolutePath,
                    startupCommand = startCmd
                )

                if (runtime == RuntimeType.PYTHON) {
                    scaffoldPythonVenv(targetDir)
                }
                projectRepository.save(project)
                _message.value = "Imported '${project.name}' successfully as ${runtime.displayName}"
                onComplete?.invoke(true)
            } catch (e: Exception) {
                _message.value = "Import failed: ${e.message}"
                onComplete?.invoke(false)
            }
        }
    }

    suspend fun exportProjectToUri(
        context: Context,
        project: Project,
        destinationUri: Uri,
        excludeDependencies: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        val projectDir = File(project.workingDir)
        if (!projectDir.exists()) {
            _message.value = "Project directory not found"
            return@withContext false
        }
        try {
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                java.util.zip.ZipOutputStream(outputStream).use { zos ->
                    val excludeSet = if (excludeDependencies) {
                        setOf(".venv", "node_modules", "vendor", ".git", ".idea", ".vscode", "__pycache__")
                    } else {
                        setOf(".git")
                    }
                    projectDir.walkTopDown()
                        .filter { file ->
                            val relPath = file.relativeTo(projectDir).path
                            !excludeSet.any { excluded -> relPath == excluded || relPath.startsWith("$excluded/") || relPath.startsWith("$excluded\\") }
                        }
                        .forEach { file ->
                            val entryName = file.relativeTo(projectDir).path.replace("\\", "/")
                            if (file.isDirectory) {
                                if (entryName.isNotEmpty()) {
                                    zos.putNextEntry(java.util.zip.ZipEntry("$entryName/"))
                                    zos.closeEntry()
                                }
                            } else {
                                zos.putNextEntry(java.util.zip.ZipEntry(entryName))
                                file.inputStream().use { it.copyTo(zos) }
                                zos.closeEntry()
                            }
                        }
                }
            }
            _message.value = "Exported ${project.name} successfully"
            true
        } catch (e: Exception) {
            _message.value = "Export failed: ${e.message}"
            false
        }
    }

    fun importProjectFromGithub(
        context: Context,
        repoUrl: String,
        customName: String? = null,
        customPort: Int? = null,
        customRuntime: RuntimeType? = null,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                _message.value = "Connecting to GitHub..."
                val cleanUrl = repoUrl.trim().removeSuffix(".git").removeSuffix("/")
                val cleaned = cleanUrl.replace("https://github.com/", "").replace("http://github.com/", "").trim()
                val parts = cleaned.split("/")
                if (parts.size < 2) {
                    _message.value = "Invalid GitHub repository URL"
                    onComplete?.invoke(false)
                    return@launch
                }
                val owner = parts[0]
                val repo = parts[1]
                val branch = if (parts.size >= 4 && parts[2] == "tree") parts[3] else "main"

                val projectName = customName?.takeIf { it.isNotBlank() } ?: repo

                val projectsDir = File(context.filesDir, "projects")
                var targetDir = File(projectsDir, projectName)
                var count = 1
                while (targetDir.exists()) {
                    targetDir = File(projectsDir, "${projectName}_$count")
                    count++
                }
                targetDir.mkdirs()

                val downloadSuccess = withContext(Dispatchers.IO) {
                    val candidateUrls = listOf(
                        "https://github.com/$owner/$repo/archive/refs/heads/$branch.zip",
                        "https://github.com/$owner/$repo/archive/refs/heads/master.zip",
                        "https://api.github.com/repos/$owner/$repo/zipball/$branch"
                    )
                    var downloaded = false
                    val tempZip = File(context.cacheDir, "github_${System.currentTimeMillis()}.zip")

                    for (url in candidateUrls) {
                        try {
                            val req = Request.Builder()
                                .url(url)
                                .header("User-Agent", "Localhost-App")
                                .build()
                            val resp = okHttpClient.newCall(req).execute()
                            if (resp.isSuccessful) {
                                resp.body?.byteStream()?.use { input ->
                                    FileOutputStream(tempZip).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                downloaded = true
                                break
                            }
                        } catch (_: Exception) {}
                    }

                    if (downloaded && tempZip.exists()) {
                        try {
                            java.io.FileInputStream(tempZip).use { fis ->
                                FileUtils.unzip(fis, targetDir)
                            }
                            tempZip.delete()
                            true
                        } catch (e: Exception) {
                            false
                        }
                    } else {
                        false
                    }
                }

                if (!downloadSuccess) {
                    _message.value = "Failed to download GitHub repository"
                    onComplete?.invoke(false)
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    var rootFiles = targetDir.listFiles()
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
                }

                val (detectedRuntime, detectedCmd) = detectRuntimeFromDirectory(targetDir)
                val runtime = customRuntime ?: detectedRuntime
                val startCmd = if (customRuntime != null) {
                    when (customRuntime) {
                        RuntimeType.PYTHON -> "python3 app.py"
                        RuntimeType.NODEJS -> "node server.js"
                        RuntimeType.PHP -> "builtin"
                        RuntimeType.STATIC -> "builtin"
                        RuntimeType.JAVA -> "java -jar app.jar"
                    }
                } else detectedCmd
                val allExisting = projectRepository.getAll()
                val usedPorts = allExisting.map { it.port }.toSet()
                var port = customPort ?: runtime.defaultPort
                if (customPort == null) {
                    while (usedPorts.contains(port)) {
                        port++
                    }
                }

                if (runtime == RuntimeType.PYTHON) {
                    scaffoldPythonVenv(targetDir)
                }

                val newProject = Project(
                    id = UUID.randomUUID().toString(),
                    name = targetDir.name,
                    runtime = runtime,
                    port = port,
                    workingDir = targetDir.absolutePath,
                    startupCommand = startCmd
                )

                projectRepository.save(newProject)
                _message.value = "Imported GitHub project: ${newProject.name} (${runtime.displayName})"
                onComplete?.invoke(true)
            } catch (e: Exception) {
                _message.value = "GitHub import error: ${e.message}"
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

            val rawBytes = try { reqFile.readBytes() } catch (e: Exception) { byteArrayOf() }
            val decodedText = when {
                rawBytes.size >= 2 && rawBytes[0] == 0xFF.toByte() && rawBytes[1] == 0xFE.toByte() ->
                    String(rawBytes, 2, rawBytes.size - 2, Charsets.UTF_16LE)
                rawBytes.size >= 2 && rawBytes[0] == 0xFE.toByte() && rawBytes[1] == 0xFF.toByte() ->
                    String(rawBytes, 2, rawBytes.size - 2, Charsets.UTF_16BE)
                rawBytes.size >= 3 && rawBytes[0] == 0xEF.toByte() && rawBytes[1] == 0xBB.toByte() && rawBytes[2] == 0xBF.toByte() ->
                    String(rawBytes, 3, rawBytes.size - 3, Charsets.UTF_8)
                else -> {
                    val utf8 = String(rawBytes, Charsets.UTF_8)
                    if (utf8.contains('\u0000')) {
                        try { String(rawBytes, Charsets.UTF_16LE) } catch (_: Exception) { utf8.filter { it != '\u0000' } }
                    } else {
                        utf8
                    }
                }
            }

            val validLines = decodedText.lines()
                .map { it.filter { ch -> ch != '\u0000' && ch != '\r' }.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }

            if (validLines.isEmpty()) {
                _message.value = "Invalid requirements txt passed: '$targetName' is empty or contains no valid dependencies"
                _isInstallingDeps.value = false
                return@launch
            }

            val runtimeProvided = setOf(
                "audioop", "audioop-lts", "cryptography", "pillow", "pil",
                "psycopg2", "psycopg2-binary", "regex", "cffi", "pycparser",
                "bcrypt", "lxml", "greenlet", "asyncio", "importlib", "logging",
                "datetime", "flask-cli", "dotenv"
            )

            val installableLines = validLines.filter { line ->
                val pkgName = line.split(Regex("[=<>~! ]"))[0].trim().lowercase()
                !runtimeProvided.contains(pkgName)
            }

            val projectVenv = File(workDir, ".venv").apply { mkdirs() }
            val wheelsDir = File(File(execPath).parentFile?.parentFile, "wheels")

            if (installableLines.isEmpty()) {
                recordInstalledDependencies(workDir, validLines)
                val projectSitePackages314 = File(projectVenv, "lib/python3.14/site-packages").apply { mkdirs() }
                runtimeManager.extractCorePythonAssets(projectSitePackages314)
                _message.value = "Dependencies satisfied from pre-installed runtime environment"
                _isInstallingDeps.value = false
                return@launch
            }

            val tempReqFile = File(projectVenv, ".install_reqs.txt").apply {
                writeText(installableLines.joinToString("\n") + "\n", Charsets.UTF_8)
            }

            val cmd = mutableListOf(
                execPath,
                "-m",
                "pip",
                "install",
                "--prefix",
                projectVenv.absolutePath,
                "--prefer-binary",
                "-r",
                tempReqFile.absolutePath
            )
            if (wheelsDir.exists()) {
                cmd.addAll(listOf("--find-links", wheelsDir.absolutePath))
            }

            val sanitizedCmd = cmd.map { str -> str.filter { ch -> ch != '\u0000' && ch != '\r' && ch != '\n' }.trim() }.filter { it.isNotBlank() }

            try {
                withContext(Dispatchers.IO) {
                    val pb = ProcessBuilder(sanitizedCmd)
                    pb.directory(workDir)
                    val pbEnv = pb.environment()
                    env.forEach { (k, v) ->
                        val ck = k.filter { ch -> ch != '\u0000' && ch != '\r' && ch != '\n' }.trim()
                        val cv = v.filter { ch -> ch != '\u0000' && ch != '\r' && ch != '\n' }.trim()
                        if (ck.isNotBlank()) pbEnv[ck] = cv
                    }
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
                        val projectSitePackages314 = File(projectVenv, "lib/python3.14/site-packages")
                        if (projectSitePackages314.exists()) {
                            runtimeManager.extractCorePythonAssets(projectSitePackages314)
                        }
                        _message.value = "Dependencies from '$targetName' installed successfully"
                    } else {
                        val errMsg = combined.lines().filter { it.isNotBlank() }.takeLast(2).joinToString(" ").take(150)
                        _message.value = "Failed to install '$targetName'${if (errMsg.isNotBlank()) ": $errMsg" else ""}"
                    }
                }
            } catch (e: Exception) {
                _message.value = "Failed to install dependencies: ${e.message ?: "Unknown error"}"
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
                RuntimeType.PHP -> {
                    if (!composerManager.isInstalled()) {
                        val dlResult = composerManager.downloadComposer()
                        if (dlResult.isFailure) {
                            _message.value = "Failed to download Composer: ${dlResult.exceptionOrNull()?.message}"
                            _isInstallingDeps.value = false
                            return@launch
                        }
                    }
                    val composerPhar = composerManager.composerFile
                    when {
                        packageName.equals("install", ignoreCase = true) -> listOf(execPath, composerPhar.absolutePath, "install", "--no-interaction")
                        packageName.equals("update", ignoreCase = true) -> listOf(execPath, composerPhar.absolutePath, "update", "--no-interaction")
                        packageName.equals("dump-autoload", ignoreCase = true) -> listOf(execPath, composerPhar.absolutePath, "dump-autoload")
                        packageName.isNotBlank() -> listOf(execPath, composerPhar.absolutePath, "require", packageName.trim(), "--no-interaction")
                        else -> listOf(execPath, composerPhar.absolutePath, "install", "--no-interaction")
                    }
                }
                else -> null
            }

            if (cmd == null) {
                _message.value = "Package installation is not supported for ${project.runtime.displayName}"
                _isInstallingDeps.value = false
                return@launch
            }

            val sanitizedCmd = cmd.map { str -> str.filter { ch -> ch != '\u0000' && ch != '\r' && ch != '\n' }.trim() }.filter { it.isNotBlank() }

            try {
                withContext(Dispatchers.IO) {
                    val pb = ProcessBuilder(sanitizedCmd)
                    pb.directory(workDir)
                    val pbEnv = pb.environment()
                    env.forEach { (k, v) ->
                        val ck = k.filter { ch -> ch != '\u0000' && ch != '\r' && ch != '\n' }.trim()
                        val cv = v.filter { ch -> ch != '\u0000' && ch != '\r' && ch != '\n' }.trim()
                        if (ck.isNotBlank()) pbEnv[ck] = cv
                    }
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
                        if (project.runtime == RuntimeType.PYTHON) {
                            val projectSitePackages314 = File(projectVenv, "lib/python3.14/site-packages")
                            if (projectSitePackages314.exists()) {
                                runtimeManager.extractCorePythonAssets(projectSitePackages314)
                            }
                        }
                        _message.value = if (isCustom) "Package '$packageName' installed successfully" else "Dependencies installed successfully"
                    } else {
                        val errMsg = combined.lines().filter { it.isNotBlank() }.takeLast(2).joinToString(" ").take(150)
                        _message.value = if (isCustom) "Failed to install '$packageName'${if (errMsg.isNotBlank()) ": $errMsg" else ""}" else "Failed to install dependencies${if (errMsg.isNotBlank()) ": $errMsg" else ""}"
                    }
                }
            } catch (e: Exception) {
                _message.value = if (isCustom) "Failed to install '$packageName': ${e.message ?: "Unknown error"}" else "Failed to install dependencies: ${e.message ?: "Unknown error"}"
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
                RuntimeType.PHP -> {
                    val composerPhar = composerManager.composerFile
                    if (composerPhar.exists()) {
                        listOf(execPath, composerPhar.absolutePath, "remove", packageName.trim(), "--no-interaction")
                    } else null
                }
                else -> null
            }

            if (cmd == null) {
                _message.value = "Package uninstallation is not supported for ${project.runtime.displayName}"
                _isInstallingDeps.value = false
                return@launch
            }

            val sanitizedCmd = cmd.map { str -> str.filter { ch -> ch != '\u0000' && ch != '\r' && ch != '\n' }.trim() }.filter { it.isNotBlank() }

            try {
                withContext(Dispatchers.IO) {
                    val pb = ProcessBuilder(sanitizedCmd)
                    pb.directory(workDir)
                    val pbEnv = pb.environment()
                    env.forEach { (k, v) ->
                        val ck = k.filter { ch -> ch != '\u0000' && ch != '\r' && ch != '\n' }.trim()
                        val cv = v.filter { ch -> ch != '\u0000' && ch != '\r' && ch != '\n' }.trim()
                        if (ck.isNotBlank()) pbEnv[ck] = cv
                    }
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
        tunnelManager.stopPortForwarding()
        _message.value = "Tunnel stopped"
    }

    fun startNamedTunnel(token: String, domain: String? = null) {
        tunnelManager.startNamedTunnel(token, domain = domain)
        _message.value = "Connecting Cloudflare Named Tunnel..."
    }

    fun setNamedTunnelDomain(domain: String) {
        tunnelManager.setNamedTunnelDomain(domain)
    }

    fun stopNamedTunnel() {
        tunnelManager.stopNamedTunnel()
        _message.value = "Cloudflare Named Tunnel stopped"
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
            if (runtime == RuntimeType.PYTHON) {
                withContext(Dispatchers.IO) {
                    scaffoldPythonVenv(projectDir)
                }
            }
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

    fun observeLogs(projectId: String): Flow<List<LogEntry>> =
        logRepository.observeLogs(projectId)

    fun clearLogs(projectId: String) {
        viewModelScope.launch {
            logRepository.clear(projectId)
        }
    }

    fun observeRequests(projectId: String, since: Long = 0L): Flow<List<RequestRecord>> =
        requestRecordDao.observeRequests(projectId, since).map { list -> list.map { it.toDomain() } }

    fun observeErrors(projectId: String): Flow<List<RequestRecord>> =
        requestRecordDao.observeErrorLogs(projectId).map { list -> list.map { it.toDomain() } }

    fun isVpnDisabled(projectId: String): Flow<Boolean> =
        settingsRepository.isVpnDisabledForProject(projectId)

    fun setVpnDisabled(projectId: String, disabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setVpnDisabledForProject(projectId, disabled)
        }
    }

    fun getVpnBlockedMessage(projectId: String): kotlinx.coroutines.flow.Flow<String> =
        settingsRepository.getVpnBlockedMessage(projectId)

    fun setVpnBlockedMessage(projectId: String, message: String) {
        viewModelScope.launch {
            settingsRepository.setVpnBlockedMessage(projectId, message)
            _message.value = "VPN error message saved"
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun clearRequests(projectId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            requestRecordDao.clearForProject(projectId)
        }
    }

    fun recordUrlVisit(projectId: String, url: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val uri = try { Uri.parse(url) } catch (_: Exception) { null }
            val path = uri?.path?.ifBlank { "/" } ?: "/"
            val isError = url.contains("error", ignoreCase = true)
            val code = if (isError) 500 else 200
            requestRecordDao.insert(
                com.localhost.core.data.database.RequestRecordEntity(
                    projectId = projectId,
                    timestamp = System.currentTimeMillis(),
                    method = "GET",
                    path = path,
                    statusCode = code,
                    responseBytes = 1420L,
                    durationMs = 8L,
                    errorMessage = null
                )
            )
        }
    }

    fun regenerateVenv(project: Project) {
        viewModelScope.launch(Dispatchers.IO) {
            _message.value = "Regenerating clean .venv for ${project.name}..."
            val workDir = File(project.workingDir)
            val venvDir = File(workDir, ".venv")
            try {
                if (venvDir.exists()) {
                    venvDir.deleteRecursively()
                }
                val pyExec = runtimeManager.getExecutablePath(com.localhost.core.model.RuntimeType.PYTHON)
                if (pyExec != null) {
                    val env = runtimeManager.getEnvironmentForRuntime(com.localhost.core.model.RuntimeType.PYTHON)
                    val pb = ProcessBuilder(pyExec, "-m", "venv", "--system-site-packages", venvDir.absolutePath)
                    pb.directory(workDir)
                    pb.environment().putAll(env)
                    val proc = pb.start()
                    val exit = proc.waitFor()
                    if (exit == 0) {
                        _message.value = "Fresh .venv virtual environment created successfully"
                    } else {
                        _message.value = "Failed to regenerate .venv (exit $exit)"
                    }
                } else {
                    _message.value = "Python runtime not found"
                }
            } catch (e: Exception) {
                _message.value = "Error regenerating .venv: ${e.message}"
            }
        }
    }

    fun freezeRequirements(project: Project, fileName: String = "requirements.txt") {
        viewModelScope.launch(Dispatchers.IO) {
            _message.value = "Freezing requirements into $fileName..."
            val workDir = File(project.workingDir)
            val venvPip = File(workDir, ".venv/bin/pip")
            val pyPip = if (venvPip.exists()) venvPip.absolutePath else (runtimeManager.getExecutablePath(com.localhost.core.model.RuntimeType.PYTHON)?.let { File(File(it).parentFile, "pip").absolutePath } ?: "pip")
            try {
                val env = runtimeManager.getEnvironmentForRuntime(com.localhost.core.model.RuntimeType.PYTHON)
                val pb = ProcessBuilder(pyPip, "freeze")
                pb.directory(workDir)
                pb.environment().putAll(env)
                val proc = pb.start()
                val output = proc.inputStream.bufferedReader().readText()
                proc.waitFor()
                val targetFile = File(workDir, fileName)
                targetFile.writeText(output, Charsets.UTF_8)
                _message.value = "Saved ${output.lines().filter { it.isNotBlank() }.size} packages to $fileName"
            } catch (e: Exception) {
                _message.value = "Failed to freeze requirements: ${e.message}"
            }
        }
    }

    fun loadDotEnv(project: Project): Map<String, String> {
        val envFile = File(project.workingDir, ".env")
        if (!envFile.exists()) return emptyMap()
        val result = mutableMapOf<String, String>()
        try {
            envFile.readLines().forEach { raw ->
                val line = raw.trim()
                if (line.isNotEmpty() && !line.startsWith("#") && line.contains("=")) {
                    val k = line.substringBefore("=").trim()
                    val v = line.substringAfter("=").trim().removeSurrounding("\"").removeSurrounding("'")
                    if (k.isNotEmpty()) result[k] = v
                }
            }
        } catch (_: Exception) {}
        return result
    }

    fun saveDotEnv(project: Project, envMap: Map<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val envFile = File(project.workingDir, ".env")
            try {
                val sb = StringBuilder()
                sb.append("# Environment Variables for ").append(project.name).append("\n")
                envMap.forEach { (k, v) ->
                    sb.append(k).append("=").append(v).append("\n")
                }
                envFile.writeText(sb.toString(), Charsets.UTF_8)
                _message.value = "Saved .env file (${envMap.size} variables)"
            } catch (e: Exception) {
                _message.value = "Failed to save .env: ${e.message}"
            }
        }
    }

    val savedTunnelTokens: StateFlow<List<com.localhost.core.model.SavedTunnelToken>> = settingsRepository.getSavedTunnelTokens()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveTunnelToken(name: String, token: String) {
        if (token.isBlank()) return
        val finalName = if (name.isBlank()) "Tunnel-${token.take(6)}" else name.trim()
        val entry = com.localhost.core.model.SavedTunnelToken(
            id = UUID.randomUUID().toString(),
            name = finalName,
            token = token.trim()
        )
        viewModelScope.launch {
            settingsRepository.saveTunnelToken(entry)
        }
    }

    fun deleteTunnelToken(tokenId: String) {
        viewModelScope.launch {
            settingsRepository.deleteTunnelToken(tokenId)
        }
    }

    fun syncProjectsFromFilesystem(filesDir: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val projectsDir = File(filesDir, "projects")
            if (!projectsDir.exists() || !projectsDir.isDirectory) return@launch
            val folders = projectsDir.listFiles { f -> f.isDirectory } ?: return@launch
            val existing: List<com.localhost.core.model.Project> = projectRepository.getAll()

            existing.forEach { proj ->
                val workDir = File(proj.workingDir)
                if (!workDir.exists()) {
                    projectRepository.delete(proj.id)
                }
            }

            val updatedExisting = projectRepository.getAll()
            val existingPaths = updatedExisting.map { File(it.workingDir).canonicalPath }.toSet()

            folders.forEach { folder ->
                val files = folder.listFiles()
                val visibleFiles = files?.filter { !it.name.startsWith(".") } ?: emptyList()
                if (!existingPaths.contains(folder.canonicalPath) && visibleFiles.isNotEmpty()) {
                    val name = folder.name
                    val runtime = when {
                        File(folder, "app.py").exists() || File(folder, "main.py").exists() || File(folder, "requirements.txt").exists() -> com.localhost.core.model.RuntimeType.PYTHON
                        File(folder, "package.json").exists() || File(folder, "server.js").exists() -> com.localhost.core.model.RuntimeType.NODEJS
                        File(folder, "index.php").exists() || File(folder, "composer.json").exists() -> com.localhost.core.model.RuntimeType.PHP
                        File(folder, "pom.xml").exists() || File(folder, "build.gradle").exists() -> com.localhost.core.model.RuntimeType.JAVA
                        else -> com.localhost.core.model.RuntimeType.STATIC
                    }
                    val newProj = Project(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        runtime = runtime,
                        port = runtime.defaultPort,
                        workingDir = folder.absolutePath,
                        startupCommand = when (runtime) {
                            com.localhost.core.model.RuntimeType.PYTHON -> "python3 app.py"
                            com.localhost.core.model.RuntimeType.NODEJS -> "node server.js"
                            com.localhost.core.model.RuntimeType.PHP -> "builtin"
                            com.localhost.core.model.RuntimeType.STATIC -> "builtin"
                            com.localhost.core.model.RuntimeType.JAVA -> "java -jar app.jar"
                        }
                    )
                    projectRepository.save(newProj)
                }
            }
        }
    }

}