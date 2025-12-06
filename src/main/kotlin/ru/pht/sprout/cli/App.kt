package ru.pht.sprout.cli

import io.github.z4kn4fein.semver.constraints.toConstraint
import kotlinx.coroutines.runBlocking
import ru.pht.sprout.module.repo.impl.GithubRepository
import java.io.File

object App {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // Самая лучшая подруга на свете - Катенька <3
        println(GithubRepository().findAsync("pht/example/example-github-module", "1.0.0".toConstraint()).first().downloadAsync(File("run").toPath()))
    }
}