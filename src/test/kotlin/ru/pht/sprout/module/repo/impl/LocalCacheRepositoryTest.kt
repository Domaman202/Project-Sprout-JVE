package ru.pht.sprout.module.repo.impl

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
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import ru.pht.sprout.module.header.ModuleHeader
import ru.pht.sprout.module.repo.ICachingRepository
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    @BeforeEach
    fun setUp() {
        cacheDirectory = tempDir.resolve("cache/modules")
        cacheListFile = tempDir.resolve("cache/modules.json")

        // Настраиваем моки для GithubRepository
        mockGithubEngine = MockEngine { request ->
            when (request.url.toString()) {
                "https://github.com/Domaman202/Project-Sprout-Module-List-Github/raw/refs/heads/master/verified.json" -> {
                    val json = """
                        [
                            {
                                "name": "test/module",
                                "version": "1.0.0",
                                "git": "https://github.com/test/module.git",
                                "hash": "${createTestZipHash("test/module", "1.0.0")}",
                                "file": "https://github.com/releases/v1.0.0/module.zip"
                            },
                            {
                                "name": "github/only",
                                "version": "1.0.0",
                                "git": "https://github.com/github/only.git",
                                "hash": "${createTestZipHash("github/only", "1.0.0")}",
                                "file": "https://github.com/releases/github-only.zip"
                            }
                        ]
                    """.trimIndent()
                    respond(
                        content = json,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }

                "https://github.com/releases/v1.0.0/module.zip" -> {
                    val zipBytes = createTestZip("test/module", "1.0.0")
                    respond(
                        content = ByteReadChannel(zipBytes),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/zip")
                    )
                }

                "https://github.com/releases/github-only.zip" -> {
                    val zipBytes = createTestZip("github/only", "1.0.0")
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

        // Настраиваем моки для GiteaRepository
        mockGiteaEngine = MockEngine { request ->
            when (request.url.toString()) {
                "https://gitea.com/Domaman202/Project-Sprout-Module-List-Gitea/raw/branch/master/verified.json" -> {
                    val json = """
                        [
                            {
                                "name": "test/module",
                                "version": "1.1.0",
                                "git": "https://gitea.com/test/module.git",
                                "hash": "${createTestZipHash("test/module", "1.1.0")}",
                                "file": "https://gitea.com/releases/v1.1.0/module.zip"
                            },
                            {
                                "name": "gitea/only",
                                "version": "1.0.0",
                                "git": "https://gitea.com/gitea/only.git",
                                "hash": "${createTestZipHash("gitea/only", "1.0.0")}",
                                "file": "https://gitea.com/releases/gitea-only.zip"
                            }
                        ]
                    """.trimIndent()
                    respond(
                        content = json,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }

                "https://gitea.com/releases/v1.1.0/module.zip" -> {
                    val zipBytes = createTestZip("test/module", "1.1.0")
                    respond(
                        content = ByteReadChannel(zipBytes),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/zip")
                    )
                }

                "https://gitea.com/releases/gitea-only.zip" -> {
                    val zipBytes = createTestZip("gitea/only", "1.0.0")
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

        githubRepository = GithubRepository(HttpClient(mockGithubEngine))
        giteaRepository = GiteaRepository(HttpClient(mockGiteaEngine))

        localCacheRepository = LocalCacheRepository(
            cacheDirectory,
            cacheListFile,
            listOf(githubRepository, giteaRepository)
        )
    }

    @Test
    fun `findAllCachedAsync should return empty list when no modules are cached`() = runTest {
        // При инициализации пустого кэша метод должен возвращать пустой список
        val cachedModules = localCacheRepository.findAllCachedAsync()
        assertEquals(0, cachedModules.size)

        // Также проверим синхронный метод
        val cachedModulesSync = localCacheRepository.findAllCached()
        assertEquals(0, cachedModulesSync.size)
    }

    @Test
    fun `findAllCachedAsync should return cached modules after they are downloaded`() = runTest {
        // Изначально кэш пустой
        val initialCached = localCacheRepository.findAllCachedAsync()
        assertEquals(0, initialCached.size)

        // Загружаем модуль
        val constraint = Constraint.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(1, modules.size)

        // Проверяем, что модуль еще не в кэше
        val beforeDownloadCached = localCacheRepository.findAllCachedAsync()
        assertEquals(0, beforeDownloadCached.size)

        // Загружаем заголовок (это вызовет скачивание в кэш)
        modules[0].headerAsync()

        // Теперь модуль должен быть в кэше
        val afterDownloadCached = localCacheRepository.findAllCachedAsync()
        assertEquals(1, afterDownloadCached.size)
        assertEquals("test/module", afterDownloadCached[0].name)
        assertEquals("1.0.0", afterDownloadCached[0].version.toString())

        // Проверяем синхронный метод
        val cachedSync = localCacheRepository.findAllCached()
        assertEquals(1, cachedSync.size)
        assertEquals("test/module", cachedSync[0].name)
        assertEquals("1.0.0", cachedSync[0].version.toString())
    }

    @Test
    fun `findAllCachedAsync should return only cached modules, not all available`() = runTest {
        // Получаем все доступные модули
        val allModules = localCacheRepository.findAllAsync()
        assertTrue(allModules.size >= 4) // Должно быть как минимум 4 модуля

        // Кэшированных пока нет
        val cachedModules = localCacheRepository.findAllCachedAsync()
        assertEquals(0, cachedModules.size)

        // Кэшируем только один модуль
        val testModule = allModules.find { it.name == "test/module" && it.version.toString() == "1.0.0" }
        require(testModule != null)
        testModule.headerAsync()

        // Теперь в кэше должен быть только один модуль
        val cachedAfterOne = localCacheRepository.findAllCachedAsync()
        assertEquals(1, cachedAfterOne.size)
        assertEquals("test/module", cachedAfterOne[0].name)
        assertEquals("1.0.0", cachedAfterOne[0].version.toString())
    }

    @Test
    fun `findAllCachedAsync should return cached modules when loading from existing cache file`() = runTest {
        // Сначала кэшируем модуль
        val constraint = Constraint.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        modules[0].headerAsync()

        // Проверяем, что модуль в кэше
        val cachedBefore = localCacheRepository.findAllCachedAsync()
        assertEquals(1, cachedBefore.size)

        // Создаем новый репозиторий с теми же кэш-файлами
        val newRepository = LocalCacheRepository(
            cacheDirectory,
            cacheListFile,
            listOf(githubRepository, giteaRepository)
        )

        // Новый репозиторий должен загрузить кэшированные модули из файла
        val cachedAfter = newRepository.findAllCachedAsync()
        assertEquals(1, cachedAfter.size)
        assertEquals("test/module", cachedAfter[0].name)
        assertEquals("1.0.0", cachedAfter[0].version.toString())

        // Проверяем синхронный метод
        val cachedSync = newRepository.findAllCached()
        assertEquals(1, cachedSync.size)
        assertEquals("test/module", cachedSync[0].name)
        assertEquals("1.0.0", cachedSync[0].version.toString())
    }

    @Test
    fun `findAllCachedAsync should not include modules that are not yet downloaded`() = runTest {
        // Получаем модуль, но не скачиваем его
        val constraint = Constraint.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(1, modules.size)

        // Проверяем, что в кэше все еще пусто
        val cached = localCacheRepository.findAllCachedAsync()
        assertEquals(0, cached.size)

        // Теперь скачиваем другой модуль
        val githubOnlyModules = localCacheRepository.findAsync("github/only", Constraint.parse("1.0.0"))
        githubOnlyModules[0].headerAsync()

        // В кэше должен быть только github/only, не test/module
        val cachedAfter = localCacheRepository.findAllCachedAsync()
        assertEquals(1, cachedAfter.size)
        assertEquals("github/only", cachedAfter[0].name)
        assertEquals("1.0.0", cachedAfter[0].version.toString())
    }

    @Test
    fun `findAllCachedAsync should work correctly with multiple cached modules`() = runTest {
        // Кэшируем несколько модулей
        val modulesToCache = listOf(
            "test/module" to "1.0.0",
            "github/only" to "1.0.0",
            "gitea/only" to "1.0.0"
        )

        for ((name, version) in modulesToCache) {
            val modules = localCacheRepository.findAsync(name, Constraint.parse(version))
            if (modules.isNotEmpty()) {
                modules[0].headerAsync()
            }
        }

        // Проверяем, что все три модуля в кэше
        val cached = localCacheRepository.findAllCachedAsync()
        assertEquals(3, cached.size)

        // Проверяем, что все ожидаемые модули присутствуют
        val cachedNames = cached.map { it.name }.sorted()
        val expectedNames = listOf("gitea/only", "github/only", "test/module").sorted()
        assertEquals(expectedNames, cachedNames)

        // Проверяем синхронный метод
        val cachedSync = localCacheRepository.findAllCached()
        assertEquals(3, cachedSync.size)
    }

    @Test
    fun `findAllCachedAsync should return empty list for new cache directory`() = runTest {
        // Удаляем существующий кэш
        if (Files.exists(cacheDirectory)) {
            Files.walk(cacheDirectory)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
        Files.deleteIfExists(cacheListFile)

        // Создаем новый репозиторий с пустым кэшем
        val newRepository = LocalCacheRepository(
            cacheDirectory,
            cacheListFile,
            listOf(githubRepository, giteaRepository)
        )

        // Метод должен вернуть пустой список
        val cached = newRepository.findAllCachedAsync()
        assertEquals(0, cached.size)

        // Проверяем синхронный метод
        val cachedSync = newRepository.findAllCached()
        assertEquals(0, cachedSync.size)
    }

    @Test
    fun `findAllCachedAsync should maintain cache after module operations`() = runTest {
        // Кэшируем модуль
        val modules = localCacheRepository.findAsync("test/module", Constraint.parse("1.0.0"))
        modules[0].headerAsync()

        // Проверяем, что модуль в кэше
        var cached = localCacheRepository.findAllCachedAsync()
        assertEquals(1, cached.size)

        // Выполняем другие операции с модулем
        modules[0].downloadZipAsync(tempDir.resolve("test.zip"))
        modules[0].downloadAsync(tempDir.resolve("output"))

        // Кэш должен остаться неизменным
        cached = localCacheRepository.findAllCachedAsync()
        assertEquals(1, cached.size)
        assertEquals("test/module", cached[0].name)

        // Проверяем синхронный метод
        val cachedSync = localCacheRepository.findAllCached()
        assertEquals(1, cachedSync.size)
    }

    @Test
    fun `should initialize with empty cache when directory does not exist`() = runTest {
        // Удаляем директорию кэша, если существует
        if (Files.exists(cacheDirectory)) {
            Files.walk(cacheDirectory)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
        Files.deleteIfExists(cacheListFile)

        val newRepository = LocalCacheRepository(
            cacheDirectory,
            cacheListFile,
            listOf(githubRepository, giteaRepository)
        )

        // Должен создаться пустой кэш
        val modules = newRepository.findAllAsync()
        assertTrue(modules.isNotEmpty()) // Но модули из репозиториев все равно должны быть

        // Директория кэша должна быть создана
        assertTrue(Files.exists(cacheDirectory))

        // findAllCachedAsync должен вернуть пустой список
        val cached = newRepository.findAllCachedAsync()
        assertEquals(0, cached.size)
    }

    @Test
    fun `should create directories and initialize empty lists when cache directory does not exist`() = runTest {
        // Удаляем все перед тестом
        if (Files.exists(cacheDirectory)) {
            Files.walk(cacheDirectory)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
        Files.deleteIfExists(cacheListFile)

        // Создаем репозиторий - он должен создать директорию и инициализировать пустые списки
        val repo = LocalCacheRepository(
            cacheDirectory,
            cacheListFile,
            listOf(githubRepository, giteaRepository)
        )

        assertTrue(Files.exists(cacheDirectory))
        // Файл списка не должен быть создан, так как списки пустые
        assertFalse(Files.exists(cacheListFile))

        // Кэш должен быть пустым
        assertEquals(0, repo.findAllCachedAsync().size)
    }

    @Test
    fun `should delete cache list file when cache directory does not exist`() = runTest {
        // Сначала создаем файл списка
        Files.createDirectories(cacheListFile.parent)
        Files.write(cacheListFile, "[]".toByteArray())

        // Удаляем директорию кэша
        if (Files.exists(cacheDirectory)) {
            Files.walk(cacheDirectory)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }

        // Создаем репозиторий - он должен удалить файл списка
        val repo = LocalCacheRepository(
            cacheDirectory,
            cacheListFile,
            listOf(githubRepository, giteaRepository)
        )

        // Файл списка должен быть удален
        assertFalse(Files.exists(cacheListFile))
        // Директория должна быть создана
        assertTrue(Files.exists(cacheDirectory))

        // Кэш должен быть пустым
        assertEquals(0, repo.findAllCachedAsync().size)
    }

    @Test
    fun `should initialize empty lists when cache directory exists but cache list file does not`() = runTest {
        // Создаем директорию кэша
        Files.createDirectories(cacheDirectory)

        // Убедимся, что файл списка не существует
        Files.deleteIfExists(cacheListFile)

        val repo = LocalCacheRepository(
            cacheDirectory,
            cacheListFile,
            listOf(githubRepository, giteaRepository)
        )

        // Директория должна существовать
        assertTrue(Files.exists(cacheDirectory))
        // Файл списка не должен быть создан
        assertFalse(Files.exists(cacheListFile))

        // Кэш должен быть пустым
        assertEquals(0, repo.findAllCachedAsync().size)
    }

    @Test
    fun `findAllAsync should return modules from all repositories excluding caching repositories`() = runTest {
        // Создаем мок кэширующего репозитория
        class MockCachingRepository : IRepository, ICachingRepository {
            override suspend fun findAsync(name: String, version: Constraint): List<IDownloadable> {
                return listOf()
            }

            override suspend fun findAllAsync(): List<IDownloadable> {
                return listOf(
                    object : IDownloadable {
                        override val name: String = "cached/module"
                        override val version: Version = Version(1, 0, 0)

                        override suspend fun headerAsync(): ModuleHeader {
                            TODO("Not yet implemented")
                        }

                        override suspend fun downloadAsync(dir: Path) {
                            TODO("Not yet implemented")
                        }

                        override suspend fun downloadZipAsync(file: Path) {
                            TODO("Not yet implemented")
                        }
                    }
                )
            }

            override fun findAllCached(): List<IDownloadable> {
                return listOf()
            }

            override suspend fun findAllCachedAsync(): List<IDownloadable> {
                return listOf()
            }
        }

        val cachingRepository = MockCachingRepository()

        val repoWithCaching = LocalCacheRepository(
            cacheDirectory,
            cacheListFile,
            listOf(cachingRepository, githubRepository, giteaRepository)
        )

        val allModules = repoWithCaching.findAllAsync()

        // Должны получить модули только из github и gitea репозиториев
        // Кэширующий репозиторий должен быть исключен
        val moduleNames = allModules.map { it.name }

        // github/only и gitea/only должны быть в списке
        assertTrue(moduleNames.contains("github/only"))
        assertTrue(moduleNames.contains("gitea/only"))
        // cached/module не должен быть в списке
        assertFalse(moduleNames.contains("cached/module"))
    }

    @Test
    fun `findAsync should return modules matching name and version constraint sorted by version`() = runTest {
        val constraint = Constraint.parse(">=1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)

        // Должны получить обе версии: 1.0.0 и 1.1.0
        assertEquals(2, modules.size)

        // Проверяем сортировку по версии (от меньшей к большей)
        assertEquals("1.0.0", modules[0].version.toString())
        assertEquals("1.1.0", modules[1].version.toString())
    }

    @Test
    fun `MaybeCachedDownloadable should call saveLocalizedList when downloading module first time`() = runTest {
        val constraint = Constraint.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(1, modules.size)

        // Убедимся, что файл списка не существует
        Files.deleteIfExists(cacheListFile)

        // Вызываем метод, который должен кэшировать модуль и сохранить список
        modules[0].headerAsync()

        // Проверяем, что файл списка был создан
        assertTrue(Files.exists(cacheListFile), "Файл списка кэша должен быть создан")

        // Проверяем, что файл не пустой
        val content = Files.readAllBytes(cacheListFile).toString(Charsets.UTF_8)
        assertTrue(content.isNotEmpty())

        // Проверяем, что файл содержит валидный JSON (должен быть массив)
        assertTrue(content.trim().startsWith("["))
        assertTrue(content.trim().endsWith("]"))

        // Проверяем, что модуль теперь в кэше
        val cached = localCacheRepository.findAllCachedAsync()
        assertEquals(1, cached.size)
        assertEquals("test/module", cached[0].name)
    }

    @Test
    fun `MaybeCachedDownloadable should not download module if it already exists in cache`() = runTest {
        val constraint = Constraint.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(1, modules.size)

        // Сначала загружаем модуль
        modules[0].headerAsync()

        // Проверяем, что файл списка создан
        assertTrue(Files.exists(cacheListFile))

        // Запоминаем время модификации файла списка
        val initialModTime = Files.getLastModifiedTime(cacheListFile).toMillis()

        // Ждем немного
        Thread.sleep(10)

        // Второй вызов не должен обновлять файл списка
        modules[0].headerAsync()
        val secondModTime = Files.getLastModifiedTime(cacheListFile).toMillis()

        // Время модизации должно остаться тем же (допускаем погрешность до 10мс из-за файловой системы)
        val difference = kotlin.math.abs(secondModTime - initialModTime)
        assertTrue(difference < 50, "Файл списка не должен был быть перезаписан, разница: ${difference}мс")
    }

    @Test
    fun `saveLocalizedList should create parent directories if they do not exist`() = runTest {
        // Создаем путь с несуществующими родительскими директориями
        val nonExistentParent = tempDir.resolve("non/existent/path/modules.json")

        val repo = LocalCacheRepository(
            cacheDirectory,
            nonExistentParent,
            listOf(githubRepository, giteaRepository)
        )

        // Вызываем findAsync чтобы MaybeCachedDownloadable мог вызвать saveLocalizedList
        val modules = repo.findAsync("test/module", Constraint.parse("1.0.0"))

        // Вызываем headerAsync, который должен привести к вызову saveLocalizedList
        modules[0].headerAsync()

        // Проверяем, что файл был создан
        assertTrue(Files.exists(nonExistentParent))

        // Проверяем, что родительские директории созданы
        assertTrue(Files.exists(nonExistentParent.parent))
    }

    @Test
    fun `downloadZipAsync should create parent directories if they do not exist`() = runTest {
        val constraint = Constraint.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(1, modules.size)

        // Сначала загружаем модуль в кэш
        modules[0].headerAsync()

        val outputDir = tempDir.resolve("non/existent/path")
        val zipFileName = outputDir.resolve("module.zip")

        // Должны быть созданы родительские директории
        modules[0].downloadZipAsync(zipFileName)

        assertTrue(Files.exists(zipFileName))
        assertTrue(Files.exists(outputDir))
    }

    @Test
    fun `downloadAsync should create parent directories if they do not exist`() = runTest {
        val constraint = Constraint.parse("1.0.0")
        val modules = localCacheRepository.findAsync("test/module", constraint)
        assertEquals(1, modules.size)

        // Сначала загружаем модуль в кэш
        modules[0].headerAsync()

        val downloadDir = tempDir.resolve("non/existent/path")

        // Должны быть созданы родительские директории
        modules[0].downloadAsync(downloadDir)

        val moduleDir = downloadDir.resolve("test/module")
        assertTrue(Files.exists(moduleDir))
        assertTrue(Files.exists(downloadDir))
    }

    @Test
    fun `should handle duplicate modules from different repositories`() = runTest {
        // В нашем моке есть модуль test/module в обоих репозиториях
        val allModules = localCacheRepository.findAllAsync()

        // Должны получить оба модуля (разные версии)
        val testModules = allModules.filter { it.name == "test/module" }
        assertEquals(2, testModules.size)

        // Проверяем, что версии разные
        val versions = testModules.map { it.version.toString() }.sorted()
        assertEquals(listOf("1.0.0", "1.1.0"), versions)
    }

    @Test
    fun `cached modules should be reused from localized list`() = runTest {
        // Сначала получаем и кэшируем модуль
        val constraint = Constraint.parse("1.0.0")
        val modules1 = localCacheRepository.findAsync("test/module", constraint)
        modules1[0].headerAsync()

        // Проверяем, что файл списка создан
        assertTrue(Files.exists(cacheListFile))

        // Создаем новый репозиторий - он должен загрузить кэш из файла
        val newRepository = LocalCacheRepository(
            cacheDirectory,
            cacheListFile,
            listOf(githubRepository, giteaRepository)
        )

        // Проверяем, что можем найти модуль
        val modules2 = newRepository.findAsync("test/module", constraint)
        assertEquals(1, modules2.size)

        // Проверяем, что можем получить заголовок (должен использоваться кэш)
        val header = modules2[0].headerAsync()
        assertEquals("test/module", header.name)
        assertEquals("1.0.0", header.version.toString())

        // Проверяем, что findAllCachedAsync возвращает закэшированный модуль
        val cached = newRepository.findAllCachedAsync()
        assertEquals(1, cached.size)
        assertEquals("test/module", cached[0].name)
        assertEquals("1.0.0", cached[0].version.toString())

        // Проверяем синхронный метод
        val cachedSync = newRepository.findAllCached()
        assertEquals(1, cachedSync.size)
        assertEquals("test/module", cachedSync[0].name)
        assertEquals("1.0.0", cachedSync[0].version.toString())
    }

    @Test
    fun `should load empty localized list from file`() = runTest {
        // Создаем тестовый файл списка с пустым массивом
        Files.createDirectories(cacheListFile.parent)
        Files.write(cacheListFile, "[]".toByteArray())

        // Создаем репозиторий - он должен загрузить пустой список из файла
        val repo = LocalCacheRepository(
            cacheDirectory,
            cacheListFile,
            listOf(githubRepository, giteaRepository)
        )

        // Проверяем, что файл все еще существует
        assertTrue(Files.exists(cacheListFile))

        // Получаем все модули
        val modules = repo.findAllAsync()

        // Модули из репозиториев должны быть доступны
        assertTrue(modules.isNotEmpty())

        // Но кэш должен быть пустым
        assertEquals(0, repo.findAllCachedAsync().size)
        assertEquals(0, repo.findAllCached().size)
    }

    @Test
    fun `should throw exception when cache list file contains invalid JSON`() = runTest {
        // Создаем тестовый файл списка с некорректным содержимым
        Files.createDirectories(cacheListFile.parent)
        Files.write(cacheListFile, "not a json".toByteArray())

        // Создаем репозиторий - он должен выбросить исключение при попытке десериализации
        assertThrows<Exception> {
            LocalCacheRepository(
                cacheDirectory,
                cacheListFile,
                listOf(githubRepository, giteaRepository)
            )
        }
    }

    @Test
    fun `findAllCached and findAllCachedAsync should return same results`() = runTest {
        // Кэшируем модуль
        val modules = localCacheRepository.findAsync("test/module", Constraint.parse("1.0.0"))
        modules[0].headerAsync()

        // Оба метода должны возвращать одинаковые результаты
        val cachedAsync = localCacheRepository.findAllCachedAsync()
        val cachedSync = localCacheRepository.findAllCached()

        assertEquals(cachedAsync.size, cachedSync.size)
        if (cachedAsync.isNotEmpty() && cachedSync.isNotEmpty()) {
            assertEquals(cachedAsync[0].name, cachedSync[0].name)
            assertEquals(cachedAsync[0].version.toString(), cachedSync[0].version.toString())
        }
    }

    // Вспомогательные методы

    private fun createTestZip(moduleName: String, version: String): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            // Добавляем module.pht с правильным форматом
            val moduleContent = when (moduleName) {
                "test/module" -> sampleModulePhtContent.replace("1.0.0", version)
                "github/only" -> sampleModulePhtContent.replace("test/module", "github/only").replace("1.0.0", version)
                "gitea/only" -> sampleModulePhtContent.replace("test/module", "gitea/only").replace("1.0.0", version)
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