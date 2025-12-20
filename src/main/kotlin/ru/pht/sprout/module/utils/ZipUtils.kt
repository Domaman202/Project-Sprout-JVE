package ru.pht.sprout.module.utils

import org.kotlincrypto.hash.sha2.SHA512
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

/**
 * Утилиты для работы с архивами.
 */
object ZipUtils {
    /**
     * Вычисление хеша SHA-512 для массива байт.
     *
     * @param zip Массив.
     * @return Хеш.
     */
    fun calcSHA512(zip: ByteArray): String =
        SHA512().digest(zip).toHexString(HexFormat.Default)

    /**
     * Архивация директории и её файлов.
     *
     * @param dir Директория.
     * @return Архив.
     */
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

    /**
     * Распаковка архива в директорию.
     *
     * @param dir Директория.
     * @param zip Архив.
     */
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

    /**
     * Распаковка и парсинг заголовка модуля.
     *
     * @param zip Архив.
     * @return Заголовок.
     */
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