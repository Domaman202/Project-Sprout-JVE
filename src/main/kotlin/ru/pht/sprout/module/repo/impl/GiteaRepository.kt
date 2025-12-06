package ru.pht.sprout.module.repo.impl

import io.ktor.client.*
import io.ktor.client.engine.cio.*

class GiteaRepository(client: HttpClient = HttpClient(CIO)) : GitRepository(client, "https://gitea.com/Domaman202/Project-Sprout-Module-List-Gitea/raw/branch/master/verified.json")