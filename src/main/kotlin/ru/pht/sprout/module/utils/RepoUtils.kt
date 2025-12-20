package ru.pht.sprout.module.utils

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.constraints.Constraint
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.module.repo.cache.ICachingRepository

/**
 * Утилиты для работы с репозиториями.
 */
object RepoUtils {
    /**
     * Поиск отсутствующих и добавления версий модулей в репозиториях.
     *
     * @param repositories Репозитории.
     * @param available Доступные модули.
     * @param name Имя модуля.
     * @param version Версия модуля.
     * @param combineAndAdd Сборка всех найденных модулей и добавление в список.
     */
    inline fun findUnavailableAndVerifySorted(
        repositories: List<IRepository>,
        available: MutableList<IDownloadable>,
        name: String,
        version: Constraint,
        combineAndAdd: (combine: List<IDownloadable>, addTo: (IDownloadable) -> Unit) -> Unit
    ) {
        findFilterAndVerify0(
            repositories,
            { findAll() },
            { it.name == name && available.none { cache -> cache.version == it.version } && version.isSatisfiedBy(it.version) },
            { combineAndAdd(it, available::add) }
        )
        available.sortBy { it.version }
    }

    /**
     * Асинхронный поиск отсутствующих версий модулей в репозиториях.
     *
     * @param repositories Репозитории.
     * @param available Доступные модули.
     * @param name Имя модуля.
     * @param version Версия модуля.
     * @param combineAndAdd Сборка всех найденных модулей и добавление в список.
     */
    suspend inline fun findUnavailableAndVerifySortedAsync(
        repositories: List<IRepository>,
        available: MutableList<IDownloadable>,
        name: String,
        version: Constraint,
        combineAndAdd: (combine: List<IDownloadable>, addTo: (IDownloadable) -> Unit) -> Unit
    ) {
        findFilterAndVerify0(
            repositories,
            { findAllAsync() },
            { it.name == name && available.none { cache -> cache.version == it.version } && version.isSatisfiedBy(it.version) },
            { combineAndAdd(it, available::add) }
        )
        available.sortBy { it.version }
    }

    /**
     * Поиск модулей в репозиториях.
     *
     * @param repositories Репозитории.
     * @param name Имя модуля.
     * @param version Версия модуля.
     * @param combineAndAdd Сборка всех найденных модулей и добавление в список.
     * @return Отсортированный список модулей соответствующих имени и версии.
     */
    inline fun findAndVerifySorted(
        repositories: List<IRepository>,
        name: String,
        version: Constraint,
        combineAndAdd: (combine: List<IDownloadable>, addTo: (IDownloadable) -> Unit) -> Unit
    ): List<IDownloadable> {
        val verified = ArrayList<IDownloadable>()
        findFilterAndVerify0(
            repositories,
            { findAll() },
            { it.name == name && version.isSatisfiedBy(it.version) },
            { combineAndAdd(it, verified::add) }
        )
        verified.sortBy { it.version }
        return verified
    }

    /**
     * Асинхронный поиск модулей в репозиториях.
     *
     * @param repositories Репозитории.
     * @param name Имя модуля.
     * @param version Версия модуля.
     * @param combineAndAdd Сборка всех найденных модулей и добавление в список.
     * @return Отсортированный список модулей соответствующих имени и версии.
     */
    suspend inline fun findAndVerifySortedAsync(
        repositories: List<IRepository>,
        name: String,
        version: Constraint,
        combineAndAdd: (combine: List<IDownloadable>, addTo: (IDownloadable) -> Unit) -> Unit
    ): List<IDownloadable> {
        val verified = ArrayList<IDownloadable>()
        findFilterAndVerify0(
            repositories,
            { findAllAsync() },
            { it.name == name && version.isSatisfiedBy(it.version) },
            { combineAndAdd(it, verified::add) }
        )
        verified.sortBy { it.version }
        return verified
    }

    inline fun findFilterAndVerify0(
        repositories: List<IRepository>,
        findAll: IRepository.() -> List<IDownloadable>,
        findFilter: (downloadable: IDownloadable) -> Boolean,
        combineAndAdd: (combine: List<IDownloadable>) -> Unit
    ) {
        // Мап [Версия; Мап [Хеш; Список Источников]]
        val find: MutableMap<Version, MutableMap<String, MutableList<IDownloadable>>> = HashMap()
        // Перебор всех репозиториев
        repositories.forEach { repo ->
            // Пропуск кеширующих репозиториев
            if (repo is ICachingRepository)
                return@forEach
            // Перебор всех источников
            findAll(repo).forEach { downloadable ->
                // Проверка модуля
                if (findFilter(downloadable)) {
                    // Добавляем в найденное
                    find
                        .getOrPut(downloadable.version) { HashMap() }
                        .getOrPut(downloadable.hash) { ArrayList() }
                        .add(downloadable)
                }
            }
        }
        // Перебор всех найденных источников
        find.values.forEach { hashes ->
            // Выбираем хеш у которого больше всего ссылок
            combineAndAdd(hashes.maxBy { it.value.size }.value)
        }
    }

    /**
     * Поиск всех доступных модулей в репозиториях.
     *
     * @param repositories Репозитории.
     * @param combineAndAddToVerified Сборка найденных модулей одного имени и версии с последующим добавлением к списку верифицированных.
     * @return Не сортированный список всех модулей.
     */
    inline fun findAllAndVerify(
        repositories: List<IRepository>,
        combineAndAddToVerified: (List<IDownloadable>) -> Unit
    ): Unit = findAllAndVerify0(repositories, { findAll() }, combineAndAddToVerified)

    /**
     * Асинхронный поиск всех доступных модулей в репозиториях.
     *
     * @param repositories Репозитории.
     * @param combineAndAddToVerified Сборка найденных модулей одного имени и версии с последующим добавлением к списку верифицированных.
     * @return Не сортированный список всех модулей.
     */
    suspend inline fun findAllAndVerifyAsync(
        repositories: List<IRepository>,
        combineAndAddToVerified: (List<IDownloadable>) -> Unit
    ): Unit = findAllAndVerify0(repositories, { findAllAsync() }, combineAndAddToVerified)

    inline fun findAllAndVerify0(
        repositories: List<IRepository>,
        findAll: IRepository.() -> List<IDownloadable>,
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
            findAll(repo).forEach { downloadable ->
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