package ru.pht.sprout.module.utils

import ru.pht.sprout.module.header.ModuleHeader
import ru.pht.sprout.module.header.lexer.Lexer
import ru.pht.sprout.module.header.parser.Parser
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.createDirectories

object ZipUtils {
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

    fun unzipHeader(zip: InputStream): ModuleHeader {
        ZipInputStream(zip).use {
            while (true) {
                val entry = it.nextEntry ?: throw IOException("File 'module.pht' not founded")
                if (!entry.isDirectory && entry.name == "module.pht")
                    return Parser(Lexer((it.readBytes().toString(Charsets.UTF_8)))).parse()
                it.closeEntry()
            }
        }
    }
}