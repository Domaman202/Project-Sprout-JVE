package ru.pht.sprout.module.repo.cache.impl

import io.github.z4kn4fein.semver.constraints.toConstraint
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.pht.sprout.module.repo.impl.*
import ru.pht.sprout.module.utils.ZipUtils
import ru.pht.sprout.module.utils.useTmpDir
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@EnabledIf("ru.pht.sprout.TestConfigInternal#repoTest", disabledReason = "Тест выключен конфигурацией")
class NoCacheRepositoryTest {
    @Test
    @DisplayName("Поиск / фильтрация / объединение")
    fun findFilterCombineTest() {
        val repository = NoCacheRepository(listOf(TestRepositoryA, TestRepositoryC, TestRepositoryD, AssertNoCacheRepository))
        val find = repository.find("test/a", ">=2.0.0".toConstraint())
        assertEquals(2, find.size)
        assertEquals(
            listOf("test/a@2.0.0", "test/a@3.0.0"),
            find.map { "${it.name}@${it.version}" }
        )
        val (first, second) = find
        if (first is CombinedDownloadable && second is CombinedDownloadable) {
            assertEquals(first.originals, listOf(TestDownloadableA200A, TestDownloadableA200C))
            assertEquals(second.originals, listOf(TestDownloadableA300A, TestDownloadableA300D))
        }
        assertTrue(repository.findAllCached().isEmpty())
    }

    @Test
    @DisplayName("Поиск / фильтрация / верификация / объединение")
    fun findFilterCombineVerifyTest() {
        val repository = NoCacheRepository(listOf(TestRepositoryB, TestRepositoryD, TestRepositoryDCrack))
        val find = repository.find("test/b", "2.0.0".toConstraint())
        assertEquals(1, find.size)
        val first = find.first()
        assertEquals(first.hash, TestDownloadableB200B.hash)
        if (first is CombinedDownloadable) {
            assertEquals(
                listOf(
                    TestDownloadableB200B,
                    TestDownloadableB200D,
                    // Компрометация идёт со стороны репозитория - хеш не совпадёт с остальными репозиториями.
                    // Это выявляется в момент получения ссылки, поэтому сам ресурс не проходит.
//                    TestDownloadableB200DCrack
                ),
                first.originals
            )
        }
        assertTrue(repository.findAllCached().isEmpty())
    }

    @ParameterizedTest
    @DisplayName("Поиск / фильтрация / объединение / загрузка")
    @CsvSource("true", "false")
    fun findFilterCombineDownloadTest(reversed: Boolean) = runTest {
        val repository = NoCacheRepository(
            if (reversed)
                listOf(TestRepositoryDBroken, TestRepositoryD)
            else listOf(TestRepositoryD, TestRepositoryDBroken)
        )
        // Поломка заголовка
        val findA = repository.find("test/a", "3.0.0".toConstraint())
        assertEquals(1, findA.size)
        assertEquals("test/a", findA.first().header().name)
        assertEquals("test/a", findA.first().headerAsync().name)
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
        // Отсутствие кеширования
        assertTrue(repository.findAllCached().isEmpty())
    }

    @Test
    @DisplayName("Поиск всех / объединение")
    fun findAllTest() {
        val repository = NoCacheRepository(listOf(TestRepositoryA, TestRepositoryC, TestRepositoryD, AssertNoCacheRepository))
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

    @Test
    @DisplayName("Поиск / верификация / объединение")
    fun findAllVerifyTest() {
        val repository = NoCacheRepository(listOf(TestRepositoryB, TestRepositoryD, TestRepositoryDCrack))
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
//                    listOf(TestDownloadableA300B, TestDownloadableA300D),
                    listOf(TestDownloadableA300B, TestDownloadableA300D, TestDownloadableA300DCrack),
                    listOf(TestDownloadableB100B),
                    // Компрометация идёт со стороны репозитория - хеш не совпадёт с остальными репозиториями.
                    // Это выявляется в момент получения ссылки, поэтому сам ресурс не проходит.
                    listOf(TestDownloadableB200B, TestDownloadableB200D)
//                    listOf(TestDownloadableB200B, TestDownloadableB200D, TestDownloadableB200DCrack)
                ).sortedBy { it.hashCode() },
                list.map { (it as CombinedDownloadable).originals }.sortedBy { it.hashCode() }
            )
        }
        assertTrue(repository.findAllCached().isEmpty())
    }
}