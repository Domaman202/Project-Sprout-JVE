package ru.pht.sprout.module.utils

import io.github.z4kn4fein.semver.toVersion
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import java.nio.file.Files
import kotlin.io.path.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@EnabledIf("ru.pht.sprout.TestConfigInternal#zipTest", disabledReason = "Тест выключен конфигурацией")
@OptIn(ExperimentalPathApi::class)
class ZipUtilsTest {
    @Test
    @DisplayName("Хеш SHA-512")
    fun sha512Test() {
        val hash0 = ZipUtils.calcSHA512(byteArrayOf(12, 21, 33))
        val hash1 = ZipUtils.calcSHA512(byteArrayOf(12, 21, 33))
        val hash2 = ZipUtils.calcSHA512(byteArrayOf(127, 0, 127))
        assertEquals(hash0, hash1)
        assertNotEquals(hash0, hash2)
    }

    @Test
    @DisplayName("Архивация / распаковка")
    fun zipUnzipTest() {
        val tmpZip = Files.createTempDirectory("ProjectSprout.ZipUtilsTest.TestZip")
        val tmpUnzip = Files.createTempDirectory("ProjectSprout.ZipUtilsTest.TestUnzip")
        try {
            // Пишем тестовые данные
            tmpZip.resolve("module.pht").writeText("(module \"pht/module\" {[name \"pht/example/zip\"]} {[vers \"1.0.0\"]})")
            tmpZip.resolve("src").createDirectories()
            tmpZip.resolve("src/main.pht").writeText("source content")
            tmpZip.resolve("plg").createDirectories()
            tmpZip.resolve("plg/main.pht").writeText("plugin content")
            tmpZip.resolve("res/txt").createDirectories()
            tmpZip.resolve("res/txt/data.txt").writeText("data content")
            tmpZip.resolve("res/empty").createDirectories()
            // Архивируем
            val zip = ZipUtils.zip(tmpZip)
            // Распакуем заголовок
            val header = ZipUtils.unzipHeader(zip)
            assertEquals("pht/example/zip", header.name)
            assertEquals("1.0.0".toVersion(), header.version)
            // Распакуем в папку
            ZipUtils.unzip(tmpUnzip, zip)
            // Читаем тестовые данные
            assertEquals("(module \"pht/module\" {[name \"pht/example/zip\"]} {[vers \"1.0.0\"]})", tmpUnzip.resolve("module.pht").readText())
            assertEquals("source content", tmpUnzip.resolve("src/main.pht").readText())
            assertEquals("plugin content", tmpUnzip.resolve("plg/main.pht").readText())
            assertEquals("data content", tmpUnzip.resolve("res/txt/data.txt").readText(), )
            assertEquals(tmpUnzip.resolve("res/empty").exists(), tmpUnzip.resolve("res/empty").isDirectory())
        } finally {
            tmpZip.deleteRecursively()
            tmpUnzip.deleteRecursively()
        }
    }
}