package ru.pht.sprout.module.repo.impl

import io.ktor.client.*
import ru.pht.sprout.module.utils.HttpUtils

/**
 * Репозиторий для работы с репозиториями из `github.com`.
 */
class GithubRepository(
    client: HttpClient = HttpUtils.clientWithoutLogging()
) : GitRepository(
    client,
    "https://github.com/Domaman202/Project-Sprout-Module-List-Github/raw/refs/heads/master/verified.json"
)