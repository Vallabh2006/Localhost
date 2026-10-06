package com.localhost.core.common

import android.os.Build
import android.system.Os
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileUtils {

    fun calculateDirectorySize(dir: File): Long {
        var size = 0L
        try {
            if (!dir.exists()) return 0L
            if (dir.isFile) return dir.length()
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) calculateDirectorySize(file) else file.length()
            }
        } catch (_: Throwable) {}
        return size
    }


    fun isSafePath(entryPath: String): Boolean {
        val normalized = entryPath.replace('\\', '/')
        if (normalized.startsWith("/")) return false
        val parts = normalized.split("/")
        var depth = 0
        for (part in parts) {
            when (part) {
                ".." -> {
                    depth--
                    if (depth < 0) return false
                }
                ".", "" -> { /* no-op */ }
                else -> depth++
            }
        }
        return true
    }

    fun deleteRecursively(file: File): Boolean {
        if (!file.exists()) return true
        if (file.isDirectory) {
            file.listFiles()?.forEach { deleteRecursively(it) }
        }
        return file.delete()
    }

    fun zipDirectory(
        sourceDir: File,
        outputStream: OutputStream,
        excludeDirNames: Set<String> = setOf(".venv", "node_modules", ".tmp", "__pycache__", ".git", ".idea", ".vscode")
    ) {
        ZipOutputStream(outputStream).use { zos ->
            val files = sourceDir.walkTopDown()
                .onEnter { dir -> !excludeDirNames.contains(dir.name) }
                .filter { it.isFile }
            for (file in files) {
                try {
                    val relativePath = file.relativeTo(sourceDir).path
                    if (isSafePath(relativePath)) {
                        zos.putNextEntry(ZipEntry(relativePath))
                        file.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun unzip(inputStream: InputStream, targetDir: File) {
        if (!targetDir.exists()) targetDir.mkdirs()
        ZipInputStream(inputStream, StandardCharsets.UTF_8).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                var cleanName = entry.name.replace('\\', '/')
                while (cleanName.startsWith("./") || cleanName.startsWith("/")) {
                    cleanName = if (cleanName.startsWith("./")) cleanName.removePrefix("./") else cleanName.removePrefix("/")
                }
                val termuxPrefix = "data/data/com.termux/files/usr/"
                if (cleanName.startsWith(termuxPrefix)) {
                    cleanName = cleanName.removePrefix(termuxPrefix)
                }
                if (isSafePath(cleanName) && cleanName.isNotEmpty()) {
                    val outFile = File(targetDir, cleanName)
                    if (entry.isDirectory || entry.name.endsWith('/')) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { zis.copyTo(it) }
                        try {
                            outFile.setReadable(true, false)
                            outFile.setWritable(true, false)
                            outFile.setExecutable(true, false)
                            Os.chmod(outFile.absolutePath, 493)
                        } catch (_: Exception) {}
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    fun extractArchive(archiveFile: File, targetDir: File) {
        if (!targetDir.exists()) targetDir.mkdirs()
        BufferedInputStream(FileInputStream(archiveFile)).use { bis ->
            bis.mark(8)
            val header = ByteArray(8)
            val read = bis.read(header)
            bis.reset()

            val isGzip = read >= 2 && header[0] == 0x1F.toByte() && header[1] == 0x8B.toByte()
            val isZip = read >= 4 && header[0] == 0x50.toByte() && header[1] == 0x4B.toByte()
            val isXz = read >= 6 && header[0] == 0xFD.toByte() && header[1] == 0x37.toByte() && header[2] == 0x7A.toByte() && header[3] == 0x58.toByte() && header[4] == 0x5A.toByte() && header[5] == 0x00.toByte()

            if (isZip) {
                unzip(bis, targetDir)
            } else if (isXz) {
                XZInputStream(bis).use { xzis ->
                    extractTarStream(xzis, targetDir)
                }
            } else if (isGzip) {
                GZIPInputStream(bis).use { gzis ->
                    extractTarStream(gzis, targetDir)
                }
            } else {
                extractTarStream(bis, targetDir)
            }
        }

        try {
            Runtime.getRuntime().exec(arrayOf("chmod", "-R", "755", targetDir.absolutePath)).waitFor()
        } catch (_: Exception) {}

        processSymlinksFile(targetDir)
    }

    fun extractDeb(debFile: File, targetDir: File) {
        try {
            FileInputStream(debFile).use { fis ->
                val magic = ByteArray(8)
                val readMagic = readFully(fis, magic)
                if (readMagic != 8 || String(magic, StandardCharsets.US_ASCII) != "!<arch>\n") {
                    return
                }
                while (true) {
                    val header = ByteArray(60)
                    val readHdr = readFully(fis, header)
                    if (readHdr < 60) break
                    val name = String(header, 0, 16, StandardCharsets.US_ASCII).trim()
                    val sizeStr = String(header, 48, 10, StandardCharsets.US_ASCII).trim()
                    val size = sizeStr.toLongOrNull() ?: 0L
                    if (name.startsWith("data.tar")) {
                        val tempArchive = File(targetDir, "temp_data_archive").apply { parentFile?.mkdirs() }
                        FileOutputStream(tempArchive).use { fos ->
                            var remaining = size
                            val buf = ByteArray(32768)
                            while (remaining > 0) {
                                val toRead = remaining.coerceAtMost(buf.size.toLong()).toInt()
                                val r = fis.read(buf, 0, toRead)
                                if (r <= 0) break
                                fos.write(buf, 0, r)
                                remaining -= r
                            }
                        }
                        extractArchive(tempArchive, targetDir)
                        tempArchive.delete()
                        val pad = (size % 2).toInt()
                        if (pad > 0) skipFully(fis, pad.toLong())
                    } else {
                        skipFully(fis, size + (size % 2))
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun processSymlinksFile(targetDir: File) {
        val symlinksFile = File(targetDir, "SYMLINKS.txt")
        if (symlinksFile.exists() && symlinksFile.isFile) {
            try {
                symlinksFile.readLines().forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.contains("←")) {
                        val parts = trimmed.split("←")
                        if (parts.size >= 2) {
                            val linkTarget = parts[0].trim()
                            var linkPath = parts[1].trim()
                            while (linkPath.startsWith("./") || linkPath.startsWith("/")) {
                                linkPath = if (linkPath.startsWith("./")) linkPath.removePrefix("./") else linkPath.removePrefix("/")
                            }
                            val termuxPrefix = "data/data/com.termux/files/usr/"
                            if (linkPath.startsWith(termuxPrefix)) {
                                linkPath = linkPath.removePrefix(termuxPrefix)
                            }
                            if (isSafePath(linkPath) && linkPath.isNotEmpty()) {
                                val linkFile = File(targetDir, linkPath)
                                linkFile.parentFile?.mkdirs()
                                try {
                                    if (linkFile.exists()) linkFile.delete()
                                    Os.symlink(linkTarget, linkFile.absolutePath)
                                } catch (_: Exception) {
                                    val targetCand = File(linkFile.parentFile, linkTarget)
                                    if (targetCand.exists()) {
                                        targetCand.copyTo(linkFile, overwrite = true)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun extractTarStream(inputStream: InputStream, targetDir: File) {
        val buffer = ByteArray(512)
        var nextLongName: String? = null
        val deferredSymlinks = mutableListOf<Pair<File, String>>()

        while (true) {
            val bytesRead = readFully(inputStream, buffer)
            if (bytesRead < 512) break

            var isAllZero = true
            for (b in buffer) {
                if (b.toInt() != 0) {
                    isAllZero = false
                    break
                }
            }
            if (isAllZero) {
                val secondBuffer = ByteArray(512)
                readFully(inputStream, secondBuffer)
                break
            }

            var entryName = nextLongName ?: parseTarString(buffer, 0, 100)
            nextLongName = null

            val prefix = parseTarString(buffer, 345, 155)
            if (prefix.isNotEmpty() && !entryName.startsWith(prefix)) {
                entryName = "$prefix/$entryName"
            }

            val size = parseTarOctal(buffer, 124, 12)
            val typeFlag = buffer[156].toInt().toChar()
            val linkName = parseTarString(buffer, 157, 100)

            if (typeFlag == 'L') {
                val longNameBytes = ByteArray(size.toInt())
                readFully(inputStream, longNameBytes)
                nextLongName = String(longNameBytes, StandardCharsets.UTF_8).trimEnd('\u0000', '\n', '\r')
                val pad = ((512 - (size % 512)) % 512).toInt()
                if (pad > 0) skipFully(inputStream, pad.toLong())
                continue
            }

            if (typeFlag == 'x' || typeFlag == 'g') {
                val paxBytes = ByteArray(size.toInt())
                readFully(inputStream, paxBytes)
                val paxStr = String(paxBytes, StandardCharsets.UTF_8)
                paxStr.lines().forEach { line ->
                    if (line.contains(" path=")) {
                        nextLongName = line.substringAfter(" path=").trim()
                    }
                }
                val pad = ((512 - (size % 512)) % 512).toInt()
                if (pad > 0) skipFully(inputStream, pad.toLong())
                continue
            }

            var cleanName = entryName.replace('\\', '/')
            while (cleanName.startsWith("./") || cleanName.startsWith("/")) {
                cleanName = if (cleanName.startsWith("./")) cleanName.removePrefix("./") else cleanName.removePrefix("/")
            }
            val termuxPrefix = "data/data/com.termux/files/usr/"
            if (cleanName.startsWith(termuxPrefix)) {
                cleanName = cleanName.removePrefix(termuxPrefix)
            }

            if (!isSafePath(cleanName) || cleanName.isEmpty() || cleanName == ".") {
                val pad = ((512 - (size % 512)) % 512).toInt()
                if (size > 0) skipFully(inputStream, size)
                if (pad > 0) skipFully(inputStream, pad.toLong())
                continue
            }

            val targetFile = File(targetDir, cleanName)

            if (typeFlag == '5' || cleanName.endsWith('/')) {
                targetFile.mkdirs()
                try { Os.chmod(targetFile.absolutePath, 493) } catch (_: Exception) {}
            } else if (typeFlag == '2' || typeFlag == '1') {
                targetFile.parentFile?.mkdirs()
                var cleanLink = linkName.replace('\\', '/')
                while (cleanLink.startsWith("./") || cleanLink.startsWith("/")) {
                    cleanLink = if (cleanLink.startsWith("./")) cleanLink.removePrefix("./") else cleanLink.removePrefix("/")
                }
                if (cleanLink.startsWith(termuxPrefix)) {
                    cleanLink = cleanLink.removePrefix(termuxPrefix)
                }
                deferredSymlinks.add(Pair(targetFile, cleanLink))
            } else {
                targetFile.parentFile?.mkdirs()
                FileOutputStream(targetFile).use { fos ->
                    var remaining = size
                    val copyBuf = ByteArray(16384)
                    while (remaining > 0) {
                        val toRead = remaining.coerceAtMost(copyBuf.size.toLong()).toInt()
                        val r = inputStream.read(copyBuf, 0, toRead)
                        if (r <= 0) break
                        fos.write(copyBuf, 0, r)
                        remaining -= r
                    }
                    if (remaining > 0) {
                        skipFully(inputStream, remaining)
                    }
                }
                try {
                    targetFile.setReadable(true, false)
                    targetFile.setWritable(true, false)
                    targetFile.setExecutable(true, false)
                    Os.chmod(targetFile.absolutePath, 493)
                } catch (_: Exception) {}
            }

            val pad = ((512 - (size % 512)) % 512).toInt()
            if (pad > 0) {
                skipFully(inputStream, pad.toLong())
            }
        }

        deferredSymlinks.forEach { (targetFile, linkTarget) ->
            try {
                if (targetFile.exists()) targetFile.delete()
                try {
                    Os.symlink(linkTarget, targetFile.absolutePath)
                } catch (_: Exception) {
                    val candidate = File(targetFile.parentFile, linkTarget)
                    if (candidate.exists()) {
                        candidate.copyTo(targetFile, overwrite = true)
                    }
                }
                try {
                    targetFile.setReadable(true, false)
                    targetFile.setWritable(true, false)
                    targetFile.setExecutable(true, false)
                    Os.chmod(targetFile.absolutePath, 493)
                } catch (_: Exception) {}
            } catch (_: Exception) {}
        }
    }

    private fun parseTarString(buffer: ByteArray, offset: Int, length: Int): String {
        var end = offset
        while (end < offset + length && buffer[end].toInt() != 0) {
            end++
        }
        return String(buffer, offset, end - offset, StandardCharsets.UTF_8).trim()
    }

    private fun parseTarOctal(buffer: ByteArray, offset: Int, length: Int): Long {
        if ((buffer[offset].toInt() and 0x80) != 0) {
            var result = 0L
            for (i in offset + 1 until offset + length) {
                result = (result shl 8) or (buffer[i].toLong() and 0xFF)
            }
            return result
        }

        var result = 0L
        var i = offset
        while (i < offset + length && (buffer[i] == ' '.code.toByte() || buffer[i].toInt() == 0)) {
            i++
        }
        while (i < offset + length && buffer[i] >= '0'.code.toByte() && buffer[i] <= '7'.code.toByte()) {
            result = (result shl 3) + (buffer[i] - '0'.code.toByte())
            i++
        }
        return result
    }

    private fun readFully(inputStream: InputStream, buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val r = inputStream.read(buffer, total, buffer.size - total)
            if (r <= 0) break
            total += r
        }
        return total
    }

    private fun skipFully(inputStream: InputStream, bytes: Long) {
        var remaining = bytes
        val dummy = ByteArray(16384)
        while (remaining > 0) {
            val toRead = remaining.coerceAtMost(dummy.size.toLong()).toInt()
            val r = inputStream.read(dummy, 0, toRead)
            if (r <= 0) break
            remaining -= r
        }
    }
}
