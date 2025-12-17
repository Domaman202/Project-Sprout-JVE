package ru.pht.sprout.module.repo.cache.impl

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import ru.pht.sprout.module.repo.impl.*
import ru.pht.sprout.module.utils.useTmpDir
import ru.pht.sprout.utils.fmt.FmtUtils.fmt
import ru.pht.sprout.utils.lang.Language
import ru.pht.sprout.utils.lang.exception.TranslatedIllegalArgumentException
import java.security.MessageDigest
import kotlin.io.path.createDirectory
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CombinedDownloadableTest {
    @Test
    @DisplayName("Успешный of")
    fun ofTest() {
        assertNotNull(CombinedDownloadable.of(listOf(TestDownloadableA100A, TestDownloadableA100B)))
    }

    @Test
    @DisplayName("Провальный of")
    fun ofFailTest() {
        assertEquals(
            assertThrows<TranslatedIllegalArgumentException> {
                CombinedDownloadable.of(listOf(TestDownloadableA100A, TestDownloadableA200A))
            }.translate(Language.ENGLISH),
            "Not all sources in the list belong to module '§sbtest/a@1.0.0§sr'".fmt
        )
    }

    @ParameterizedTest
    @DisplayName("Отказоустойчивость")
    @CsvSource("true", "false")
    fun faultToleranceTest(reversed: Boolean) = runTest {
        val download = CombinedDownloadable.of(
            if (reversed)
                listOf(TestDownloadableA300DBroken, TestDownloadableA300D)
            else listOf(TestDownloadableA300D, TestDownloadableA300DBroken)
        )
        // Поломка заголовка
        assertEquals(download.header().name, "test/a")
        assertEquals(download.headerAsync().name, "test/a")
        // Поломка скачивания
        useTmpDir("ProjectSprout.CombinedDownloadableTest.faultToleranceTest.sync") { tmp ->
            val zip = tmp.resolve("module.zip")
            download.downloadZip(zip)
            assertTrue(zip.exists())
            assertEquals(MessageDigest.getInstance("SHA-512").digest(zip.readBytes()).toHexString(), download.hash)
            val tmpUnzip = tmp.resolve("unzip").createDirectory()
            download.download(tmpUnzip)
            assertTrue(tmpUnzip.resolve("test/a/module.pht").exists())
        }
        // Поломка асинхронного скачивания
        useTmpDir("ProjectSprout.CombinedDownloadableTest.faultToleranceTest.async") { tmp ->
            val zip = tmp.resolve("module.zip")
            download.downloadZipAsync(zip)
            assertTrue(zip.exists())
            assertEquals(MessageDigest.getInstance("SHA-512").digest(zip.readBytes()).toHexString(), download.hash)
            val tmpUnzip = tmp.resolve("unzip").createDirectory()
            download.downloadAsync(tmpUnzip)
            assertTrue(tmpUnzip.resolve("test/a/module.pht").exists())
        }
    }

    @Test
    @DisplayName("equals & hash")
    fun equalsAndHashTest() {
        val a0 = CombinedDownloadable.of(listOf(TestDownloadableA100A))
        val a1 = CombinedDownloadable.of(listOf(TestDownloadableA100A))
        val ab = CombinedDownloadable.of(listOf(TestDownloadableA100A, TestDownloadableA100B))
        assertEquals(a0, a1)
        assertEquals(a0.hashCode(), a1.hashCode())
        assertNotEquals(a0, ab)
        assertNotEquals(a0.hashCode(), ab.hashCode())
    }
}