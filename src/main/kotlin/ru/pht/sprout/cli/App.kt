package ru.pht.sprout.cli

import ru.pht.sprout.cli.args.Command
import ru.pht.sprout.cli.args.CommandArgument
import ru.pht.sprout.cli.build.BuildSystem
import ru.pht.sprout.cli.fmt.fmt
import ru.pht.sprout.cli.lang.Language
import ru.pht.sprout.cli.lang.Translation
import java.util.*

object App {
    // ===== КОНСТАНТЫ ===== //
    const val VERSION = "1.0.0"
    const val AUTHOR = "DomamaN202"
    // ===== INITIAL ===== //
    val COMMANDS: Array<Command.Definition>
    // ===== RUNTIME ===== //
    val LANG = Language.of(Locale.getDefault())
    val BUILD_SYSTEM = BuildSystem()

    // ===== API ===== //

    fun translate(key: String, vararg args: Pair<String, Any?>): String =
        LANG.translate(key, *args)

    // ===== MAIN ===== //

    @JvmStatic
    fun main(args: Array<String>) {
//        val command = ArgumentsParser(args, COMMANDS, LANG).findCommand()
        printInfo()
    }

    private fun printInfo() {
        println("§sb§bf§f0      MADE      §sb§ba§f0       MADE       ".fmt)
        println("§sb§bc§f0       IN       §sb§bc§f0        BY        ".fmt)
        println("§sb§b9§f0     RUSSIA     §sb§b9§f0     QUMUQLAR     ".fmt)
        println()
        println(translate("app.printInfo.os",   Pair("name", System.getProperty("os.name")), Pair("version", System.getProperty("os.version"))))
        println(translate("app.printInfo.java", Pair("version", System.getProperty("java.version")), Pair("vendor", System.getProperty("java.vendor"))))
        println(translate("app.printInfo.lang", Pair("name", LANG.name)))
        println()
        println(translate("app.printInfo.project.name"))
        println(translate("app.printInfo.project.version",  Pair("version", VERSION)))
        println(translate("app.printInfo.project.author",   Pair("author", AUTHOR)))
        println()
        println(translate("app.printInfo.build.module",     Pair("status", if (BUILD_SYSTEM.tryParseModule()) "§f6${BUILD_SYSTEM.moduleHeader!!.name}" else if (BUILD_SYSTEM.moduleHeaderError == null) "§f1Не найден" else "§f1Ошибка чтения")))
        println(translate("app.printInfo.build.download",   Pair("download", BUILD_SYSTEM.cachingRepository.findAllCached().size)))
        println(translate("app.printInfo.build.repository", Pair("repository", BUILD_SYSTEM.repositories.size)))
    }

    init {
        COMMANDS = arrayOf(
            Command.Definition(
                "h",
                "help",
                arrayOf(
                    CommandArgument.Definition(
                        "command",
                        Translation("cmd.help.arg0"),
                        CommandArgument.Type.VARIATION,
                        CommandArgument.Variants.of { COMMANDS.map { it.long } },
                        true
                    )
                ),
                Translation("cmd.help.desc")
            )
        )
    }
}