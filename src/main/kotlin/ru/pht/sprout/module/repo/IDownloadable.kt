package ru.pht.sprout.module.repo

import ru.pht.sprout.module.Module
import ru.pht.sprout.module.parser.ParserException
import java.io.File
import java.io.IOException

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
    fun header(): Module

    /**
     * Асинхронное получение заголовка модуля.
     *
     * @return Заголовок модуля.
     * @throws IOException Ошибка сети.
     * @throws ParserException Ошибка парсинга заголовка.
     */
    @Throws(IOException::class, ParserException::class)
    suspend fun headerAsync(): Module

    /**
     * Загрузка модуля в директорию.
     *
     * @param dir Директория для загрузки модуля.
     * @throws IOException Ошибка сети.
     */
    @Throws(IOException::class)
    fun download(dir: File)

    /**
     * Асинхронная загрузка модуля в директорию.
     *
     * @param dir Директория для загрузки модуля.
     * @throws IOException Ошибка сети.
     */
    @Throws(IOException::class)
    suspend fun downloadAsync(dir: File)
}