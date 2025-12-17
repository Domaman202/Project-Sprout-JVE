package ru.pht.sprout

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import ru.pht.sprout.TestConfig.Common.FMT_TEST
import ru.pht.sprout.TestConfig.Common.OTHER_UTILS_TEST
import ru.pht.sprout.TestConfig.Common.TRANSLATE_TEST
import ru.pht.sprout.TestConfig.Module.HEADER_PARSE_TEST
import ru.pht.sprout.TestConfig.Module.REAL_NET_TEST
import ru.pht.sprout.TestConfig.Module.REPO_TEST
import ru.pht.sprout.TestConfig.Module.ZIP_TEST

object TestConfigInternal {
    @JvmStatic fun fmtTest(): Boolean = FMT_TEST
    @JvmStatic fun translateTest(): Boolean = TRANSLATE_TEST
    @JvmStatic fun otherUtilsTest(): Boolean = OTHER_UTILS_TEST
    @JvmStatic fun zipTest(): Boolean = ZIP_TEST
    @JvmStatic fun realNetTest(): Boolean = REAL_NET_TEST
    @JvmStatic fun headerParseTest(): Boolean = HEADER_PARSE_TEST
    @JvmStatic fun repoTest(): Boolean = REPO_TEST
    @JvmStatic fun realNetRepoTest(): Boolean = repoTest() and realNetTest()
    @JvmStatic fun giteaRepoTest(): Boolean = realNetRepoTest() and checkConnection("https://gitea.com/Domaman202/Project-Sprout-Module-List-Gitea/raw/branch/master/verified.json")
    @JvmStatic fun gitflicRepoTest(): Boolean = realNetRepoTest() and checkConnection("https://gitflic.ru/project/domaman202/project-sprout-module-list-gitflic/blob/raw?file=verified.json/")
    @JvmStatic fun githubRepoTest(): Boolean = realNetRepoTest() and checkConnection("https://github.com/Domaman202/Project-Sprout-Module-List-Github/raw/refs/heads/master/verified.json")

    private fun checkConnection(url: String): Boolean = runBlocking {
        try {
            HttpClient(CIO) {
                install(HttpTimeout) {
                    connectTimeoutMillis = 2_500
                }
            }.get(url).status == HttpStatusCode.OK
        } catch (_: HttpRequestTimeoutException) {
            false
        }
    }
}