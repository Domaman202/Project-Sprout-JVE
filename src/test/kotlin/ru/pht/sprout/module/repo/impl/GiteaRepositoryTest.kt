package ru.pht.sprout.module.repo.impl

import io.github.z4kn4fein.semver.constraints.Constraint
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GiteaRepositoryTest {

    private lateinit var repository: GiteaRepository
    private lateinit var mockEngine: MockEngine
    private lateinit var client: HttpClient

    @TempDir
    lateinit var tempDir: Path

    private val sampleModulePhtContent = """
        (module "pht/module"
            {[name "pht/example/example-gitea-module"]}
            {[vers "1.0.0"]})
    """.trimIndent()

    private val sampleModulePhtContent2 = """
        (module "pht/module"
            {[name "another/module"]}
            {[vers "2.0.0"]})
    """.trimIndent()

    private val testFileContent = "Test file content"

    @BeforeEach
    fun setUp() {
        mockEngine = MockEngine { request ->
            when (request.url.toString()) {
                "https://gitea.com/Domaman202/Project-Sprout-Module-List-Gitea/raw/branch/master/verified.json" -> {
                    val json = """
                        [
                            {
                                "name": "pht/example/example-gitea-module",
                                "version": "1.0.0",
                                "git": "https://gitea.com/Domaman202/example-module.git",
                                "hash": "${createTestZipHash("pht/example/example-gitea-module", "1.0.0")}",
                                "file": "https://gitea.com/releases/v1.0.0/module.zip"
                            },
                            {
                                "name": "pht/example/example-gitea-module",
                                "version": "1.1.0",
                                "git": "https://gitea.com/Domaman202/example-module.git",
                                "hash": "${createTestZipHash("pht/example/example-gitea-module", "1.1.0")}",
                                "file": "https://gitea.com/releases/v1.1.0/module.zip"
                            },
                            {
                                "name": "another/module",
                                "version": "2.0.0",
                                "git": "https://gitea.com/another/module.git",
                                "hash": "${createTestZipHash("another/module", "2.0.0")}",
                                "file": "https://gitea.com/releases/v2.0.0/module.zip"
                            }
                        ]
                    """.trimIndent()
                    respond(
                        content = json,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }

                "https://gitea.com/releases/v1.0.0/module.zip" -> {
                    val zipBytes = createTestZip("pht/example/example-gitea-module", "1.0.0")
                    respond(
                        content = ByteReadChannel(zipBytes),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/zip")
                    )
                }

                "https://gitea.com/releases/v1.1.0/module.zip" -> {
                    val zipBytes = createTestZip("pht/example/example-gitea-module", "1.1.0")
                    respond(
                        content = ByteReadChannel(zipBytes),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/zip")
                    )
                }

                "https://gitea.com/releases/v2.0.0/module.zip" -> {
                    val zipBytes = createTestZip("another/module", "2.0.0")
                    respond(
                        content = ByteReadChannel(zipBytes),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/zip")
                    )
                }

                "https://gitea.com/releases/invalid-hash/module.zip" -> {
                    val zipBytes = createTestZip("invalid/module", "1.0.0")
                    // Искажаем хэш
                    val corruptedBytes = zipBytes.copyOf().apply { this[0] = (this[0] + 1).toByte() }
                    respond(
                        content = ByteReadChannel(corruptedBytes),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/zip")
                    )
                }

                "https://gitea.com/releases/no-module-pht/module.zip" -> {
                    val zipBytes = createZipWithoutModulePht()
                    respond(
                        content = ByteReadChannel(zipBytes),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/zip")
                    )
                }

                "https://gitea.com/releases/404/module.zip" -> {
                    respond(
                        content = "Not Found",
                        status = HttpStatusCode.NotFound
                    )
                }

                else -> {
                    respond(
                        content = "Not Found",
                        status = HttpStatusCode.NotFound
                    )
                }
            }
        }

        client = HttpClient(mockEngine)
        repository = GiteaRepository(client)
    }

    @Test
    fun `find should return correct modules for name and version constraint`() = runTest {
        val constraint = Constraint.parse("1.0.0")
        val modules = repository.findAsync("pht/example/example-gitea-module", constraint)

        assertEquals(1, modules.size)
        val module = modules[0].headerAsync()
        assertEquals("pht/example/example-gitea-module", module.name)
        assertEquals("1.0.0", module.version.toString())
    }

    @Test
    fun `find should return multiple versions when constraint matches`() = runTest {
        val constraint = Constraint.parse(">=1.0.0 <2.0.0")
        val modules = repository.findAsync("pht/example/example-gitea-module", constraint)

        assertEquals(2, modules.size)
        val versions = modules.map { it.headerAsync().version.toString() }.sorted()
        assertEquals(listOf("1.0.0", "1.1.0"), versions)
    }

    @Test
    fun `find should return empty list when no modules match`() = runTest {
        val constraint = Constraint.parse("3.0.0")
        val modules = repository.findAsync("pht/example/example-gitea-module", constraint)

        assertTrue(modules.isEmpty())
    }

    @Test
    fun `find should return correct module for different name`() = runTest {
        val constraint = Constraint.parse("2.0.0")
        val modules = repository.findAsync("another/module", constraint)

        assertEquals(1, modules.size)
        val module = modules[0].headerAsync()
        assertEquals("another/module", module.name)
        assertEquals("2.0.0", module.version.toString())
    }

    @Test
    fun `download should extract files correctly`() = runTest {
        val constraint = Constraint.parse("1.0.0")
        val modules = repository.findAsync("pht/example/example-gitea-module", constraint)
        assertEquals(1, modules.size)

        val downloadDir = tempDir.resolve("test-download")
        modules[0].downloadAsync(downloadDir)

        val moduleDir = downloadDir.resolve("pht/example/example-gitea-module")
        assertTrue(Files.exists(moduleDir))

        val modulePhtFile = moduleDir.resolve("module.pht")
        assertTrue(Files.exists(modulePhtFile))

        val modulePhtContent = Files.readAllBytes(modulePhtFile).toString(Charsets.UTF_8)
        assertTrue(modulePhtContent.contains("name \"pht/example/example-gitea-module\""))

        val testFile = moduleDir.resolve("test.txt")
        assertTrue(Files.exists(testFile))
        assertEquals(testFileContent, Files.readAllBytes(testFile).toString(Charsets.UTF_8))
    }

    @Test
    fun `download should throw exception when hash mismatch`() = runTest {
        // Создаем отдельный репозиторий для теста с неверным хэшем
        val mockEngineWithInvalidHash = MockEngine { request ->
            when (request.url.toString()) {
                "https://gitea.com/Domaman202/Project-Sprout-Module-List-Gitea/raw/branch/master/verified.json" -> {
                    val json = """
                        [{
                            "name": "invalid/module",
                            "version": "1.0.0",
                            "git": "https://gitea.com/invalid/module.git",
                            "hash": "incorrecthash",
                            "file": "https://gitea.com/releases/invalid-hash/module.zip"
                        }]
                    """.trimIndent()
                    respond(
                        content = json,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }

                "https://gitea.com/releases/invalid-hash/module.zip" -> {
                    val zipBytes = createTestZip("invalid/module", "1.0.0")
                    respond(
                        content = ByteReadChannel(zipBytes),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/zip")
                    )
                }

                else -> {
                    respond(
                        content = "Not Found",
                        status = HttpStatusCode.NotFound
                    )
                }
            }
        }

        val invalidRepo = GiteaRepository(HttpClient(mockEngineWithInvalidHash))
        val constraint = Constraint.parse("1.0.0")
        val modules = invalidRepo.findAsync("invalid/module", constraint)
        assertEquals(1, modules.size)

        val downloadDir = tempDir.resolve("invalid-download")
        assertThrows<Exception> {
            runTest {
                modules[0].downloadAsync(downloadDir)
            }
        }
    }

    @Test
    fun `header should throw exception when module dot pht not found`() = runTest {
        // Создаем отдельный репозиторий для теста без module.pht
        val mockEngineWithoutModulePht = MockEngine { request ->
            when (request.url.toString()) {
                "https://gitea.com/Domaman202/Project-Sprout-Module-List-Gitea/raw/branch/master/verified.json" -> {
                    val json = """
                        [{
                            "name": "no-module/module",
                            "version": "1.0.0",
                            "git": "https://gitea.com/no-module/module.git",
                            "hash": "${createTestZipHash("no-module/module", "1.0.0")}",
                            "file": "https://gitea.com/releases/no-module-pht/module.zip"
                        }]
                    """.trimIndent()
                    respond(
                        content = json,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }

                "https://gitea.com/releases/no-module-pht/module.zip" -> {
                    val zipBytes = createZipWithoutModulePht()
                    respond(
                        content = ByteReadChannel(zipBytes),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/zip")
                    )
                }

                else -> {
                    respond(
                        content = "Not Found",
                        status = HttpStatusCode.NotFound
                    )
                }
            }
        }

        val repoWithoutModulePht = GiteaRepository(HttpClient(mockEngineWithoutModulePht))
        val constraint = Constraint.parse("1.0.0")
        val modules = repoWithoutModulePht.findAsync("no-module/module", constraint)
        assertEquals(1, modules.size)

        assertThrows<Exception> {
            runTest {
                modules[0].headerAsync()
            }
        }
    }

    @Test
    fun `synchronous methods should work correctly`() {
        val constraint = Constraint.parse("1.0.0")
        val modules = repository.find("pht/example/example-gitea-module", constraint)

        assertEquals(1, modules.size)
        val module = modules[0].header()
        assertEquals("pht/example/example-gitea-module", module.name)
        assertEquals("1.0.0", module.version.toString())

        val downloadDir = tempDir.resolve("sync-download")
        modules[0].download(downloadDir)

        val moduleDir = downloadDir.resolve("pht/example/example-gitea-module")
        assertTrue(Files.exists(moduleDir))
    }

    @Test
    fun `should handle network errors gracefully`() = runTest {
        val failingEngine = MockEngine { _ ->
            respond(
                content = "Internal Server Error",
                status = HttpStatusCode.InternalServerError
            )
        }
        val failingRepo = GiteaRepository(HttpClient(failingEngine))

        val constraint = Constraint.parse("1.0.0")
        assertThrows<Exception> {
            failingRepo.findAsync("pht/example/example-gitea-module", constraint)
        }
    }

    @Test
    fun `should handle empty JSON response`() = runTest {
        val emptyJsonEngine = MockEngine { request ->
            when (request.url.toString()) {
                "https://gitea.com/Domaman202/Project-Sprout-Module-List-Gitea/raw/branch/master/verified.json" -> {
                    respond(
                        content = "[]",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> {
                    respond(
                        content = "Not Found",
                        status = HttpStatusCode.NotFound
                    )
                }
            }
        }

        val emptyRepo = GiteaRepository(HttpClient(emptyJsonEngine))
        val constraint = Constraint.parse("1.0.0")
        val modules = emptyRepo.findAsync("pht/example/example-gitea-module", constraint)

        assertTrue(modules.isEmpty())
    }

    @Test
    fun `should handle malformed JSON response`() = runTest {
        val malformedJsonEngine = MockEngine { request ->
            when (request.url.toString()) {
                "https://gitea.com/Domaman202/Project-Sprout-Module-List-Gitea/raw/branch/master/verified.json" -> {
                    respond(
                        content = "{ malformed json }",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> {
                    respond(
                        content = "Not Found",
                        status = HttpStatusCode.NotFound
                    )
                }
            }
        }

        val malformedRepo = GiteaRepository(HttpClient(malformedJsonEngine))
        val constraint = Constraint.parse("1.0.0")

        assertThrows<Exception> {
            malformedRepo.findAsync("pht/example/example-gitea-module", constraint)
        }
    }

    @Test
    fun `should handle 404 when downloading module`() = runTest {
        val notFoundEngine = MockEngine { request ->
            when (request.url.toString()) {
                "https://gitea.com/Domaman202/Project-Sprout-Module-List-Gitea/raw/branch/master/verified.json" -> {
                    val json = """
                        [{
                            "name": "not-found/module",
                            "version": "1.0.0",
                            "git": "https://gitea.com/not-found/module.git",
                            "hash": "${createTestZipHash("not-found/module", "1.0.0")}",
                            "file": "https://gitea.com/releases/404/module.zip"
                        }]
                    """.trimIndent()
                    respond(
                        content = json,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }

                "https://gitea.com/releases/404/module.zip" -> {
                    respond(
                        content = "Not Found",
                        status = HttpStatusCode.NotFound
                    )
                }

                else -> {
                    respond(
                        content = "Not Found",
                        status = HttpStatusCode.NotFound
                    )
                }
            }
        }

        val notFoundRepo = GiteaRepository(HttpClient(notFoundEngine))
        val constraint = Constraint.parse("1.0.0")
        val modules = notFoundRepo.findAsync("not-found/module", constraint)
        assertEquals(1, modules.size)

        val downloadDir = tempDir.resolve("not-found-download")
        assertThrows<Exception> {
            runTest {
                modules[0].downloadAsync(downloadDir)
            }
        }
    }

    // Вспомогательные методы

    private fun createTestZip(moduleName: String, version: String): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // Добавляем module.pht с правильным форматом
            val moduleContent = when (moduleName) {
                "pht/example/example-gitea-module" -> sampleModulePhtContent
                    .replace("1.0.0", version)
                else -> sampleModulePhtContent2
                    .replace("2.0.0", version)
                    .replace("another/module", moduleName)
            }
            zos.putNextEntry(ZipEntry("module.pht"))
            zos.write(moduleContent.toByteArray())
            zos.closeEntry()

            // Добавляем тестовый файл
            zos.putNextEntry(ZipEntry("test.txt"))
            zos.write(testFileContent.toByteArray())
            zos.closeEntry()

            // Добавляем вложенную директорию с файлом
            zos.putNextEntry(ZipEntry("nested/"))
            zos.putNextEntry(ZipEntry("nested/file.txt"))
            zos.write("nested content".toByteArray())
            zos.closeEntry()
        }
        return baos.toByteArray()
    }

    private fun createTestZipHash(moduleName: String, version: String): String {
        val zipBytes = createTestZip(moduleName, version)
        val digest = MessageDigest.getInstance("SHA-512")
        val hash = digest.digest(zipBytes)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun createZipWithoutModulePht(): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // Добавляем только другие файлы, без module.pht
            zos.putNextEntry(ZipEntry("other.txt"))
            zos.write("other content".toByteArray())
            zos.closeEntry()
        }
        return baos.toByteArray()
    }
}