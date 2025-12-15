package ru.pht.sprout.module.repo.impl

import io.github.z4kn4fein.semver.constraints.toConstraint
import io.github.z4kn4fein.semver.toVersion
import kotlinx.io.IOException
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.EnabledIf
import ru.pht.sprout.module.repo.impl.GithubRepository
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.io.path.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@EnabledIf("ru.pht.sprout.TestConfigInternal#realNetRepoTest", disabledReason = "Тест выключен конфигурацией")
@OptIn(ExperimentalPathApi::class)
class GithubRepositoryTest {
    @Test
    @DisplayName("Тестирование поиска конкретного модуля")
    fun findTest() {
        val find = REPO.find("pht/example/example-github-module", "1.0.0".toConstraint())
        assertEquals(find.size, 1)
        val download = find.first()
        val header = download.header()
        assertEquals(header.name, "pht/example/example-github-module")
        assertEquals(header.version, "1.0.0".toVersion())
        val tmp = Files.createTempDirectory("ProjectSprout.GithubRepositoryTest.findTest")
        try {
            val zip = tmp.resolve("module.zip")
            download.downloadZip(zip)
            assertTrue(zip.exists())
            assertEquals(MessageDigest.getInstance("SHA-512").digest(zip.readBytes()).toHexString(), download.hash)
            val tmpUnzip = tmp.resolve("unzip").createDirectory()
            download.download(tmpUnzip)
            val unzip = tmpUnzip.resolve("pht/example/example-github-module")
            assertTrue(unzip.resolve("module.pht").exists())
            assertTrue(unzip.resolve("src/example.pht").exists())
            assertTrue(unzip.resolve("plg/example.pht").exists())
        } finally {
            tmp.deleteRecursively()
        }
    }

    @Test
    @DisplayName("Тестирование поиска всех доступных модулей")
    fun findAllTest() {
        val all = REPO.findAll()
        assertTrue(all.isNotEmpty())
        val find = REPO.find("pht/example/example-github-module", "1.0.0".toConstraint())
        assertEquals(find.size, 1)
        assertContains(all, find.first())
    }

    @Test
    @DisplayName("Тестирование верификации во время загрузки")
    fun verifyTest() {
        val find = REPO.find("pht/example/crack-example-github-module", "1.0.1".toConstraint())
        assertEquals(find.size, 1)
        val download = find.first()
        val tmp = Files.createTempDirectory("ProjectSprout.GithubRepositoryTest.verifyTest")
        try {
            assertThrows<IOException> { download.header() }
            assertThrows<IOException> { download.download(tmp.resolve("module.zip")) }
            assertThrows<IOException> { download.downloadZip(tmp.resolve("unzip")) }
        } finally {
            tmp.deleteRecursively()
        }
    }

    companion object {
        lateinit var REPO: GithubRepository

        @JvmStatic
        @BeforeAll
        fun init() {
            REPO = GithubRepository()
        }
    }
}