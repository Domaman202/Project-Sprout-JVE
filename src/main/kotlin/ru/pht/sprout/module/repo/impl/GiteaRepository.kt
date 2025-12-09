package ru.pht.sprout.module.repo.impl

import io.ktor.client.*
import ru.pht.sprout.utils.HttpUtils

/**
 * Репозиторий для работы с репозиториями из `gitea.com`.
 */
class GiteaRepository(
    client: HttpClient = HttpUtils.clientWithoutLogging()
) : GitRepository(
    client,
    "https://gitea.com/Domaman202/Project-Sprout-Module-List-Gitea/raw/branch/master/verified.json"
)