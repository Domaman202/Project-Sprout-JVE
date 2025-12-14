package ru.pht.sprout.module.repo.impl

import io.ktor.client.*
import ru.pht.sprout.module.utils.HttpUtils

/**
 * Репозиторий для работы с репозиториями из `gitflic.ru`.
 */
class GitflicRepository(
    client: HttpClient = HttpUtils.clientWithoutLogging()
) : GitRepository(
    client,
    "https://gitflic.ru/project/domaman202/project-sprout-module-list-gitflic/blob/raw?file=verified.json"
)