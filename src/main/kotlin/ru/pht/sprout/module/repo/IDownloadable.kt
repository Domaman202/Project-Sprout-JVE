package ru.pht.sprout.module.repo

import io.github.z4kn4fein.semver.Version
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ru.pht.sprout.module.header.ModuleHeader
import ru.pht.sprout.module.header.parser.ParserException
import java.io.IOException
import java.nio.file.Path

/**
 * Источник конкретной версии конкретного модуля.
 */
interface IDownloadable {
    /**
     * Имя модуля.
     */
    val name: String

    /**
     * Версия модуля.
     */
    val version: Version

    /**
     * Хеш модуля.
     */
    val hash: String

    /**
     * Получение заголовка модуля.
     *
     * @return Заголовок модуля.
     * @throws IOException Ошибка сети.
     * @throws ParserException Ошибка парсинга заголовка.
     */
    @Throws(IOException::class, ParserException::class)
    fun header(): ModuleHeader = runBlocking {
        withContext(Dispatchers.IO) {
            headerAsync()
        }
    }

    /**
     * Асинхронное получение заголовка модуля.
     *
     * @return Заголовок модуля.
     * @throws IOException Ошибка сети.
     * @throws ParserException Ошибка парсинга заголовка.
     */
    @Throws(IOException::class, ParserException::class)
    suspend fun headerAsync(): ModuleHeader

    /**
     * Загрузка модуля в директорию.
     *
     * @param dir Директория.
     * @throws IOException Ошибка сети.
     */
    @Throws(IOException::class)
    fun download(dir: Path) = runBlocking {
        withContext(Dispatchers.IO) {
            downloadAsync(dir)
        }
    }

    /**
     * Асинхронная загрузка модуля в директорию.
     *
     * @param dir Директория.
     * @throws IOException Ошибка сети.
     */
    @Throws(IOException::class)
    suspend fun downloadAsync(dir: Path)

    /**
     * Загрузка архива модуля в директорию.
     *
     * @param file Файл архива.
     * @throws IOException Ошибка сети.
     */
    @Throws(IOException::class)
    fun downloadZip(file: Path) = runBlocking {
        withContext(Dispatchers.IO) {
            downloadZipAsync(file)
        }
    }

    /**
     * Асинхронная загрузка архива модуля в директорию.
     *
     * @param file Файл архива.
     * @throws IOException Ошибка сети.
     */
    @Throws(IOException::class)
    suspend fun downloadZipAsync(file: Path)
}