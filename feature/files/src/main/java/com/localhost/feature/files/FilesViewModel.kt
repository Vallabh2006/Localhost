package com.localhost.feature.files

import android.content.Context
import android.net.Uri
import com.localhost.core.common.FileUtils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.localhost.core.data.repository.ProjectRepository
import com.localhost.core.data.repository.SnapshotRepository
import com.localhost.core.model.Project
import com.localhost.core.model.ProjectSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

enum class FileType {
    TEXT,
    IMAGE,
    SVG,
    PDF,
    ARCHIVE,
    BINARY
}

data class UiFile(
    val file: File,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val fileType: FileType
)

@HiltViewModel
class FilesViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val snapshotRepository: SnapshotRepository
) : ViewModel() {

    val projects: StateFlow<List<Project>> = projectRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProjectId = MutableStateFlow<String?>(null)
    val selectedProjectId = _selectedProjectId.asStateFlow()

    private val _currentDir = MutableStateFlow<File?>(null)
    val currentDir = _currentDir.asStateFlow()

    private val _files = MutableStateFlow<List<UiFile>>(emptyList())
    val files = _files.asStateFlow()

    private val _editingFile = MutableStateFlow<File?>(null)
    val editingFile = _editingFile.asStateFlow()

    private val _editingFileType = MutableStateFlow(FileType.TEXT)
    val editingFileType = _editingFileType.asStateFlow()

    private val _fileContent = MutableStateFlow("")
    val fileContent = _fileContent.asStateFlow()

    private val _imageBitmap = MutableStateFlow<Bitmap?>(null)
    val imageBitmap = _imageBitmap.asStateFlow()

    private val _pdfPageBitmap = MutableStateFlow<Bitmap?>(null)
    val pdfPageBitmap = _pdfPageBitmap.asStateFlow()

    private val _pdfPageCount = MutableStateFlow(0)
    val pdfPageCount = _pdfPageCount.asStateFlow()

    private val _currentPdfPage = MutableStateFlow(0)
    val currentPdfPage = _currentPdfPage.asStateFlow()

    private val _snapshots = MutableStateFlow<List<ProjectSnapshot>>(emptyList())
    val snapshots = _snapshots.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun selectProject(id: String) {
        _selectedProjectId.value = id
        viewModelScope.launch {
            val project = projectRepository.getById(id)
            if (project != null) {
                val dir = File(project.workingDir).apply { mkdirs() }
                _currentDir.value = dir
                loadFiles(dir)
                loadSnapshots(id)
            }
        }
    }

    private fun loadSnapshots(projectId: String) {
        viewModelScope.launch {
            snapshotRepository.observeSnapshots(projectId).collect {
                _snapshots.value = it
            }
        }
    }

    fun navigateTo(dir: File) {
        _currentDir.value = dir
        loadFiles(dir)
    }

    fun navigateUp() {
        val current = _currentDir.value ?: return
        val parent = current.parentFile
        if (parent != null && parent.exists()) {
            _currentDir.value = parent
            loadFiles(parent)
        }
    }

    fun openFile(file: File) {
        val type = detectFileType(file)
        _editingFile.value = file
        _editingFileType.value = type

        viewModelScope.launch(Dispatchers.IO) {
            when (type) {
                FileType.TEXT, FileType.SVG -> {
                    val content = try {
                        file.readText(Charsets.UTF_8)
                    } catch (_: Exception) {
                        "Binary file or cannot read as UTF-8."
                    }
                    _fileContent.value = content
                }
                FileType.IMAGE -> {
                    try {
                        val bmp = BitmapFactory.decodeFile(file.absolutePath)
                        _imageBitmap.value = bmp
                    } catch (_: Exception) {
                        _imageBitmap.value = null
                    }
                }
                FileType.PDF -> {
                    loadPdf(file, 0)
                }
                FileType.ARCHIVE -> {
                    val size = file.length()
                    _fileContent.value = "Archive: ${file.name}\nSize: $size bytes\nPath: ${file.absolutePath}"
                }
                FileType.BINARY -> {
                    val size = file.length()
                    _fileContent.value = "Binary File: ${file.name}\nSize: $size bytes\nPath: ${file.absolutePath}"
                }
            }
        }
    }

    private fun loadPdf(file: File, pageIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                _pdfPageCount.value = renderer.pageCount
                val validPage = pageIndex.coerceIn(0, (renderer.pageCount - 1).coerceAtLeast(0))
                _currentPdfPage.value = validPage

                if (renderer.pageCount > 0) {
                    val page = renderer.openPage(validPage)
                    val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    _pdfPageBitmap.value = bitmap
                }
                renderer.close()
                pfd.close()
            } catch (e: Exception) {
                _pdfPageBitmap.value = null
                _message.value = "Error rendering PDF: ${e.message}"
            }
        }
    }

    fun nextPdfPage() {
        val file = _editingFile.value ?: return
        if (_currentPdfPage.value + 1 < _pdfPageCount.value) {
            loadPdf(file, _currentPdfPage.value + 1)
        }
    }

    fun previousPdfPage() {
        val file = _editingFile.value ?: return
        if (_currentPdfPage.value > 0) {
            loadPdf(file, _currentPdfPage.value - 1)
        }
    }

    fun closeEditor() {
        _editingFile.value = null
        _fileContent.value = ""
        _imageBitmap.value = null
        _pdfPageBitmap.value = null
        _pdfPageCount.value = 0
        _currentPdfPage.value = 0
    }

    fun saveContent(newContent: String) {
        val file = _editingFile.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                file.writeText(newContent, Charsets.UTF_8)
                _fileContent.value = newContent
                _message.value = "Saved ${file.name}"
            } catch (e: Exception) {
                _message.value = "Save failed: ${e.message}"
            }
        }
    }

    fun createFile(name: String, isDir: Boolean) {
        val parent = _currentDir.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val target = File(parent, name)
            if (isDir) target.mkdirs() else target.createNewFile()
            loadFiles(parent)
            _message.value = if (isDir) "Created folder $name" else "Created file $name"
        }
    }

    fun deleteFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            if (file.isDirectory) file.deleteRecursively() else file.delete()
            _currentDir.value?.let { loadFiles(it) }
            _message.value = "Deleted ${file.name}"
        }
    }

    fun createCommit(message: String) {
        val projectId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            val project = projectRepository.getById(projectId) ?: return@launch
            val res = snapshotRepository.createSnapshot(project, message)
            if (res.isSuccess) {
                _message.value = "Commit '${res.getOrNull()?.message}' saved"
            } else {
                _message.value = "Commit failed: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun rollbackToCommit(snapshot: ProjectSnapshot) {
        val projectId = _selectedProjectId.value ?: return
        viewModelScope.launch {
            val project = projectRepository.getById(projectId) ?: return@launch
            val res = snapshotRepository.rollbackSnapshot(project, snapshot)
            if (res.isSuccess) {
                _message.value = "Reverted project to commit ${snapshot.id}"
                _currentDir.value?.let { loadFiles(it) }
            } else {
                _message.value = "Rollback failed: ${res.exceptionOrNull()?.message}"
            }
        }
    }

    fun deleteCommit(snapshot: ProjectSnapshot) {
        viewModelScope.launch {
            snapshotRepository.deleteSnapshot(snapshot)
            _message.value = "Removed snapshot ${snapshot.id}"
        }
    }

    fun importZip(context: Context, zipUri: Uri) {
        val parent = _currentDir.value ?: return
        viewModelScope.launch {
            _message.value = "Extracting ZIP archive..."
            val success = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(zipUri)?.use { input ->
                        FileUtils.unzip(input, parent)
                    }
                    true
                } catch (e: Exception) {
                    false
                }
            }
            if (success) {
                loadFiles(parent)
                _message.value = "Archive extracted successfully"
            } else {
                _message.value = "Failed to extract ZIP archive"
            }
        }
    }

    fun extractArchiveFile(file: File) {
        val parent = _currentDir.value ?: file.parentFile ?: return
        viewModelScope.launch {
            _message.value = "Extracting ${file.name}..."
            val success = withContext(Dispatchers.IO) {
                try {
                    FileUtils.extractArchive(file, parent)
                    true
                } catch (e: Exception) {
                    false
                }
            }
            if (success) {
                loadFiles(parent)
                _message.value = "Extracted ${file.name} successfully"
            } else {
                _message.value = "Failed to extract ${file.name}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun loadFiles(dir: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val list = dir.listFiles()?.filter { it.name != ".localhost_vcs" }?.map {
                UiFile(
                    file = it,
                    name = it.name,
                    isDirectory = it.isDirectory,
                    size = if (it.isFile) it.length() else 0L,
                    lastModified = it.lastModified(),
                    fileType = if (it.isDirectory) FileType.TEXT else detectFileType(it)
                )
            }?.sortedWith(compareByDescending<UiFile> { it.isDirectory }.thenBy { it.name.lowercase() }) ?: emptyList()

            _files.value = list
        }
    }

    private fun detectFileType(file: File): FileType {
        val ext = file.extension.lowercase()
        return when (ext) {
            "png", "jpg", "jpeg", "webp", "gif", "bmp", "ico" -> FileType.IMAGE
            "svg" -> FileType.SVG
            "pdf" -> FileType.PDF
            "zip", "tar", "gz", "tgz", "bz2", "xz", "7z", "rar" -> FileType.ARCHIVE
            "py", "js", "ts", "html", "htm", "css", "json", "md", "txt", "env",
            "sh", "bash", "yml", "yaml", "xml", "php", "sql", "kt", "java", "c",
            "cpp", "h", "rs", "go", "properties", "gradle", "toml", "ini", "conf" -> FileType.TEXT
            else -> {
                if (isProbableTextFile(file)) FileType.TEXT else FileType.BINARY
            }
        }
    }

    private fun isProbableTextFile(file: File): Boolean {
        return try {
            val bytes = ByteArray(512)
            val read = file.inputStream().use { it.read(bytes) }
            if (read <= 0) return true
            for (i in 0 until read) {
                val b = bytes[i].toInt()
                if (b == 0) return false
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
