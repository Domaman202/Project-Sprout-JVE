package ru.pht.sprout.cli

import ru.DmN.cmd.style.FmtUtils.fmt
import ru.DmN.translate.Language
import ru.DmN.translate.TranslationKey
import ru.DmN.translate.TranslationPair
import ru.pht.sprout.cli.args.ArgumentsParser
import ru.pht.sprout.cli.args.Command
import ru.pht.sprout.cli.args.CommandArgument
import ru.pht.sprout.cli.build.BuildInfo
import ru.pht.sprout.utils.SproutTranslate
import java.util.*

object App {
    // ===== КОНСТАНТЫ ===== //
    const val VERSION = "1.0.0"
    const val AUTHOR = "DomamaN202"
    // ===== INITIAL ===== //
    val COMMANDS: Array<Command.Definition>
    // ===== RUNTIME ===== //
    val LANG = Language.of(Locale.getDefault())
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
        println(translate("printHelp.command", "long" to command.long))
        println(
            if (command.short == null)
                translate("printHelp.noShort")
            else translate("printHelp.short", "short" to command.short)
        )
        println(translate("printHelp.long", "long" to command.long))
        println(
            if (command.description == null)
                translate("printHelp.noDescription")
            else translate("printHelp.description", "description" to command.description.translate(LANG))
        )
    }

    private fun printInfo() {
        println(translate("printInfo.header"))
        println()
        println("§sb§bf§f0      MADE      §sb§ba§f0       MADE       ".fmt)
        println("§sb§bc§f0       IN       §sb§bc§f0        BY        ".fmt)
        println("§sb§b9§f0     RUSSIA     §sb§b9§f0     QUMUQLAR     ".fmt)
        println()
        println(translate("printInfo.os",   "name"    to System.getProperty("os.name"),      "version" to System.getProperty("os.version")))
        println(translate("printInfo.java", "version" to System.getProperty("java.version"), "vendor"  to System.getProperty("java.vendor")))
        println(translate("printInfo.lang"))
        println()
        printVersionInfo()
        println()
        println(translate("printInfo.build.module",     "status"     to if (BUILD_INFO.tryParseModule()) "§f6${BUILD_INFO.moduleHeader!!.name}" else if (BUILD_INFO.moduleHeaderError == null) "§f1Не найден" else "§f1Ошибка чтения"))
        println(translate("printInfo.build.download",   "download"   to BUILD_INFO.cachingRepository.findAllCached().size))
        println(translate("printInfo.build.repository", "repository" to BUILD_INFO.repositories.size))
    }

    private fun printVersionInfo() {
        println(translate("printInfo.project.name"))
        println(translate("printInfo.project.version",  "version" to VERSION))
        println(translate("printInfo.project.author",   "author"  to AUTHOR))
    }

    private fun translate(key: String, vararg args: Pair<String, Any?>): String =
        SproutTranslate.of<App>(LANG, key, *args)

    private fun translationPair(group: String): TranslationPair =
        TranslationPair(TranslationKey("${App.javaClass.name}.$group"), SproutTranslate)

    init {
        COMMANDS = arrayOf(
            Command.Definition(
                "h",
                "help",
                arrayOf(
                    CommandArgument.Definition(
                        "command",
                        translationPair("cmd.help.arg0"),
                        CommandArgument.Type.VARIATION,
                        CommandArgument.Variants.of { COMMANDS.map { it.long } },
                        true
                    )
                ),
                translationPair("cmd.help.desc")
            ),
            Command.Definition(
                "v",
                "version",
                emptyArray(),
                translationPair("cmd.version.desc")
            ),
            Command.Definition(
                null,
                "shell",
                emptyArray(),
                translationPair("cmd.help.desc")
            )
        )
    }
}