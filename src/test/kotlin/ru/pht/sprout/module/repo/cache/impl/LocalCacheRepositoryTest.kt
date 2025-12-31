package ru.pht.sprout.module.repo.cache.impl

import io.github.z4kn4fein.semver.constraints.toConstraint
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.module.repo.impl.*
import ru.pht.sprout.module.utils.ZipUtils
import ru.pht.sprout.module.utils.useTmpDir
import java.lang.Thread.sleep
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@EnabledIf("ru.pht.sprout.TestConfigInternal#repoTest", disabledReason = "Тест выключен конфигурацией")
class LocalCacheRepositoryTest {
    @Test
    @DisplayName("Поиск / фильтрация / объединение")
    fun findFilterCombineTest() {
        useTmpDir("ProjectSprout.LocalCacheRepositoryTest.findFilterCombineTest") { tmp ->
            val repository = LocalCacheRepository(
                tmp.resolve("modules"),
                tmp.resolve("modules.json"),
                tmp.resolve("modules.lastInvalidationTime.raw"),
                listOf(TestRepositoryA, TestRepositoryC, TestRepositoryD, AssertNoCacheRepository),
                Duration.ZERO
            )
            val find = repository.find("test/a", ">=2.0.0".toConstraint())
            assertEquals(2, find.size)
            assertEquals(
                listOf("test/a@2.0.0", "test/a@3.0.0"),
                find.map { "${it.name}@${it.version}" }
            )
            val (first, second) = find
            if (first is CombinedDownloadable && second is CombinedDownloadable) {
                assertEquals(listOf(TestDownloadableA200A, TestDownloadableA200C), first.originals)
                assertEquals(listOf(TestDownloadableA300A, TestDownloadableA300D), second.originals)
            }
            assertTrue(repository.findAllCached().isEmpty())
        }
    }

    @Test
    @DisplayName("Поиск / фильтрация / верификация / объединение")
    fun findFilterCombineVerifyTest() {
        useTmpDir("ProjectSprout.LocalCacheRepositoryTest.findFilterCombineVerifyTest") { tmp ->
            val repository = LocalCacheRepository(
                tmp.resolve("modules"),
                tmp.resolve("modules.json"),
                tmp.resolve("modules.lastInvalidationTime.raw"),
                listOf(TestRepositoryB, TestRepositoryD, TestRepositoryDCrack),
                Duration.ZERO
            )
            val find = repository.find("test/b", "2.0.0".toConstraint())
            assertEquals(find.size, 1)
            val first = find.first()
            assertNotEquals(first.hash, TestDownloadableB200DCrack.hash)
            if (first is CombinedDownloadable) {
                assertEquals(
                    listOf(
                        TestDownloadableB200B,
                        TestDownloadableB200D,
                        // Компрометация идёт со стороны репозитория - хеш не совпадёт с остальными репозиториями.
                        // Это выявляется в момент получения ссылки, поэтому сам ресурс не проходит.
//                        TestDownloadableB200DCrack
                    ),
                    first.originals
                )
            }
            assertTrue(repository.findAllCached().isEmpty())
        }
    }

