package ru.pht.sprout.module.utils

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.constraints.Constraint
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.module.repo.cache.ICachingRepository

object RepoUtils {
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
    ): List<IDownloadable> = findAndVerifySorted0(repositories, { findAll() }, name, version, combineAndAdd)

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
    ): List<IDownloadable> = findAndVerifySorted0(repositories, { findAllAsync() }, name, version, combineAndAdd)

    inline fun findAndVerifySorted0(
        repositories: List<IRepository>,
        findAll: IRepository.() -> List<IDownloadable>,
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
            findAll(repo).forEach { downloadable ->
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
    ) = findAllAndVerify0(repositories, { findAll() }, combineAndAddToVerified)

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
    ) = findAllAndVerify0(repositories, { findAllAsync() }, combineAndAddToVerified)

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