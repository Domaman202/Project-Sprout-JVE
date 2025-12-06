package ru.pht.sprout.module.repo

import io.github.z4kn4fein.semver.constraints.Constraint
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
    fun find(name: String, version: Constraint): List<IDownloadable>

    /**
     * Асинхронный поиск модулей.
     *
     * @param name Имя искомого модуля.
     * @return Список модулей соответствующих имени.
     */
    @Throws(IOException::class)
    suspend fun findAsync(name: String, version: Constraint): List<IDownloadable>
}