    @ParameterizedTest
    @DisplayName("Поиск / фильтрация / объединение / загрузка")
    @CsvSource("true", "false")
    fun findFilterCombineDownloadTest(reversed: Boolean) = runTest {
        useTmpDir("ProjectSprout.LocalCacheRepositoryTest.findFilterCombineVerifyTest") { tmp ->
            val repository = LocalCacheRepository(
                tmp.resolve("modules"),
                tmp.resolve("modules.json"),
                tmp.resolve("modules.lastInvalidationTime.raw"),
                if (reversed)
                    listOf(TestRepositoryDBroken, TestRepositoryD)
                else listOf(TestRepositoryD, TestRepositoryDBroken),
                Duration.ZERO
            )
            // Проверка кеширования
            assertTrue(repository.findAllCachedAsync().isEmpty())
            // Поломка заголовка
            val findA = repository.find("test/a", "3.0.0".toConstraint())
            assertEquals(1, findA.size)
            assertEquals("test/a", findA.first().header().name)
            assertEquals("test/a", findA.first().headerAsync().name)
            // Проверка кеширования
            assertEquals(repository.findAllCachedAsync().size, 1)
            // Поломка скачивания
            useTmpDir("ProjectSprout.NoCacheRepositoryTest.findFilterCombineDownloadTest.sync") { tmp ->
                val find = repository.find("test/b", "2.0.0".toConstraint())
                assertEquals(1, find.size)
                val download = find.first()
                val zip = tmp.resolve("module.zip")
                download.downloadZip(zip)
                assertTrue(zip.exists())
                assertEquals(ZipUtils.calcSHA512(zip.readBytes()), download.hash)
                val tmpUnzip = tmp.resolve("unzip").createDirectory()
                download.download(tmpUnzip)
                assertTrue(tmpUnzip.resolve("test/b/module.pht").exists())
            }
            // Поломка асинхронного скачивания
            useTmpDir("ProjectSprout.NoCacheRepositoryTest.findFilterCombineDownloadTest.async") { tmp ->
                val find = repository.findAsync("test/b", "2.0.0".toConstraint())
                assertEquals(1, find.size)
                val download = find.first()
                val zip = tmp.resolve("module.zip")
                download.downloadZipAsync(zip)
                assertTrue(zip.exists())
                assertEquals(ZipUtils.calcSHA512(zip.readBytes()), download.hash)
                val tmpUnzip = tmp.resolve("unzip").createDirectory()
                download.downloadAsync(tmpUnzip)
                assertTrue(tmpUnzip.resolve("test/b/module.pht").exists())
            }
            // Проверка кеширования
            assertEquals(2, repository.findAllCachedAsync().size)
        }
    }

    @Test
    @DisplayName("Только новое / Только кеш / Кеш + Новое (без обновления кеша)")
    fun cacheDownloadNoInvalidationTest() {
        useTmpDir("ProjectSprout.LocalCacheRepositoryTest.cacheDownloadTest") { tmp ->
            // Загрузка в кеш
            run {
                val original = LocalCacheRepository(
                    tmp.resolve("modules"),
                    tmp.resolve("modules.json"),
                    tmp.resolve("modules.lastInvalidationTime.raw"),
                    listOf(TestRepositoryA),
                    Duration.ZERO
                )
                val find = original.find("test/a", ">=2.0.0".toConstraint())
                assertEquals(2, find.size)
                useTmpDir("ProjectSprout.LocalCacheRepositoryTest.cacheDownloadTest.original") { tmp ->
                    find.forEach { it.downloadZip(tmp.resolve("${it.name}@${it.version}")) }
                    assertTrue(tmp.resolve("test/a@2.0.0").exists())
                    assertTrue(tmp.resolve("test/a@3.0.0").exists())
                }
            }
            // Проверка кеша
            run {
                val cached = LocalCacheRepository(
                    tmp.resolve("modules"),
                    tmp.resolve("modules.json"),
                    tmp.resolve("modules.lastInvalidationTime.raw"),
                    emptyList(),
                    Duration.ZERO
                )
                assertEquals(2, cached.findAllCached().size)
                val find = cached.find("test/a", ">=1.0.0".toConstraint())
                assertEquals(2, find.size)
                useTmpDir("ProjectSprout.LocalCacheRepositoryTest.cacheDownloadTest.cached") { tmp ->
                    find.forEach { it.downloadZip(tmp.resolve("${it.name}@${it.version}")) }
                    assertTrue(tmp.resolve("test/a@2.0.0").exists())
                    assertTrue(tmp.resolve("test/a@3.0.0").exists())
                }
            }
            // Добираем вне кеша, без обновления кеша.
            run {
                val new = LocalCacheRepository(
                    tmp.resolve("modules"),
                    tmp.resolve("modules.json"),
                    tmp.resolve("modules.lastInvalidationTime.raw"),
                    listOf(TestRepositoryA),
                    (-1).milliseconds
                )
                assertEquals(2, new.findAllCached().size)
                val find = new.find("test/a", ">=1.0.0".toConstraint())
                assertEquals(2, find.size)
                useTmpDir("ProjectSprout.LocalCacheRepositoryTest.cacheDownloadTest.new") { tmp ->
                    find.forEach { it.downloadZip(tmp.resolve("${it.name}@${it.version}")) }
                    assertTrue(tmp.resolve("test/a@2.0.0").exists())
                    assertTrue(tmp.resolve("test/a@3.0.0").exists())
                }
                assertEquals(2, new.findAllCached().size)
            }
        }
    }

