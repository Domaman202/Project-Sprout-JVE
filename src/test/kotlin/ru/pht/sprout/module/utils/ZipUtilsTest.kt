package ru.pht.sprout.module.utils

import io.github.z4kn4fein.semver.Version
import org.junit.jupiter.api.DisplayName
import java.io.ByteArrayInputStream
import java.nio.file.Files
import kotlin.io.path.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ZipUtilsTest {
    @OptIn(ExperimentalPathApi::class)
    @Test
    @DisplayName("Проверка всех функций сразу")
    fun test() {
        val tmpZip = Files.createTempDirectory("ProjectSprout.ZipUtils.TestZip")
        val tmpUnzip = Files.createTempDirectory("ProjectSprout.ZipUtils.TestUnzip")
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
            val header = ZipUtils.unzipHeader(ByteArrayInputStream(zip))
            assertEquals(header.name, "pht/example/zip")
            assertEquals(header.version, Version.parse("1.0.0"))
            // Распакуем в папку
            ZipUtils.unzip(tmpUnzip, zip)
            // Читаем тестовые данные
            assertEquals(tmpUnzip.resolve("module.pht").readText(), "(module \"pht/module\" {[name \"pht/example/zip\"]} {[vers \"1.0.0\"]})")
            assertEquals(tmpUnzip.resolve("src/main.pht").readText(), "source content")
            assertEquals(tmpUnzip.resolve("plg/main.pht").readText(), "plugin content")
            assertEquals(tmpUnzip.resolve("res/txt/data.txt").readText(), "data content")
            assertEquals(tmpUnzip.resolve("res/empty").exists(), tmpUnzip.resolve("res/empty").isDirectory())
        } finally {
            tmpZip.deleteRecursively()
            tmpUnzip.deleteRecursively()
        }
    }
}