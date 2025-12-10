package ru.pht.sprout.module.repo.cache.impl

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.constraints.Constraint
import ru.pht.sprout.cli.build.BuildInfo
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.module.repo.cache.ICachingRepository
import java.util.*

class NoCacheRepository(private val repositories: List<IRepository>) : ICachingRepository {
    constructor(buildSystem: BuildInfo) : this(buildSystem.repositories)

    override suspend fun findAsync(name: String, version: Constraint): List<IDownloadable> {
        // Ищем ссылки на скачивание
        val find: MutableMap<Version, MutableList<IDownloadable>> = HashMap()
        this.repositories.forEach { repository ->
            if (repository is ICachingRepository)
                return@forEach
            repository.findAllAsync()
                .stream()
                .filter { it.name == name && version.isSatisfiedBy(it.version) }
                .forEach {
                    val list = find[it.version]
                    if (list == null)
                        find[it.version] = arrayListOf(it)
                    else list += it
                }
        }
        // Верифицируем и сортируем
        val verified: SortedSet<IDownloadable> = TreeSet(Comparator.comparing(IDownloadable::version))
        find.values.forEach { links ->
            // Собираем хеши
            val hashes: MutableMap<String, MutableList<IDownloadable>> = HashMap()
            links.forEach {
                val list = hashes[it.hash]
                if (list == null)
                    hashes[it.hash] = arrayListOf(it)
                else list += it
            }
            // Выбираем ссылки где больше хешей совпадают
            val values = hashes.values.iterator()
            var many: List<IDownloadable> = values.next()
            for (other in values) {
                if (many.size < other.size) {
                    many = other
                }
            }
            // Добавляем к списку верифицированных
            verified += many
        }
        // Возвращаем
        return verified.toList()
    }

    override suspend fun findAllAsync(): List<IDownloadable> {
        // Ищем ссылки на скачивание и распределяем
        val find: MutableMap<String, MutableMap<Version, MutableMap<String, MutableList<IDownloadable>>>> = HashMap()
        this.repositories.forEach { repository ->
            if (repository is ICachingRepository)
                return@forEach
            repository.findAllAsync().forEach { download ->
                var versions = find[download.name]
                if (versions == null) {
                    versions = HashMap()
                    find[download.name] = versions
                }
                var hashes = versions[download.version]
                if (hashes == null) {
                    hashes = HashMap()
                    versions[download.version] = hashes
                }
                var downloads = hashes[download.hash]
                if (downloads == null) {
                    downloads = ArrayList()
                    hashes[download.hash] = downloads
                }
                downloads += download
            }
        }
        // Верифицируем
        val verified: MutableSet<IDownloadable> = HashSet()
        find.values.forEach { modules ->
            modules.values.forEach { versions ->
                // Выбираем ссылки где больше хешей совпадают
                val values = versions.values.iterator()
                var many: List<IDownloadable> = values.next()
                for (other in values) {
                    if (many.size < other.size) {
                        many = other
                    }
                }
                // Преобразуем, добавляем к списку верифицированных и кешу
                verified += many
            }
        }
        // Возвращаем
        return verified.toList()
    }

    override fun findAllCached(): List<IDownloadable> =
        emptyList()

    override suspend fun findAllCachedAsync(): List<IDownloadable> =
        emptyList()
}