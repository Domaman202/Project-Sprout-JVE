package ru.pht.sprout.module.utils

import ru.pht.sprout.module.header.ModuleHeader
import ru.pht.sprout.module.header.lexer.Lexer
import ru.pht.sprout.module.header.parser.Parser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.*

object ZipUtils {
    fun zip(dir: Path): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            dir.walk().forEach { file ->
                zip.putNextEntry(ZipEntry(file.relativeTo(dir).pathString))
                zip.write(file.readBytes())
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    fun unzip(dir: Path, zip: ByteArray) {
        ZipInputStream(ByteArrayInputStream(zip)).use {
            while (true) {
                val entry = it.nextEntry ?: break
                val target = dir.resolve(entry.name).normalize()
                if (entry.isDirectory) {
                    target.createDirectories()
                } else {
                    target.parent.createDirectories()
                    Files.copy(it, target)
                }
                it.closeEntry()
            }
        }
    }

    fun unzipHeader(zip: ByteArray): ModuleHeader {
        ZipInputStream(ByteArrayInputStream(zip)).use {
            while (true) {
                val entry = it.nextEntry ?: throw IOException("File 'module.pht' not founded")
                if (!entry.isDirectory && entry.name == "module.pht")
                    return Parser(Lexer((it.readBytes().toString(Charsets.UTF_8)))).parse()
                it.closeEntry()
            }
        }
    }
}