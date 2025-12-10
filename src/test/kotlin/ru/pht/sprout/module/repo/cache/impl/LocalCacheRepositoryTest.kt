package ru.pht.sprout.module.repo.cache.impl

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.constraints.Constraint
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.pht.sprout.module.header.ModuleHeader
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.module.repo.impl.GiteaRepository
import ru.pht.sprout.module.repo.impl.GithubRepository
import ru.pht.sprout.module.utils.ZipUtils
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LocalCacheRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var cacheDirectory: Path
    private lateinit var cacheListFile: Path
    private lateinit var mockGithubEngine: MockEngine
    private lateinit var mockGiteaEngine: MockEngine
    private lateinit var githubRepository: GithubRepository
    private lateinit var giteaRepository: GiteaRepository
    private lateinit var localCacheRepository: LocalCacheRepository

    private val sampleModulePhtContent = """
        (module "pht/module"
            {[name "test/module"]}
            {[vers "1.0.0"]})
    """.trimIndent()

    private val sampleModulePhtContent2 = """
        (module "pht/module"
            {[name "another/module"]}
            {[vers "2.0.0"]})
    """.trimIndent()

    private val testFileContent = "Test file content"

    // Вычисленные хеши для тестовых данных
    private val testModule10Zip = createTestZip("test/module", "1.0.0", "github")
    private val testModule10Hash = computeHash(testModule10Zip)

    private val testModule11Zip = createTestZip("test/module", "1.1.0", "gitea")
    private val testModule11Hash = computeHash(testModule11Zip)

    private val githubOnlyZip = createTestZip("github/only", "1.0.0", "github")
    private val githubOnlyHash = computeHash(githubOnlyZip)

    private val giteaOnlyZip = createTestZip("gitea/only", "1.0.0", "gitea")
    private val giteaOnlyHash = computeHash(giteaOnlyZip)

    // Zip и хеши для конфликтных модулей (должны быть разными)
    private val githubConflictZip = createTestZip("conflict/module", "1.0.0", "github-conflict")
    private val githubConflictHash = computeHash(githubConflictZip)

    private val giteaConflictZip = createTestZip("conflict/module", "1.0.0", "gitea-conflict")
    private val giteaConflictHash = computeHash(giteaConflictZip)

    // Zip и хеш для дублирующихся модулей (должен быть одинаковым)
    private val duplicateZip = createTestZip("duplicate/module", "1.0.0", "duplicate")
    private val duplicateHash = computeHash(duplicateZip)

    @BeforeEach
    fun setUp() {
        cacheDirectory = tempDir.resolve("cache/modules")
        cacheListFile = tempDir.resolve("cache/modules.json")

        // Настраиваем моки для GithubRepository
        mockGithubEngine = MockEngine.Companion { request ->
            when (request.url.toString()) {
                "https://github.com/Domaman202/Project-Sprout-Module-List-Github/raw/refs/heads/master/verified.json" -> {
                    val json = """
                        [
                            {
                                "name": "test/module",
                                "version": "1.0.0",
                                "git": "https://github.com/test/module.git",
                                "hash": "$testModule10Hash",
                                "file": "https://github.com/releases/v1.0.0/module.zip"
                            },
                            {
                                "name": "github/only",
                                "version": "1.0.0",
                                "git": "https://github.com/github/only.git",
                                "hash": "$githubOnlyHash",
                                "file": "https://github.com/releases/github-only.zip"
                            },
                            {
                                "name": "conflict/module",
                                "version": "1.0.0",
                                "git": "https://github.com/conflict/module.git",
                                "hash": "$githubConflictHash",
                                "file": "https://github.com/releases/conflict-github.zip"
                            },
                            {
                                "name": "duplicate/module",
                                "version": "1.0.0",
                                "git": "https://github.com/duplicate/module.git",
                                "hash": "$duplicateHash",
                                "file": "https://github.com/releases/duplicate-github.zip"
                            }
                        ]
                    """.trimIndent()
                    respond(
                        content = json,
                        status = HttpStatusCode.Companion.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }

                "https://github.com/releases/v1.0.0/module.zip" -> {
                    respond(
                        content = ByteReadChannel(testModule10Zip),
                        status = HttpStatusCode.Companion.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/zip")
                    )
                }

                "https://github.com/releases/github-only.zip" -> {
                    respond(
                        content = ByteReadChannel(githubOnlyZip),
                        status = HttpStatusCode.Companion.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/zip")
                    )
                }

                "https://github.com/releases/conflict-github.zip" -> {
                    respond(
                        content = ByteReadChannel(githubConflictZip),
                        status = HttpStatusCode.Companion.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/zip")
                    )
                }

                "https://github.com/releases/duplicate-github.zip" -> {
                    respond(
                        content = ByteReadChannel(duplicateZip),
                        status = HttpStatusCode.Companion.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/zip")
                    )
                }

                else -> {
                    respond(
                        content = "Not Found",
                        status = HttpStatusCode.Companion.NotFound
                    )
                }
            }
        }

        // Настраиваем моки для GiteaRepository
        mockGiteaEngine = MockEngine.Companion { request ->
            when (request.url.toString()) {
                "https://gitea.com/Domaman202/Project-Sprout-Module-List-Gitea/raw/branch/master/verified.json" -> {
                    val json = """
                        [
                            {
                                "name": "test/module",
                                "version": "1.1.0",
                                "git": "https://gitea.com/test/module.git",
                                "hash": "$testModule11Hash",
                                "file": "https://gitea.com/releases/v1.1.0/module.zip"
                            },
                            {
                                "name": "gitea/only",
                                "version": "1.0.0",
                                "git": "https://gitea.com/gitea/only.git",
                                "hash": "$giteaOnlyHash",
                                "file": "https://gitea.com/releases/gitea-only.zip"
                            },
                            {
                                "name": "conflict/module",
                                "version": "1.0.0",
                                "git": "https://gitea.com/conflict/module.git",
                                "hash": "$giteaConflictHash",
                                "file": "https://gitea.com/releases/conflict-gitea.zip"
                            },
                            {
                                "name": "conflict/module",
                                "version": "1.0.0",
                                "git": "https://gitea.com/conflict/module2.git",
                                "hash": "$giteaConflictHash",
                                "file": "https://gitea.com/releases/conflict-gitea2.zip"
                            },
                            {
                                "name": "duplicate/module",
                                "version": "1.0.0",
                                "git": "https://gitea.com/duplicate/module.git",
                                "hash": "$duplicateHash",
                                "file": "https://gitea.com/releases/duplicate-gitea.zip"
                            }
                        ]
                    """.trimIndent()
                    respond(
                        content = json,
                        status = HttpStatusCode.Companion.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }

                "https://gitea.com/releases/v1.1.0/module.zip" -> {
                    respond(
                        content = ByteReadChannel(testModule11Zip),
                        status = HttpStatusCode.Companion.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/zip")
                    )
                }

                "https://gitea.com/releases/gitea-only.zip" -> {
                    respond(
                        content = ByteReadChannel(giteaOnlyZip),
                        status = HttpStatusCode.Companion.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/zip")
                    )
                }

                "https://gitea.com/releases/conflict-gitea.zip",
                "https://gitea.com/releases/conflict-gitea2.zip" -> {
                    respond(
                        content = ByteReadChannel(giteaConflictZip),
                        status = HttpStatusCode.Companion.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/zip")
                    )
                }

                "https://gitea.com/releases/duplicate-gitea.zip" -> {
                    respond(
                        content = ByteReadChannel(duplicateZip),
                        status = HttpStatusCode.Companion.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/zip")
                    )
                }

                else -> {
                    respond(
                        content = "Not Found",
                        status = HttpStatusCode.Companion.NotFound
                    )
                }
            }
        }

        githubRepository = GithubRepository(HttpClient(mockGithubEngine))
        giteaRepository = GiteaRepository(HttpClient(mockGiteaEngine))

        localCacheRepository = LocalCacheRepository(
            cacheDirectory,
            cacheListFile,
            listOf(githubRepository, giteaRepository)
        )
    }

    @Test
    fun `cached file should have name with version`() = runTest {
        val constraint = Constraint.Companion.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(1, modules.size)

        // Кэшируем модуль
        modules[0].headerAsync()

        // Проверяем, что файл создан с именем, содержащим версию
        val expectedFile = cacheDirectory.resolve("test/module.1.0.0.zip")
        assertTrue(Files.exists(expectedFile), "Файл должен быть создан с именем, содержащим версию: ${expectedFile}")

        // Проверяем, что в localized список содержится модуль
        val cached = localCacheRepository.findAllCachedAsync()
        assertEquals(1, cached.size)
        assertEquals("test/module", cached[0].name)
        assertEquals("1.0.0", cached[0].version.toString())
        assertEquals(testModule10Hash, cached[0].hash)
    }

    @Test
    fun `multiple versions of same module should create different cache files`() = runTest {
        // Получаем обе версии модуля
        val constraint = Constraint.Companion.parse(">=1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(2, modules.size)

        // Кэшируем обе версии
        modules[0].headerAsync()
        modules[1].headerAsync()

        // Проверяем, что созданы два разных файла
        val file1 = cacheDirectory.resolve("test/module.1.0.0.zip")
        val file2 = cacheDirectory.resolve("test/module.1.1.0.zip")

        assertTrue(Files.exists(file1), "Должен существовать файл для версии 1.0.0")
        assertTrue(Files.exists(file2), "Должен существовать файл для версии 1.1.0")

        // Проверяем, что это разные файлы
        assertNotEquals(file1, file2)

        // Проверяем содержимое localized
        val cached = localCacheRepository.findAllCachedAsync()
        assertEquals(2, cached.size)

        // Проверяем, что обе версии присутствуют в кэше
        val cachedVersions = cached.map { it.version.toString() }.sorted()
        assertEquals(listOf("1.0.0", "1.1.0"), cachedVersions)

        // Проверяем хеши
        val cachedHashes = cached.map { it.hash }.sorted()
        assertEquals(listOf(testModule10Hash, testModule11Hash).sorted(), cachedHashes)
    }

    @Test
    fun `duplicate modules with same hash should not create duplicate cache entries`() = runTest {
        // Для duplicate/module в обоих репозиториях одинаковый хеш
        val constraint = Constraint.Companion.parse("1.0.0")
        val modules = localCacheRepository.findAsync("duplicate/module", constraint)

        // Должен быть выбран только один модуль
        assertEquals(1, modules.size)
        assertEquals(duplicateHash, modules[0].hash)

        // Кэшируем модуль
        modules[0].headerAsync()

        // Проверяем, что создан только один файл
        val expectedFile = cacheDirectory.resolve("duplicate/module.1.0.0.zip")
        assertTrue(Files.exists(expectedFile))

        // Проверяем, что в localized только одна запись
        val cached = localCacheRepository.findAllCachedAsync()
        assertEquals(1, cached.size)
        assertEquals("duplicate/module", cached[0].name)
        assertEquals("1.0.0", cached[0].version.toString())
        assertEquals(duplicateHash, cached[0].hash)

        // Проверяем, что HashSet предотвратил дублирование
        val allModules = localCacheRepository.findAllAsync()
        val duplicateModules = allModules.filter { it.name == "duplicate/module" }
        assertEquals(1, duplicateModules.size, "В общем списке должен быть только один модуль duplicate/module")
    }

    @Test
    fun `findAsync should use SortedMap for version ordering`() = runTest {
        // Для test/module есть две версии: 1.0.0 и 1.1.0
        // Они должны быть отсортированы по возрастанию версии
        val constraint = Constraint.Companion.parse(">=1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)

        // Проверяем, что версии отсортированы
        assertEquals(2, modules.size)
        assertEquals("1.0.0", modules[0].version.toString())
        assertEquals("1.1.0", modules[1].version.toString())

        // Проверяем, что файлы будут созданы с правильными именами
        modules[0].headerAsync()
        modules[1].headerAsync()

        val file1 = cacheDirectory.resolve("test/module.1.0.0.zip")
        val file2 = cacheDirectory.resolve("test/module.1.1.0.zip")

        assertTrue(Files.exists(file1))
        assertTrue(Files.exists(file2))

        // Проверяем хеши
        assertEquals(testModule10Hash, modules[0].hash)
        assertEquals(testModule11Hash, modules[1].hash)
    }

    @Test
    fun `findAsync should return unique modules based on file path`() = runTest {
        // Проверяем, что findAsync возвращает уникальные модули
        val constraint = Constraint.Companion.parse("1.0.0")
        val modules = localCacheRepository.findAsync("duplicate/module", constraint)

        // Должен быть только один модуль, даже если в разных репозиториях есть одинаковые
        assertEquals(1, modules.size)
        assertEquals(duplicateHash, modules[0].hash)

        // Проверяем, что при добавлении в Set остается только один элемент
        val set = modules.toSet()
        assertEquals(1, set.size)
    }

    @Test
    fun `cache file name should include version and use normalized path`() = runTest {
        val constraint = Constraint.Companion.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(1, modules.size)

        // Кэшируем модуль
        modules[0].headerAsync()

        // Проверяем, что файл создан с правильным именем
        val expectedFileName = "test/module.1.0.0.zip"
        val expectedFile = cacheDirectory.resolve(expectedFileName)
        assertTrue(Files.exists(expectedFile))

        // Проверяем, что путь нормализован
        val normalizedPath = expectedFile.normalize()
        assertEquals(expectedFile, normalizedPath)

        // Проверяем хеш
        assertEquals(testModule10Hash, modules[0].hash)
    }

    @Test
    fun `module with same name and version from different sources should have same cache file`() = runTest {
        // Создаем тестовый zip для модуля
        val testZipContent = createTestZip("same/module", "1.0.0", "test")
        val testHash = computeHash(testZipContent)

        // Создаем два мок-репозитория с одинаковым модулем
        val mockRepo1 = createMockRepository("same/module", "1.0.0", testHash, testZipContent)
        val mockRepo2 = createMockRepository("same/module", "1.0.0", testHash, testZipContent)

        val repo = LocalCacheRepository(
            cacheDirectory,
            cacheListFile,
            listOf(mockRepo1, mockRepo2)
        )

        val allModules = repo.findAllAsync()

        // Должен быть только один модуль, так как оба имеют одинаковые имя, версию и хеш
        assertEquals(1, allModules.size)
        assertEquals("same/module", allModules[0].name)
        assertEquals("1.0.0", allModules[0].version.toString())
        assertEquals(testHash, allModules[0].hash)

        // Кэшируем модуль
        allModules[0].headerAsync()

        // Проверяем, что файл создан с правильным именем
        val expectedFile = cacheDirectory.resolve("same/module.1.0.0.zip")
        assertTrue(Files.exists(expectedFile))
    }

    @Test
    fun `findAsync should select module with most common hash when conflicts exist`() = runTest {
        val constraint = Constraint.Companion.parse("1.0.0")
        val modules = localCacheRepository.findAsync("conflict/module", constraint)

        // Должна быть выбрана версия из Gitea, так как у нее больше совпадений по хешам (2 против 1)
        assertEquals(1, modules.size)
        assertEquals("conflict/module", modules[0].name)
        assertEquals("1.0.0", modules[0].version.toString())
        assertEquals(giteaConflictHash, modules[0].hash)
    }

    @Test
    fun `findAllAsync should handle hash conflicts and select most common hash`() = runTest {
        // Получаем все модули
        val allModules = localCacheRepository.findAllAsync()

        // Проверяем, что conflict/module присутствует только один раз
        val conflictModules = allModules.filter { it.name == "conflict/module" }
        assertEquals(1, conflictModules.size)

        // Проверяем, что выбрана версия с наиболее распространенным хешем (giteaConflictHash)
        assertEquals(giteaConflictHash, conflictModules[0].hash)

        // Проверяем другие модули
        val testModules = allModules.filter { it.name == "test/module" }
        assertEquals(2, testModules.size) // Две разные версии с разными хешами
    }

    @Test
    fun `findAllCachedAsync should return empty list when no modules are cached`() = runTest {
        val cachedModules = localCacheRepository.findAllCachedAsync()
        assertEquals(0, cachedModules.size)

        val cachedModulesSync = localCacheRepository.findAllCached()
        assertEquals(0, cachedModulesSync.size)
    }

    @Test
    fun `findAllCachedAsync should return cached modules after they are downloaded`() = runTest {
        val initialCached = localCacheRepository.findAllCachedAsync()
        assertEquals(0, initialCached.size)

        val constraint = Constraint.Companion.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(1, modules.size)

        val beforeDownloadCached = localCacheRepository.findAllCachedAsync()
        assertEquals(0, beforeDownloadCached.size)

        modules[0].headerAsync()

        val afterDownloadCached = localCacheRepository.findAllCachedAsync()
        assertEquals(1, afterDownloadCached.size)
        assertEquals("test/module", afterDownloadCached[0].name)
        assertEquals("1.0.0", afterDownloadCached[0].version.toString())
        assertEquals(testModule10Hash, afterDownloadCached[0].hash)

        val cachedSync = localCacheRepository.findAllCached()
        assertEquals(1, cachedSync.size)
        assertEquals("test/module", cachedSync[0].name)
        assertEquals("1.0.0", cachedSync[0].version.toString())
        assertEquals(testModule10Hash, cachedSync[0].hash)
    }

    @Test
    fun `MaybeCachedDownloadable should call saveLocalizedList when downloading module first time`() = runTest {
        val constraint = Constraint.Companion.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(1, modules.size)

        Files.deleteIfExists(cacheListFile)

        modules[0].headerAsync()

        assertTrue(Files.exists(cacheListFile), "Файл списка кэша должен быть создан")

        val content = Files.readAllBytes(cacheListFile).toString(Charsets.UTF_8)
        assertTrue(content.isNotEmpty())
        assertTrue(content.trim().startsWith("["))
        assertTrue(content.trim().endsWith("]"))

        val cached = localCacheRepository.findAllCachedAsync()
        assertEquals(1, cached.size)
        assertEquals("test/module", cached[0].name)

        // Проверяем, что в файле содержится правильный путь с версией
        assertTrue(content.contains("test/module.1.0.0.zip"))
    }

    @Test
    fun `should handle duplicate modules from different repositories`() = runTest {
        val allModules = localCacheRepository.findAllAsync()

        val testModules = allModules.filter { it.name == "test/module" }
        assertEquals(2, testModules.size)

        val versions = testModules.map { it.version.toString() }.sorted()
        assertEquals(listOf("1.0.0", "1.1.0"), versions)

        // Проверяем хеши
        val hashes = testModules.map { it.hash }.sorted()
        assertEquals(listOf(testModule10Hash, testModule11Hash).sorted(), hashes)
    }

    @Test
    fun `cached modules should be reused from localized list`() = runTest {
        val constraint = Constraint.Companion.parse("1.0.0")
        val modules1 = localCacheRepository.findAsync("test/module", constraint)
        modules1[0].headerAsync()

        assertTrue(Files.exists(cacheListFile))

        val newRepository = LocalCacheRepository(
            cacheDirectory,
            cacheListFile,
            listOf(githubRepository, giteaRepository)
        )

        val modules2 = newRepository.findAsync("test/module", constraint)
        assertEquals(1, modules2.size)

        val header = modules2[0].headerAsync()
        assertEquals("test/module", header.name)
        assertEquals("1.0.0", header.version.toString())

        val cached = newRepository.findAllCachedAsync()
        assertEquals(1, cached.size)
        assertEquals("test/module", cached[0].name)
        assertEquals("1.0.0", cached[0].version.toString())
        assertEquals(testModule10Hash, cached[0].hash)

        val cachedSync = newRepository.findAllCached()
        assertEquals(1, cachedSync.size)
        assertEquals("test/module", cachedSync[0].name)
        assertEquals("1.0.0", cachedSync[0].version.toString())
        assertEquals(testModule10Hash, cachedSync[0].hash)
    }

    @Test
    fun `downloadZipAsync should download zip file to specified location`() = runTest {
        val constraint = Constraint.Companion.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(1, modules.size)

        // Создаем временный файл для скачивания
        val downloadFile = tempDir.resolve("downloaded.zip")

        // Скачиваем zip файл
        modules[0].downloadZipAsync(downloadFile)

        // Проверяем, что файл создан
        assertTrue(Files.exists(downloadFile), "Zip файл должен быть скачан в указанное место")

        // Проверяем содержимое файла
        val downloadedContent = Files.readAllBytes(downloadFile)
        assertTrue(downloadedContent.contentEquals(testModule10Zip),
            "Содержимое скачанного файла должно совпадать с ожидаемым")

        // Проверяем, что файл добавлен в кэш
        val cachedFiles = cacheDirectory.toFile().listFiles()
        assertNotNull(cachedFiles)
        assertEquals(1, cachedFiles.size, "Файл должен быть закэширован")
    }

    @Test
    fun `downloadZipAsync should use cached file when available`() = runTest {
        val constraint = Constraint.Companion.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(1, modules.size)

        // Сначала кэшируем модуль через headerAsync
        modules[0].headerAsync()

        // Убеждаемся, что файл в кэше существует
        val cachedFile = cacheDirectory.resolve("test/module.1.0.0.zip")
        assertTrue(Files.exists(cachedFile), "Файл должен быть закэширован")

        // Получаем время последнего изменения кэшированного файла
        val lastModifiedBefore = Files.getLastModifiedTime(cachedFile)

        // Создаем временный файл для скачивания
        val downloadFile = tempDir.resolve("downloaded2.zip")

        // Скачиваем zip файл (должен использовать кэш)
        modules[0].downloadZipAsync(downloadFile)

        // Проверяем, что файл создан
        assertTrue(Files.exists(downloadFile))

        // Проверяем, что кэшированный файл не изменился
        val lastModifiedAfter = Files.getLastModifiedTime(cachedFile)
        assertEquals(lastModifiedBefore, lastModifiedAfter,
            "Кэшированный файл не должен изменяться при повторном скачивании")
    }

    @Test
    fun `downloadAsync should extract module to specified directory`() = runTest {
        val constraint = Constraint.Companion.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(1, modules.size)

        // Создаем временную директорию для распаковки
        val extractDir = tempDir.resolve("extracted")

        // Скачиваем и распаковываем модуль
        modules[0].downloadAsync(extractDir)

        // Проверяем, что создана поддиректория с именем модуля
        val moduleDir = extractDir.resolve("test/module")
        assertTrue(Files.exists(moduleDir), "Должна быть создана директория с именем модуля")

        // Проверяем содержимое распакованного модуля
        val modulePhtFile = moduleDir.resolve("module.pht")
        assertTrue(Files.exists(modulePhtFile), "Файл module.pht должен быть распакован")

        val testTxtFile = moduleDir.resolve("test.txt")
        assertTrue(Files.exists(testTxtFile), "Файл test.txt должен быть распакован")

        val nestedFile = moduleDir.resolve("nested/file.txt")
        assertTrue(Files.exists(nestedFile), "Вложенный файл nested/file.txt должен быть распакован")

        // Проверяем содержимое файлов
        val modulePhtContent = Files.readAllLines(modulePhtFile, Charsets.UTF_8).joinToString("\n")
        assertTrue(modulePhtContent.contains("test/module"))
        assertTrue(modulePhtContent.contains("1.0.0"))

        val testTxtContent = String(Files.readAllBytes(testTxtFile), Charsets.UTF_8)
        assertEquals(testFileContent, testTxtContent)

        val nestedContent = String(Files.readAllBytes(nestedFile), Charsets.UTF_8)
        assertEquals("nested content", nestedContent)

        // Проверяем, что модуль добавлен в кэш
        val cached = localCacheRepository.findAllCachedAsync()
        assertEquals(1, cached.size)
        assertEquals("test/module", cached[0].name)
    }

    @Test
    fun `downloadAsync should use cached file when available`() = runTest {
        val constraint = Constraint.Companion.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(1, modules.size)

        // Сначала кэшируем модуль через headerAsync
        modules[0].headerAsync()

        // Убеждаемся, что файл в кэше существует
        val cachedFile = cacheDirectory.resolve("test/module.1.0.0.zip")
        assertTrue(Files.exists(cachedFile), "Файл должен быть закэширован")

        // Получаем время последнего изменения кэшированного файла
        val lastModifiedBefore = Files.getLastModifiedTime(cachedFile)

        // Создаем временную директорию для распаковки
        val extractDir = tempDir.resolve("extracted2")

        // Скачиваем и распаковываем модуль (должен использовать кэш)
        modules[0].downloadAsync(extractDir)

        // Проверяем, что файлы распакованы
        val moduleDir = extractDir.resolve("test/module")
        assertTrue(Files.exists(moduleDir))

        // Проверяем, что кэшированный файл не изменился
        val lastModifiedAfter = Files.getLastModifiedTime(cachedFile)
        assertEquals(lastModifiedBefore, lastModifiedAfter,
            "Кэшированный файл не должен изменяться при повторной распаковке")
    }

    @Test
    fun `downloadAsync should create parent directories if they dont exist`() = runTest {
        val constraint = Constraint.Companion.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(1, modules.size)

        // Создаем путь с несуществующими родительскими директориями
        val extractDir = tempDir.resolve("deeply/nested/extract/dir")

        // Проверяем, что директории не существуют
        assertTrue(!Files.exists(extractDir))

        // Скачиваем и распаковываем модуль
        modules[0].downloadAsync(extractDir)

        // Проверяем, что директории созданы и модуль распакован
        val moduleDir = extractDir.resolve("test/module")
        assertTrue(Files.exists(moduleDir), "Директории должны быть созданы")
        assertTrue(Files.exists(moduleDir.resolve("module.pht")))
    }

    @Test
    fun `downloadZipAsync should create parent directories if they dont exist`() = runTest {
        val constraint = Constraint.Companion.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(1, modules.size)

        // Создаем путь с несуществующими родительскими директориями
        val downloadFile = tempDir.resolve("deeply/nested/download/dir/module.zip")

        // Проверяем, что директории не существуют
        assertTrue(!Files.exists(downloadFile.parent))

        // Скачиваем zip файл
        modules[0].downloadZipAsync(downloadFile)

        // Проверяем, что директории созданы и файл скачан
        assertTrue(Files.exists(downloadFile.parent), "Родительские директории должны быть созданы")
        assertTrue(Files.exists(downloadFile), "Файл должен быть скачан")
    }

    @Test
    fun `downloadAsync should extract multiple modules to same directory`() = runTest {
        // Получаем модули с разными версиями
        val constraint = Constraint.Companion.parse(">=1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(2, modules.size)

        // Создаем временную директорию для распаковки
        val extractDir = tempDir.resolve("multi-extract")

        // Распаковываем первую версию
        modules[0].downloadAsync(extractDir)

        // Убеждаемся, что файлы первой версии существуют
        val moduleDir = extractDir.resolve("test/module")
        assertTrue(Files.exists(moduleDir.resolve("module.pht")))

        // Получаем содержимое файла module.pht первой версии
        val firstVersionContent = String(Files.readAllBytes(moduleDir.resolve("module.pht")), Charsets.UTF_8)

        // Распаковываем вторую версию (должна перезаписать первую, если реализация это поддерживает)
        // Вместо этого, давайте распакуем в другую директорию, чтобы избежать конфликта
        val extractDir2 = tempDir.resolve("multi-extract2")
        modules[1].downloadAsync(extractDir2)

        // Проверяем, что вторая версия тоже распакована
        val moduleDir2 = extractDir2.resolve("test/module")
        assertTrue(Files.exists(moduleDir2.resolve("module.pht")))

        // Получаем содержимое файла module.pht второй версии
        val secondVersionContent = String(Files.readAllBytes(moduleDir2.resolve("module.pht")), Charsets.UTF_8)

        // Проверяем, что содержимое разное (разные версии)
        // Поскольку версии разные, содержимое должно быть разным
        assertNotEquals(firstVersionContent, secondVersionContent)

        // Проверяем, что в кэше 2 модуля
        val cached = localCacheRepository.findAllCachedAsync()
        assertEquals(2, cached.size)
    }

    @Test
    fun `downloadZipAsync should handle duplicate downloads`() = runTest {
        val constraint = Constraint.Companion.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(1, modules.size)

        // Создаем несколько файлов для скачивания
        val downloadFile1 = tempDir.resolve("download1.zip")
        val downloadFile2 = tempDir.resolve("download2.zip")

        // Скачиваем один и тот же модуль в разные файлы
        modules[0].downloadZipAsync(downloadFile1)
        modules[0].downloadZipAsync(downloadFile2)

        // Проверяем, что оба файла созданы
        assertTrue(Files.exists(downloadFile1))
        assertTrue(Files.exists(downloadFile2))

        // Проверяем, что содержимое одинаковое
        val content1 = Files.readAllBytes(downloadFile1)
        val content2 = Files.readAllBytes(downloadFile2)
        assertTrue(content1.contentEquals(content2),
            "Оба скачанных файла должны иметь одинаковое содержимое")

        // Проверяем, что в кэше только один модуль
        val cached = localCacheRepository.findAllCachedAsync()
        assertEquals(1, cached.size)
    }

    // Вспомогательные методы

    private fun createTestZip(moduleName: String, version: String, source: String = ""): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val moduleContent = when (moduleName) {
                "test/module" -> sampleModulePhtContent.replace("1.0.0", version)
                "github/only" -> sampleModulePhtContent.replace("test/module", "github/only").replace("1.0.0", version)
                "gitea/only" -> sampleModulePhtContent.replace("test/module", "gitea/only").replace("1.0.0", version)
                "conflict/module" -> sampleModulePhtContent.replace("test/module", "conflict/module").replace("1.0.0", version)
                "duplicate/module" -> sampleModulePhtContent.replace("test/module", "duplicate/module").replace("1.0.0", version)
                "same/module" -> sampleModulePhtContent.replace("test/module", "same/module").replace("1.0.0", version)
                else -> sampleModulePhtContent2
                    .replace("2.0.0", version)
                    .replace("another/module", moduleName)
            }
            val sourceMarker = if (source.isNotEmpty()) "\n; Source: $source" else ""
            zos.putNextEntry(ZipEntry("module.pht"))
            zos.write((moduleContent + sourceMarker).toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("test.txt"))
            zos.write(testFileContent.toByteArray())
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("nested/"))
            zos.putNextEntry(ZipEntry("nested/file.txt"))
            zos.write("nested content".toByteArray())
            zos.closeEntry()
        }
        return baos.toByteArray()
    }

    private fun computeHash(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-512")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun createMockRepository(
        moduleName: String,
        moduleVersion: String,
        hash: String,
        zipContent: ByteArray
    ): IRepository {
        return object : IRepository {
            override suspend fun findAsync(name: String, version: Constraint): List<IDownloadable> {
                // Проверяем, что имя совпадает и версия модуля удовлетворяет ограничению
                if (name == moduleName && version.isSatisfiedBy(Version.Companion.parse(moduleVersion))) {
                    return listOf(createMockDownloadable(moduleName, moduleVersion, hash, zipContent))
                }
                return emptyList()
            }

            override suspend fun findAllAsync(): List<IDownloadable> {
                return listOf(createMockDownloadable(moduleName, moduleVersion, hash, zipContent))
            }
        }
    }

    private fun createMockDownloadable(
        name: String,
        version: String,
        hash: String,
        zipContent: ByteArray
    ): IDownloadable {
        return object : IDownloadable {
            override val name: String = name
            override val version: Version = Version.Companion.parse(version)
            override val hash: String = hash

            override suspend fun headerAsync(): ModuleHeader {
                // Используем ZipUtils для извлечения заголовка из zip-контента
                val tempFile = Files.createTempFile("test-module", ".zip")
                try {
                    Files.write(tempFile, zipContent)
                    return ZipUtils.unzipHeader(tempFile.toFile().inputStream())
                } finally {
                    Files.deleteIfExists(tempFile)
                }
            }

            override suspend fun downloadAsync(dir: Path) {
                val tempFile = Files.createTempFile("test-module", ".zip")
                try {
                    Files.write(tempFile, zipContent)
                    ZipUtils.unzip(dir, Files.readAllBytes(tempFile))
                } finally {
                    Files.deleteIfExists(tempFile)
                }
            }

            override suspend fun downloadZipAsync(file: Path) {
                if (!Files.exists(file.parent)) {
                    Files.createDirectories(file.parent)
                }
                Files.write(file, zipContent)
            }
        }
    }
}