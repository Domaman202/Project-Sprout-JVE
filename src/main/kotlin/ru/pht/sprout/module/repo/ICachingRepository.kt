package ru.pht.sprout.module.repo

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Кеширующий репозиторий модулей.
 */
interface ICachingRepository : IRepository {
    /**
     * Не сортирующий поиск всех доступных закешированных модулей.
     *
     * @return Список всех модулей.
     */
    @Throws(IOException::class)
    fun findAllCached(): List<IDownloadable> = runBlocking {
        withContext(Dispatchers.IO) {
            findAllCachedAsync()
        }
    }

    /**
     * Асинхронный не сортирующий поиск всех доступных закешированных модулей.
     *
     * @return Список всех модулей.
     */
    @Throws(IOException::class)
    suspend fun findAllCachedAsync(): List<IDownloadable>
}