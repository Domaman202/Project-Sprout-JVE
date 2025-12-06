package ru.pht.sprout.module.repo

import ru.pht.sprout.module.header.ModuleHeader
import ru.pht.sprout.module.header.parser.ParserException
import java.io.IOException
import java.nio.file.Path

/**
 * Источник конкретной версии конкретного модуля.
 */
interface IDownloadable {
    /**
     * Получение заголовка модуля.
     *
     * @return Заголовок модуля.
     * @throws IOException Ошибка сети.
     * @throws ParserException Ошибка парсинга заголовка.
     */
    @Throws(IOException::class, ParserException::class)
    fun header(): ModuleHeader

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
     * @param dir Директория для загрузки модуля.
     * @throws IOException Ошибка сети.
     */
    @Throws(IOException::class)
    fun download(dir: Path)

    /**
     * Асинхронная загрузка модуля в директорию.
     *
     * @param dir Директория для загрузки модуля.
     * @throws IOException Ошибка сети.
     */
    @Throws(IOException::class)
    suspend fun downloadAsync(dir: Path)
}