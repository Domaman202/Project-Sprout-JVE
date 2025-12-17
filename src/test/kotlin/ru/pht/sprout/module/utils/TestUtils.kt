package ru.pht.sprout.module.utils

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

@OptIn(ExperimentalPathApi::class)
inline fun <T> useTmpDir(prefix: String = "tmp", block: (dir: Path) -> T): T {
    val tmp = Files.createTempDirectory(prefix)
    try {
        return block(tmp)
    } finally {
        tmp.deleteRecursively()
    }
}