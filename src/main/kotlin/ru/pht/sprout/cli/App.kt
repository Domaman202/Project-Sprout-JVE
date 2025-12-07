package ru.pht.sprout.cli

import io.github.z4kn4fein.semver.constraints.toConstraint
import ru.pht.sprout.build.BuildSystem
import ru.pht.sprout.cli.utils.fmt
import java.util.*

object App {
    // ===== КОНСТАНТЫ ===== //
    const val VERSION = "1.0.0"
    const val AUTHOR = "DomamaN202"
    // ===== Runtime ===== //
    val BUILD_SYSTEM = BuildSystem()

    @JvmStatic
    fun main(args: Array<String>) {
        printInfo()
    }

    private fun printInfo(locale: Locale = Locale.getDefault()) {
        println("§sb§bf§f0      MADE      §sb§ba§f0       MADE       ".fmt)
        println("§sb§bc§f0       IN       §sb§bc§f0        BY        ".fmt)
        println("§sb§b9§f0     RUSSIA     §sb§b9§f0     QUMUQLAR     ".fmt)
        println()
        when (locale.language.lowercase()) {
            "ru", "ua", "by" -> {
                println("§sb§f7Система:   §f3${System.getProperty("os.name")} ${System.getProperty("os.version")}".fmt)
                println("§sb§f7Запуск:    §f3Java ${System.getProperty("java.version")} ${System.getProperty("java.vendor")}".fmt)
                println("§sb§f7Язык:      §f3Русский".fmt)
                println()
                println("§sb§f7Проект:    §f4Росток".fmt)
                println("§sb§f7Версия:    §f4${VERSION}".fmt)
                println("§sb§f7Автор:     §f4${AUTHOR}".fmt)
                println()
                println("§sb§f7Модуль:    §f1Не найден".fmt)
                println("§sb§f7Загружено: §f6${BUILD_SYSTEM.cachingRepository.findAllCached().size}".fmt)
                println("§sb§f7Источники: §f6${BUILD_SYSTEM.repositories.size}".fmt)
            }
            else -> {
                println("§sb§f7System:     §f3${System.getProperty("os.name")} ${System.getProperty("os.version")}".fmt)
                println("§sb§f7Runtime:    §f3Java ${System.getProperty("java.version")} ${System.getProperty("java.vendor")}".fmt)
                println("§sb§f7Lang:       §f3English".fmt)
                println()
                println("§sb§f7Project:    §f4Sprout".fmt)
                println("§sb§f7Version:    §f4${VERSION}".fmt)
                println("§sb§f7Author:     §f4${AUTHOR}".fmt)
                println()
                println("§sb§f7Module:     §f1Not found".fmt)
                println("§sb§f7Download:   §f6${BUILD_SYSTEM.cachingRepository.findAllCached().size}".fmt)
                println("§sb§f7Repository: §f6${BUILD_SYSTEM.repositories.size}".fmt)
            }
        }
    }
}