    @Test
    @DisplayName("Только новое / Только кеш / Кеш + Новое (с переодическим обновлением кеша)")
    fun cacheDownloadTimedInvalidationTest() {
        useTmpDir("ProjectSprout.LocalCacheRepositoryTest.cacheDownloadTest") { tmp ->
            // Загрузка в кеш
            run {
                val original = LocalCacheRepository(
                    tmp.resolve("modules"),
                    tmp.resolve("modules.json"),
                    tmp.resolve("modules.lastInvalidationTime.raw"),
                    listOf(TestRepositoryA),
                    Duration.ZERO
                )
                val find = original.find("test/a", ">=2.0.0".toConstraint())
                assertEquals(2, find.size)
                useTmpDir("ProjectSprout.LocalCacheRepositoryTest.cacheDownloadTest.original") { tmp ->
                    find.forEach { it.downloadZip(tmp.resolve("${it.name}@${it.version}")) }
                    assertTrue(tmp.resolve("test/a@2.0.0").exists())
                    assertTrue(tmp.resolve("test/a@3.0.0").exists())
                }
            }
            // Проверка кеша
            run {
                val cached = LocalCacheRepository(
                    tmp.resolve("modules"),
                    tmp.resolve("modules.json"),
                    tmp.resolve("modules.lastInvalidationTime.raw"),
                    emptyList(),
                    Duration.ZERO
                )
                assertEquals(2, cached.findAllCached().size)
                val find = cached.find("test/a", ">=1.0.0".toConstraint())
                assertEquals(2, find.size)
                useTmpDir("ProjectSprout.LocalCacheRepositoryTest.cacheDownloadTest.cached") { tmp ->
                    find.forEach { it.downloadZip(tmp.resolve("${it.name}@${it.version}")) }
                    assertTrue(tmp.resolve("test/a@2.0.0").exists())
                    assertTrue(tmp.resolve("test/a@3.0.0").exists())
                }
            }
            // Добираем вне кеша, с временным обновлением кеша.
            run {
                val repositories = ArrayList<IRepository>()
                val new = LocalCacheRepository(
                    tmp.resolve("modules"),
                    tmp.resolve("modules.json"),
                    tmp.resolve("modules.lastInvalidationTime.raw"),
                    repositories,
                    10.milliseconds
                )
                // Первая попытка - новых репозиториев нет, обновляем кеш
                var lastInvalidationTime: Duration? = null
                run {
                    assertEquals(2, new.findAllCached().size)
                    val find = new.find("test/a", ">=1.0.0".toConstraint())
                    assertEquals(2, find.size)
                    useTmpDir("ProjectSprout.LocalCacheRepositoryTest.cacheDownloadTest.new") { tmp1 ->
                        find.forEach { it.downloadZip(tmp1.resolve("${it.name}@${it.version}")) }
                        assertTrue(tmp1.resolve("test/a@2.0.0").exists())
                        assertTrue(tmp1.resolve("test/a@3.0.0").exists())
                        val lastInvalidationTimeFile = tmp.resolve("modules.lastInvalidationTime.raw")
                        assertTrue(lastInvalidationTimeFile.exists())
                        lastInvalidationTime = Duration.parse(lastInvalidationTimeFile.readText(Charsets.UTF_8))
                    }
                    assertEquals(2, new.findAllCached().size)
                }
                // Вторая попытка - новый репозиторий есть, но прошло мало времени с обновления кеша
                repositories += TestRepositoryA
                run {
                    assertEquals(2, new.findAllCached().size)
                    val find = new.find("test/a", ">=1.0.0".toConstraint())
                    assertEquals(2, find.size)
                    useTmpDir("ProjectSprout.LocalCacheRepositoryTest.cacheDownloadTest.new") { tmp1 ->
                        find.forEach { it.downloadZip(tmp1.resolve("${it.name}@${it.version}")) }
                        assertTrue(tmp1.resolve("test/a@2.0.0").exists())
                        assertTrue(tmp1.resolve("test/a@3.0.0").exists())
                        val lastInvalidationTimeFile = tmp.resolve("modules.lastInvalidationTime.raw")
                        assertTrue(lastInvalidationTimeFile.exists())
                        assertEquals(lastInvalidationTime.toString(), lastInvalidationTimeFile.readText(Charsets.UTF_8))
                    }
                    assertEquals(2, new.findAllCached().size)
                }
                // Третья попытка - новый репозиторий есть, снова обновляем кеш
                sleep(11)
                run {
                    assertEquals(2, new.findAllCached().size)
                    val find = new.find("test/a", ">=1.0.0".toConstraint())
                    assertEquals(4, find.size)
                    useTmpDir("ProjectSprout.LocalCacheRepositoryTest.cacheDownloadTest.new") { tmp1 ->
                        find.forEach { it.downloadZip(tmp1.resolve("${it.name}@${it.version}")) }
                        assertTrue(tmp1.resolve("test/a@1.0.0").exists())
                        assertTrue(tmp1.resolve("test/a@1.1.0").exists())
                        assertTrue(tmp1.resolve("test/a@2.0.0").exists())
                        assertTrue(tmp1.resolve("test/a@3.0.0").exists())
                        val lastInvalidationTimeFile = tmp.resolve("modules.lastInvalidationTime.raw")
                        assertTrue(lastInvalidationTimeFile.exists())
                        assertNotEquals(lastInvalidationTime.toString(), lastInvalidationTimeFile.readText(Charsets.UTF_8))
                    }
                    assertEquals(4, new.findAllCached().size)
                }
            }
        }
    }

