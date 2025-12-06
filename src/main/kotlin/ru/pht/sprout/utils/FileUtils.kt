package ru.pht.sprout.utils

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteIfExists

object FileUtils {
    inline fun <T> createAndUseTmpDir(prefix: String = "tmp", block: (tmp: Path) -> T): T {
        val dir = Files.createTempDirectory(prefix)
        try {
            val result = block(dir)
            dir.deleteIfExists()
            return result
        } catch (t: Throwable) {
            dir.deleteIfExists()
            throw t
        }
    }
}