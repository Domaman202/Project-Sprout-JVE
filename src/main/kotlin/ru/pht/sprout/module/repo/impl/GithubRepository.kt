package ru.pht.sprout.module.repo.impl

import io.ktor.client.*
import io.ktor.client.engine.cio.*

class GithubRepository(client: HttpClient = HttpClient(CIO)) : GitRepository(client, "https://github.com/Domaman202/Project-Sprout-Module-List-Github/raw/refs/heads/master/verified.json")