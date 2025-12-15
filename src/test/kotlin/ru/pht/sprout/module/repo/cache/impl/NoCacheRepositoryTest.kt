package ru.pht.sprout.module.repo.cache.impl

import io.github.z4kn4fein.semver.constraints.toConstraint
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.pht.sprout.module.repo.test.*
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.io.path.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@EnabledIf("ru.pht.sprout.TestConfigInternal#repoTest", disabledReason = "Тест выключен конфигурацией")
@OptIn(ExperimentalPathApi::class)
class NoCacheRepositoryTest {
    @Test
    @DisplayName("Тестирование поиска / фильтрации / объединения")
    fun findFilterCombineTest() {
        val repository = NoCacheRepository(listOf(TestRepositoryA, TestRepositoryC, TestRepositoryD, AssertNoCacheRepository))
        val find = repository.find("test/a", ">=2.0.0".toConstraint())
        assertEquals(find.size, 2)
        assertEquals(
            find.map { "${it.name}@${it.version}" },
            listOf("test/a@2.0.0", "test/a@3.0.0")
        )
        val (first, second) = find
        if (first is NoCacheRepository.CombinedDownloadable && second is NoCacheRepository.CombinedDownloadable) {
            assertEquals(first.originals, listOf(TestDownloadableA200A, TestDownloadableA200C))
            assertEquals(second.originals, listOf(TestDownloadableA300A, TestDownloadableA300D))
        }
        assertTrue(repository.findAllCached().isEmpty())
    }

    @Test
    @DisplayName("Тестирование поиска / фильтрации / верификация / объединения")
    fun findFilterCombineVerifyTest() {
        val repository = NoCacheRepository(listOf(TestRepositoryB, TestRepositoryD, TestRepositoryDCrack))
        val find = repository.find("test/b", "2.0.0".toConstraint())
        assertEquals(find.size, 1)
        val first = find.first()
        assertEquals(first.hash, TestDownloadableB200B.hash)
        if (first is NoCacheRepository.CombinedDownloadable) {
            assertEquals(
                first.originals,
                listOf(
                    TestDownloadableB200B,
                    TestDownloadableB200D,
                    // Компрометация идёт со стороны репозитория - хеш не совпадёт с остальными репозиториями.
                    // Это выявляется в момент получения ссылки, поэтому сам ресурс не проходит.
//                    TestDownloadableB200DCrack
                )
            )
        }
        assertTrue(repository.findAllCached().isEmpty())
    }

    @ParameterizedTest
    @DisplayName("Тестирование поиска / фильтрации / объединения / загрузка")
    @CsvSource("true", "false")
    fun findFilterCombineDownloadTest(reversed: Boolean) = runTest {
        val repository = NoCacheRepository(
            if (reversed)
                listOf(TestRepositoryDBroken, TestRepositoryD)
            else listOf(TestRepositoryD, TestRepositoryDBroken)
        )
        // Поломка заголовка
        val findA = repository.find("test/a", "3.0.0".toConstraint())
        assertEquals(findA.size, 1)
        assertEquals(findA.first().header().name, "test/a")
        assertEquals(findA.first().headerAsync().name, "test/a")
        // Поломка скачивания
        val tmp0 = Files.createTempDirectory("ProjectSprout.NoCacheRepositoryTest.findFilterCombineDownloadTest.sync")
        try {
            val find = repository.find("test/b", "2.0.0".toConstraint())
            assertEquals(find.size, 1)
            val download = find.first()
            val zip = tmp0.resolve("module.zip")
            download.downloadZip(zip)
            assertTrue(zip.exists())
            assertEquals(MessageDigest.getInstance("SHA-512").digest(zip.readBytes()).toHexString(), download.hash)
            val tmpUnzip = tmp0.resolve("unzip").createDirectory()
            download.download(tmpUnzip)
            assertTrue(tmpUnzip.resolve("test/b/module.pht").exists())
        } finally {
            tmp0.deleteRecursively()
        }
        // Поломка скачивания
        val tmp1 = Files.createTempDirectory("ProjectSprout.NoCacheRepositoryTest.findFilterCombineDownloadTest.async")
        try {
            val find = repository.findAsync("test/b", "2.0.0".toConstraint())
            assertEquals(find.size, 1)
            val download = find.first()
            val zip = tmp1.resolve("module.zip")
            download.downloadZipAsync(zip)
            assertTrue(zip.exists())
            assertEquals(MessageDigest.getInstance("SHA-512").digest(zip.readBytes()).toHexString(), download.hash)
            val tmpUnzip = tmp1.resolve("unzip").createDirectory()
            download.downloadAsync(tmpUnzip)
            assertTrue(tmpUnzip.resolve("test/b/module.pht").exists())
        } finally {
            tmp1.deleteRecursively()
        }
    }

    @Test
    @DisplayName("Тестирование поиска / объединения")
    fun findAllTest() {
        val repository = NoCacheRepository(listOf(TestRepositoryA, TestRepositoryC, TestRepositoryD, AssertNoCacheRepository))
        val list = repository.findAll()
        assertEquals(list.size, 8)
        if (list.all { it is NoCacheRepository.CombinedDownloadable }) {
            assertEquals(
                list.map { (it as NoCacheRepository.CombinedDownloadable).originals }.sortedBy { it.hashCode() },
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

    @Test
    @DisplayName("Тестирование поиска / верификация / объединения")
    fun findAllVerifyTest() {
        val repository = NoCacheRepository(listOf(TestRepositoryB, TestRepositoryD, TestRepositoryDCrack))
        val list = repository.findAll()
        assertEquals(list.size, 6)
        if (list.all { it is NoCacheRepository.CombinedDownloadable }) {
            assertEquals(
                list.map { (it as NoCacheRepository.CombinedDownloadable).originals }.sortedBy { it.hashCode() },
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
                ).sortedBy { it.hashCode() }
            )
        }
        assertTrue(repository.findAllCached().isEmpty())
    }
}