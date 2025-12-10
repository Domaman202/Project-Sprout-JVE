package ru.pht.sprout.module.repo.cache

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
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