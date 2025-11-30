package ru.pht.sprout.module.repo

import java.io.IOException

/**
 * Репозиторий модулей.
 */
interface IRepository {
    /**
     * Поиск модулей.
     *
     * @param name Имя искомого модуля.
     * @return Список модулей соответствующих имени.
     */
    @Throws(IOException::class)
    fun find(name: String): List<IDownloadable>

    /**
     * Асинхронный поиск модулей.
     *
     * @param name Имя искомого модуля.
     * @return Список модулей соответствующих имени.
     */
    @Throws(IOException::class)
    suspend fun findAsync(name: String): List<IDownloadable>
}