package ru.pht.sprout.module.utils

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.constraints.Constraint
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.module.repo.cache.ICachingRepository

object RepoUtils {
    suspend inline fun findAndVerifySortedAsync(
        repositories: List<IRepository>,
        name: String,
        version: Constraint,
        combineAndAdd: (combine: List<IDownloadable>, addTo: (IDownloadable) -> Unit) -> Unit
    ): List<IDownloadable> {
        // Мап [Версия; Мап [Хеш; Список Источников]]
        val find: MutableMap<Version, MutableMap<String, MutableList<IDownloadable>>> = HashMap()
        // Перебор всех репозиториев
        repositories.forEach { repo ->
            // Пропуск кеширующих репозиториев
            if (repo is ICachingRepository)
                return@forEach
            // Перебор всех источников
            repo.findAllAsync().forEach { downloadable ->
                // Проверка имени и версии
                if (downloadable.name == name && version.isSatisfiedBy(downloadable.version)) {
                    // Добавляем в найденное
                    find
                        .getOrPut(downloadable.version) { HashMap() }
                        .getOrPut(downloadable.hash) { ArrayList() }
                        .add(downloadable)
                }
            }
        }
        // Список верифицированных источников
        val verified: MutableList<IDownloadable> = ArrayList()
        // Перебор всех найденных источников
        find.values.forEach { hashes ->
            // Выбираем хеш у которого больше всего ссылок
            combineAndAdd(hashes.maxBy { it.value.size }.value, verified::add)
        }
        // Сортировка и возврат результата
        verified.sortBy(IDownloadable::version)
        return verified
    }

    suspend inline fun findAllAndVerifyAsync(
        repositories: List<IRepository>,
        combineAndAddToVerified: (List<IDownloadable>) -> Unit
    ) {
        // Мап [Имя; Мап [Версия; Мап [Хеш; Список Источников]]]
        val find: MutableMap<String, MutableMap<Version, MutableMap<String, MutableList<IDownloadable>>>> = HashMap()
        // Перебор всех репозиториев
        repositories.forEach { repo ->
            // Пропуск кеширующих репозиториев
            if (repo is ICachingRepository)
                return@forEach
            // Перебор всех источников
            repo.findAllAsync().forEach { downloadable ->
                // Добавляем в найденное
                find
                    .getOrPut(downloadable.name) { HashMap() }
                    .getOrPut(downloadable.version) { HashMap() }
                    .getOrPut(downloadable.hash) { ArrayList() }
                    .add(downloadable)
            }
        }
        // Перебор всех найденных модулей
        find.values.forEach { modules ->
            // Перебор всех найденных источников
            modules.values.forEach { hashes ->
                // Выбираем хеш у которого больше всего ссылок
                combineAndAddToVerified(hashes.maxBy { it.value.size }.value)
            }
        }
    }
}