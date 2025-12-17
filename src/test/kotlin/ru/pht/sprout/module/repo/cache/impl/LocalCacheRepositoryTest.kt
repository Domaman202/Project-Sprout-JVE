package ru.pht.sprout.module.repo.cache.impl

import io.github.z4kn4fein.semver.constraints.toConstraint
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.pht.sprout.module.repo.impl.*
import ru.pht.sprout.module.utils.useTmpDir
import java.security.MessageDigest
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@EnabledIf("ru.pht.sprout.TestConfigInternal#repoTest", disabledReason = "Тест выключен конфигурацией")
class LocalCacheRepositoryTest {
    @Test
    @DisplayName("Поиск / фильтрация / объединение")
    fun findFilterCombineTest() {
        useTmpDir("ProjectSprout.LocalCacheRepositoryTest.findFilterCombineTest") { tmp ->
            val repository = LocalCacheRepository(
                tmp.resolve("modules"),
                tmp.resolve("modules.json"),
                listOf(TestRepositoryA, TestRepositoryC, TestRepositoryD, AssertNoCacheRepository)
            )
            val find = repository.find("test/a", ">=2.0.0".toConstraint())
            assertEquals(find.size, 2)
            assertEquals(
                find.map { "${it.name}@${it.version}" },
                listOf("test/a@2.0.0", "test/a@3.0.0")
            )
            val (first, second) = find
            if (first is CombinedDownloadable && second is CombinedDownloadable) {
                assertEquals(first.originals, listOf(TestDownloadableA200A, TestDownloadableA200C))
                assertEquals(second.originals, listOf(TestDownloadableA300A, TestDownloadableA300D))
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
                listOf(TestRepositoryB, TestRepositoryD, TestRepositoryDCrack)
            )
            val find = repository.find("test/b", "2.0.0".toConstraint())
            assertEquals(find.size, 1)
            val first = find.first()
            assertEquals(first.hash, TestDownloadableB200B.hash)
            if (first is CombinedDownloadable) {
                assertEquals(
                    first.originals,
                    listOf(
                        TestDownloadableB200B,
                        TestDownloadableB200D,
                        // Компрометация идёт со стороны репозитория - хеш не совпадёт с остальными репозиториями.
                        // Это выявляется в момент получения ссылки, поэтому сам ресурс не проходит.
//                        TestDownloadableB200DCrack
                    )
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
                if (reversed)
                    listOf(TestRepositoryDBroken, TestRepositoryD)
                else listOf(TestRepositoryD, TestRepositoryDBroken)
            )
            // Проверка кеширования
            assertTrue(repository.findAllCachedAsync().isEmpty())
            // Поломка заголовка
            val findA = repository.find("test/a", "3.0.0".toConstraint())
            assertEquals(findA.size, 1)
            assertEquals(findA.first().header().name, "test/a")
            assertEquals(findA.first().headerAsync().name, "test/a")
            // Проверка кеширования
            assertEquals(repository.findAllCachedAsync().size, 1)
            // Поломка скачивания
            useTmpDir("ProjectSprout.NoCacheRepositoryTest.findFilterCombineDownloadTest.sync") { tmp ->
                val find = repository.find("test/b", "2.0.0".toConstraint())
                assertEquals(find.size, 1)
                val download = find.first()
                val zip = tmp.resolve("module.zip")
                download.downloadZip(zip)
                assertTrue(zip.exists())
                assertEquals(MessageDigest.getInstance("SHA-512").digest(zip.readBytes()).toHexString(), download.hash)
                val tmpUnzip = tmp.resolve("unzip").createDirectory()
                download.download(tmpUnzip)
                assertTrue(tmpUnzip.resolve("test/b/module.pht").exists())
            }
            // Поломка асинхронного скачивания
            useTmpDir("ProjectSprout.NoCacheRepositoryTest.findFilterCombineDownloadTest.async") { tmp ->
                val find = repository.findAsync("test/b", "2.0.0".toConstraint())
                assertEquals(find.size, 1)
                val download = find.first()
                val zip = tmp.resolve("module.zip")
                download.downloadZipAsync(zip)
                assertTrue(zip.exists())
                assertEquals(MessageDigest.getInstance("SHA-512").digest(zip.readBytes()).toHexString(), download.hash)
                val tmpUnzip = tmp.resolve("unzip").createDirectory()
                download.downloadAsync(tmpUnzip)
                assertTrue(tmpUnzip.resolve("test/b/module.pht").exists())
            }
            // Проверка кеширования
            assertEquals(repository.findAllCachedAsync().size, 2)
        }
    }

    @Test
    @DisplayName("Только новое / Только кеш / Кеш + Новое")
    fun cacheDownloadTest() {
        useTmpDir("ProjectSprout.LocalCacheRepositoryTest.cacheDownloadTest") { tmp ->
            // Загрузка в кеш
            run {
                val original = LocalCacheRepository(
                    tmp.resolve("modules"),
                    tmp.resolve("modules.json"),
                    listOf(TestRepositoryA)
                )
                val find = original.find("test/a", ">=2.0.0".toConstraint())
                assertEquals(find.size, 2)
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
                    emptyList()
                )
                assertEquals(cached.findAllCached().size, 2)
                val find = cached.find("test/a", ">=1.0.0".toConstraint())
                assertEquals(find.size, 2)
                useTmpDir("ProjectSprout.LocalCacheRepositoryTest.cacheDownloadTest.cached") { tmp ->
                    find.forEach { it.downloadZip(tmp.resolve("${it.name}@${it.version}")) }
                    assertTrue(tmp.resolve("test/a@2.0.0").exists())
                    assertTrue(tmp.resolve("test/a@3.0.0").exists())
                }
            }
            // Добираем вне кеша
            run {
                val new = LocalCacheRepository(
                    tmp.resolve("modules"),
                    tmp.resolve("modules.json"),
                    listOf(TestRepositoryA)
                )
                assertEquals(new.findAllCached().size, 2)
                val find = new.find("test/a", ">=1.0.0".toConstraint())
                assertEquals(find.size, 4)
                useTmpDir("ProjectSprout.LocalCacheRepositoryTest.cacheDownloadTest.new") { tmp ->
                    find.forEach { it.downloadZip(tmp.resolve("${it.name}@${it.version}")) }
                    assertTrue(tmp.resolve("test/a@1.0.0").exists())
                    assertTrue(tmp.resolve("test/a@1.1.0").exists())
                    assertTrue(tmp.resolve("test/a@2.0.0").exists())
                    assertTrue(tmp.resolve("test/a@3.0.0").exists())
                }
                assertEquals(new.findAllCached().size, 4)
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
                listOf(TestRepositoryA, TestRepositoryC, TestRepositoryD, AssertNoCacheRepository)
            )
            val list = repository.findAll()
            assertEquals(list.size, 8)
            if (list.all { it is CombinedDownloadable }) {
                assertEquals(
                    list.map { (it as CombinedDownloadable).originals }.sortedBy { it.hashCode() },
                    listOf(
                        listOf(TestDownloadableA100A, TestDownloadableA100C),
                        listOf(TestDownloadableA110A, TestDownloadableA110C),
                        listOf(TestDownloadableA200A, TestDownloadableA200C),
                        listOf(TestDownloadableA300A, TestDownloadableA300D),
                        listOf(TestDownloadableB100A, TestDownloadableB100C),
                        listOf(TestDownloadableB200A, TestDownloadableB200D),
                        listOf(TestDownloadableC100A),
                        listOf(TestDownloadableD100A)
                    ).sortedBy { it.hashCode() }
                )
            }
            val find = repository.find("test/a", "1.0.0".toConstraint())
            assertEquals(find.size, 1)
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
                listOf(TestRepositoryB, TestRepositoryD, TestRepositoryDCrack)
            )
            val list = repository.findAll()
            assertEquals(list.size, 6)
            if (list.all { it is CombinedDownloadable }) {
                assertEquals(
                    list.map { (it as CombinedDownloadable).originals }.sortedBy { it.hashCode() },
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
                    ).sortedBy { it.hashCode() }
                )
            }
            assertTrue(repository.findAllCached().isEmpty())
        }
    }
}