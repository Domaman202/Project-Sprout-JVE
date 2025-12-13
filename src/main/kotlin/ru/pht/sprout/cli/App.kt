package ru.pht.sprout.cli

import ru.pht.sprout.cli.args.ArgumentsParser
import ru.pht.sprout.cli.args.Command
import ru.pht.sprout.cli.args.CommandArgument
import ru.pht.sprout.cli.build.BuildInfo
import ru.pht.sprout.utils.fmt.FmtUtils.fmt
import ru.pht.sprout.utils.lang.SproutTranslate
import ru.pht.sprout.utils.lang.Translation
import java.util.*

object App {
    // ===== КОНСТАНТЫ ===== //
    const val VERSION = "1.0.0"
    const val AUTHOR = "DomamaN202"
    // ===== INITIAL ===== //
    val COMMANDS: Array<Command.Definition>
    // ===== RUNTIME ===== //
    val LANG = SproutTranslate.language(Locale.getDefault())
    val BUILD_INFO = BuildInfo()

    // ===== MAIN ===== //

    @JvmStatic
    fun main(args: Array<String>) {
        val command = ArgumentsParser(args, COMMANDS, LANG).findCommand()
        when (command.definition.long) {
            "help" -> printHelp(command.argument("command") as String?)
            "version" -> printVersionInfo()
            "shell" -> runShell()
        }
    }

    private fun runShell() {
        printInfo()
        TODO()
    }

    private fun printHelp(command: String?) {
        printInfo()
        println()
        println(translate("printHelp.header"))
        if (command != null) {
            println()
            printHelp(COMMANDS.find { it.long == command }!!)
        } else {
            for (command in COMMANDS) {
                println()
                printHelp(command)
            }
        }
    }

    private fun printHelp(command: Command.Definition) {
        println(translate("printHelp.command", Pair("long", command.long)))
        println(if (command.short == null) translate("printHelp.noShort") else translate("printHelp.short", Pair("short", command.short)))
        println(translate("printHelp.long", Pair("long", command.long)))
        println(if (command.description == null) translate("printHelp.noDescription") else translate("printHelp.description", Pair("description", command.description.translate(LANG))))
    }

    private fun printInfo() {
        println(translate("printInfo.header"))
        println()
        println("§sb§bf§f0      MADE      §sb§ba§f0       MADE       ".fmt)
        println("§sb§bc§f0       IN       §sb§bc§f0        BY        ".fmt)
        println("§sb§b9§f0     RUSSIA     §sb§b9§f0     QUMUQLAR     ".fmt)
        println()
        println(translate("printInfo.os",   Pair("name", System.getProperty("os.name")), Pair("version", System.getProperty("os.version"))))
        println(translate("printInfo.java", Pair("version", System.getProperty("java.version")), Pair("vendor", System.getProperty("java.vendor"))))
        println(translate("printInfo.lang", Pair("name", LANG.name)))
        println()
        printVersionInfo()
        println()
        println(translate("printInfo.build.module",     Pair("status", if (BUILD_INFO.tryParseModule()) "§f6${BUILD_INFO.moduleHeader!!.name}" else if (BUILD_INFO.moduleHeaderError == null) "§f1Не найден" else "§f1Ошибка чтения")))
        println(translate("printInfo.build.download",   Pair("download", BUILD_INFO.cachingRepository.findAllCached().size)))
        println(translate("printInfo.build.repository", Pair("repository", BUILD_INFO.repositories.size)))
    }

    private fun printVersionInfo() {
        println(translate("printInfo.project.name"))
        println(translate("printInfo.project.version",  Pair("version", VERSION)))
        println(translate("printInfo.project.author",   Pair("author", AUTHOR)))
    }

    private fun translate(key: String, vararg args: Pair<String, Any?>): String =
        SproutTranslate.translate<App>(LANG, key, *args)

    private fun translation(key: String): Translation =
        SproutTranslate.of<App>(key)

    init {
        COMMANDS = arrayOf(
            Command.Definition(
                "h",
                "help",
                arrayOf(
                    CommandArgument.Definition(
                        "command",
                        translation("cmd.help.arg0"),
                        CommandArgument.Type.VARIATION,
                        CommandArgument.Variants.of { COMMANDS.map { it.long } },
                        true
                    )
                ),
                translation("cmd.help.desc")
            ),
            Command.Definition(
                "v",
                "version",
                emptyArray(),
                translation("cmd.version.desc")
            ),
            Command.Definition(
                null,
                "shell",
                emptyArray(),
                translation("cmd.help.desc")
            )
        )
    }
}