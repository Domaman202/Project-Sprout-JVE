package ru.pht.sprout.module.repo

import io.github.z4kn4fein.semver.constraints.Constraint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Репозиторий модулей.
 */
interface IRepository {
    /**
     * Поиск модулей.
     *
     * @param name Имя искомого модуля.
     * @return Отсортированный список модулей соответствующих имени и версии.
     */
    @Throws(IOException::class)
    fun find(name: String, version: Constraint): List<IDownloadable> = runBlocking {
        withContext(Dispatchers.IO) {
            findAsync(name, version)
        }
    }

    /**
     * Асинхронный поиск модулей.
     *
     * @param name Имя искомого модуля.
     * @return Отсортированный список модулей соответствующих имени и версии.
     */
    @Throws(IOException::class)
    suspend fun findAsync(name: String, version: Constraint): List<IDownloadable>


    /**
     * Поиск всех доступных модулей.
     *
     * @return Не сортированный список всех модулей.
     */
    @Throws(IOException::class)
    fun findAll(): List<IDownloadable> = runBlocking {
        withContext(Dispatchers.IO) {
            findAllAsync()
        }
    }

    /**
     * Асинхронный поиск всех доступных модулей.
     *
     * @return Не сортированный список всех модулей.
     */
    @Throws(IOException::class)
    suspend fun findAllAsync(): List<IDownloadable>
}