    @Test
    @DisplayName("Только новое / Только кеш / Кеш + Новое (с постоянным обновлением кеша)")
    fun cacheDownloadAlwaysInvalidationTest() {
        useTmpDir("ProjectSprout.LocalCacheRepositoryTest.cacheDownloadTest") { tmp ->
            // Загрузка в кеш
            run {
                val original = LocalCacheRepository(
                    tmp.resolve("modules"),
                    tmp.resolve("modules.json"),
                    tmp.resolve("modules.lastInvalidationTime.raw"),
                    listOf(TestRepositoryA),
                    Duration.ZERO
                )
                val find = original.find("test/a", ">=2.0.0".toConstraint())
                assertEquals(2, find.size)
                useTmpDir("ProjectSprout.LocalCacheRepositoryTest.cacheDownloadTest.original") { tmp ->
                    find.forEach { it.downloadZip(tmp.resolve("${it.name}@${it.version}")) }
                    assertTrue(tmp.resolve("test/a@2.0.0").exists())
                    assertTrue(tmp.resolve("test/a@3.0.0").exists())
                }
            }
            // Проверка кеша
            run {
                val cached = LocalCacheRepository(
                    tmp.resolve("modules"),
                    tmp.resolve("modules.json"),
                    tmp.resolve("modules.lastInvalidationTime.raw"),
                    emptyList(),
                    Duration.ZERO
                )
                assertEquals(2, cached.findAllCached().size)
                val find = cached.find("test/a", ">=1.0.0".toConstraint())
                assertEquals(2, find.size)
                useTmpDir("ProjectSprout.LocalCacheRepositoryTest.cacheDownloadTest.cached") { tmp ->
                    find.forEach { it.downloadZip(tmp.resolve("${it.name}@${it.version}")) }
                    assertTrue(tmp.resolve("test/a@2.0.0").exists())
                    assertTrue(tmp.resolve("test/a@3.0.0").exists())
                }
            }
            // Добираем вне кеша, с постоянным обновлением кеша.
            run {
                val new = LocalCacheRepository(
                    tmp.resolve("modules"),
                    tmp.resolve("modules.json"),
                    tmp.resolve("modules.lastInvalidationTime.raw"),
                    listOf(TestRepositoryA),
                    Duration.ZERO
                )
                assertEquals(2, new.findAllCached().size)
                val find = new.find("test/a", ">=1.0.0".toConstraint())
                assertEquals(4, find.size)
                useTmpDir("ProjectSprout.LocalCacheRepositoryTest.cacheDownloadTest.new") { tmp ->
                    find.forEach { it.downloadZip(tmp.resolve("${it.name}@${it.version}")) }
                    assertTrue(tmp.resolve("test/a@1.0.0").exists())
                    assertTrue(tmp.resolve("test/a@1.1.0").exists())
                    assertTrue(tmp.resolve("test/a@2.0.0").exists())
                    assertTrue(tmp.resolve("test/a@3.0.0").exists())
                }
                assertEquals(4, new.findAllCached().size)
            }
        }
    }

