package ru.pht.sprout.module.repo.impl

import io.github.z4kn4fein.semver.constraints.toConstraint
import io.github.z4kn4fein.semver.toVersion
import kotlinx.io.IOException
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.EnabledIf
import ru.pht.sprout.module.utils.ZipUtils
import ru.pht.sprout.module.utils.useTmpDir
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.test.*

@EnabledIf("ru.pht.sprout.TestConfigInternal#gitflicRepoTest", disabledReason = "Тест выключен конфигурацией")
class GitflicRepositoryTest {
    @Test
    @DisplayName("Поиск конкретного модуля")
    fun findTest() {
        val find = REPO.find("pht/example/example-gitflic-module", "1.0.0".toConstraint())
        assertEquals(find.size, 1)
        val download = find.first()
        val header = download.header()
        assertEquals(header.name, "pht/example/example-gitflic-module")
        assertEquals(header.version, "1.0.0".toVersion())
        useTmpDir("ProjectSprout.GitflicRepositoryTest.findTest") { tmp ->
            val zip = tmp.resolve("module.zip")
            download.downloadZip(zip)
            assertTrue(zip.exists())
            assertEquals(ZipUtils.calcSHA512(zip.readBytes()), download.hash)
            val tmpUnzip = tmp.resolve("unzip").createDirectory()
            download.download(tmpUnzip)
            val unzip = tmpUnzip.resolve("pht/example/example-gitflic-module")
            assertTrue(unzip.resolve("module.pht").exists())
            assertTrue(unzip.resolve("src/example.pht").exists())
            assertTrue(unzip.resolve("plg/example.pht").exists())
        }
    }

    @Test
    @DisplayName("Поиск всех доступных модулей")
    fun findAllTest() {
        val all = REPO.findAll()
        assertTrue(all.isNotEmpty())
        val find = REPO.find("pht/example/example-gitflic-module", "1.0.0".toConstraint())
        assertEquals(find.size, 1)
        assertContains(all, find.first())
    }

    @Test
    @DisplayName("Верификация во время загрузки")
    fun verifyTest() {
        val find = REPO.find("pht/example/crack-example-gitflic-module", "1.0.1".toConstraint())
        assertEquals(find.size, 1)
        val download = find.first()
        useTmpDir("ProjectSprout.GitflicRepositoryTest.verifyTest") { tmp ->
            assertThrows<IOException> { download.header() }
            assertThrows<IOException> { download.download(tmp.resolve("module.zip")) }
            assertThrows<IOException> { download.downloadZip(tmp.resolve("unzip")) }
        }
    }


    @Test
    @DisplayName("equals & hash")
    fun equalsAndHashTest() {
        val normal0 = REPO.find("pht/example/example-gitflic-module", "1.0.0".toConstraint())
        val normal1 = REPO.find("pht/example/example-gitflic-module", "1.0.0".toConstraint())
        val cracked = REPO.find("pht/example/crack-example-gitflic-module", "1.0.1".toConstraint())
        assertEquals(normal0, normal1)
        assertEquals(normal0.hashCode(), normal1.hashCode())
        assertNotEquals(normal0, cracked)
        assertNotEquals(normal0.hashCode(), cracked.hashCode())
    }

    companion object {
        lateinit var REPO: GitflicRepository

        @JvmStatic
        @BeforeAll
        fun init() {
            REPO = GitflicRepository()
        }
    }
}