    @Test
    @DisplayName("Поиск всех / объединение")
    fun findAllTest() {
        useTmpDir("ProjectSprout.LocalCacheRepositoryTest.findAllTest") { tmp ->
            val repository = LocalCacheRepository(
                tmp.resolve("modules"),
                tmp.resolve("modules.json"),
                tmp.resolve("modules.lastInvalidationTime.raw"),
                listOf(TestRepositoryA, TestRepositoryC, TestRepositoryD, AssertNoCacheRepository),
                Duration.ZERO
            )
            val list = repository.findAll()
            assertEquals(8, list.size)
            if (list.all { it is CombinedDownloadable }) {
                assertEquals(
                    listOf(
                        listOf(TestDownloadableA100A, TestDownloadableA100C),
                        listOf(TestDownloadableA110A, TestDownloadableA110C),
                        listOf(TestDownloadableA200A, TestDownloadableA200C),
                        listOf(TestDownloadableA300A, TestDownloadableA300D),
                        listOf(TestDownloadableB100A, TestDownloadableB100C),
                        listOf(TestDownloadableB200A, TestDownloadableB200D),
                        listOf(TestDownloadableC100A),
                        listOf(TestDownloadableD100A)
                    ).sortedBy { it.hashCode() },
                    list.map { (it as CombinedDownloadable).originals }.sortedBy { it.hashCode() }
                )
            }
            val find = repository.find("test/a", "1.0.0".toConstraint())
            assertEquals(1, find.size)
            assertContains(list, find.first())
            assertTrue(repository.findAllCached().isEmpty())
        }
    }

    @Test
    @DisplayName("Поиск / верификация / объединение")
    fun findAllVerifyTest() {
        useTmpDir("ProjectSprout.LocalCacheRepositoryTest.findAllTest") { tmp ->
            val repository = LocalCacheRepository(
                tmp.resolve("modules"),
                tmp.resolve("modules.json"),
                tmp.resolve("modules.lastInvalidationTime.raw"),
                listOf(TestRepositoryB, TestRepositoryD, TestRepositoryDCrack),
                Duration.ZERO
            )
            val list = repository.findAll()
            assertEquals(6, list.size)
            if (list.all { it is CombinedDownloadable }) {
                assertEquals(
                    listOf(
                        listOf(TestDownloadableA100B),
                        listOf(TestDownloadableA110B),
                        listOf(TestDownloadableA200B),
                        // Компрометация идёт со стороны пользователя - хеш архива не совпадёт при проверке после его загрузки.
                        // Это выявляется в момент загрузки архива, поэтому сам ресурс проходит.
//                        listOf(TestDownloadableA300B, TestDownloadableA300D),
                        listOf(TestDownloadableA300B, TestDownloadableA300D, TestDownloadableA300DCrack),
                        listOf(TestDownloadableB100B),
                        // Компрометация идёт со стороны репозитория - хеш не совпадёт с остальными репозиториями.
                        // Это выявляется в момент получения ссылки, поэтому сам ресурс не проходит.
                        listOf(TestDownloadableB200B, TestDownloadableB200D)
//                        listOf(TestDownloadableB200B, TestDownloadableB200D, TestDownloadableB200DCrack)
                    ).sortedBy { it.hashCode() },
                    list.map { (it as CombinedDownloadable).originals }.sortedBy { it.hashCode() }
                )
            }
            assertTrue(repository.findAllCached().isEmpty())
        }
    }
}