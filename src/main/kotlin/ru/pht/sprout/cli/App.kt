package ru.pht.sprout.cli

import ru.DmN.cmd.args.ArgumentsParser
import ru.DmN.cmd.args.Command
import ru.DmN.cmd.args.CommandArgument
import ru.DmN.cmd.style.FmtUtils.fmt
import ru.DmN.translate.Language
import ru.DmN.translate.TranslationKey
import ru.DmN.translate.TranslationPair
import ru.DmN.translate.exception.ITranslatedThrowable
import ru.pht.sprout.cli.build.BuildSystemInfo.Companion.BuildSystemInfo
import ru.pht.sprout.cli.build.ProjectInfo
import ru.pht.sprout.cli.shell.ShellPrintWrapper
import ru.pht.sprout.module.graph.GraphBuilder
import ru.pht.sprout.module.graph.GraphPrinter
import ru.pht.sprout.utils.SproutTranslate
import ru.pht.sprout.utils.exception.ModuleNotFoundException
import java.util.*
import kotlin.io.path.Path

object App {
    // ===== КОНСТАНТЫ ===== //
    const val VERSION = "1.0.0"
    const val AUTHOR = "DomamaN202"
    // ===== INITIAL ===== //
    val ARGUMENTS_PARSER: ArgumentsParser
    // ===== RUNTIME ===== //
    val LANG = Language.of(Locale.getDefault())
    val BUILD_SYSTEM_INFO = BuildSystemInfo()
    val PROJECT_INFO = ProjectInfo()

    // ===== MAIN ===== //

    @JvmStatic
    fun main(args: Array<String>) {
        parseAndExecCommand(true, args)
    }

    private fun parseAndExecCommand(default: Boolean, args: Array<String>) {
        val command = ARGUMENTS_PARSER.parse(LANG, args)
        when (command.definition.long) {
            "help" -> printHelp(default = default, command = command.argument("command") as String?)
            "version" -> printAllInfo(logo = default, buildSystemInfo = false, moduleInfo = false, moduleDependencyTree = false)
            "module info" -> printAllInfo(logo = default, systemInfo = false, version = false, buildSystemInfo = default, moduleDependencyTree = false)
            "module tree" -> printAllInfo(logo = default, systemInfo = false, version = false, buildSystemInfo = default, moduleInfo = default)
            "shell" -> runShell(default = default)
        }
    }

    private fun runShell(default: Boolean) {
        printAllInfo(logo = default, moduleDependencyTree = false)
        ShellPrintWrapper.setSystemOut()
        while (true) {
            val line = ShellPrintWrapper.readln()
            if (line.startsWith('/')) {
                if (line == "/exit") {
                    println(translate("shell.onExit"))
                    break
                }
                println()
                parseAndExecCommand(default = false, args = ("--" + line.substring(1)).split(' ').toTypedArray())
            } else {
                println(translate("shell.sproutNotRealized"))
            }
        }
        ShellPrintWrapper.restoreSystemOut()
    }

    private fun printHelp(default: Boolean, command: String?) {
        printAllInfo(logo = default, buildSystemInfo = false, moduleInfo = false, moduleDependencyTree = false)
        println(translate("printHelp.header"))
        if (command != null) {
            println()
            printHelp(ARGUMENTS_PARSER.commands.find { it.long == command }!!)
        } else {
            for (command in ARGUMENTS_PARSER.commands) {
                println()
                printHelp(command)
            }
        }
        println()
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
            else translate("printHelp.description", "description" to command.description!!.translate(LANG))
        )
    }

    private fun printAllInfo(
        logo: Boolean = true,
        systemInfo: Boolean = true,
        version: Boolean = true,
        buildSystemInfo: Boolean = true,
        moduleInfo: Boolean = true,
        moduleDependencyTree: Boolean = true,
    ) {
        if (logo or version) {
            println(translate("printInfo.header"))
            println()
        }
        if (logo) {
            println("§sb§bf§f0      MADE      §sb§ba§f0       MADE       ".fmt)
            println("§sb§bc§f0       IN       §sb§bc§f0        BY        ".fmt)
            println("§sb§b9§f0     RUSSIA     §sb§b9§f0     QUMUQLAR     ".fmt)
            println()
            if (systemInfo) {
                println(translate("printInfo.os",   "name"    to System.getProperty("os.name"),      "version" to System.getProperty("os.version")))
                println(translate("printInfo.java", "version" to System.getProperty("java.version"), "vendor"  to System.getProperty("java.vendor")))
                println(translate("printInfo.lang"))
                println()
            }
        }
        if (version) {
            println(translate("printInfo.name"))
            println(translate("printInfo.version", "version" to VERSION))
            println(translate("printInfo.author",  "author"  to AUTHOR))
            println()
        }
        if (buildSystemInfo) {
            println(translate("printInfo.buildSystem.header"))
            println()
            println(translate("printInfo.buildSystem.repository", "count"      to BUILD_SYSTEM_INFO.repositories.size))
            println(translate("printInfo.buildSystem.cached",     "download"   to BUILD_SYSTEM_INFO.cachingRepository.findAllCached().size))
            println(translate("printInfo.buildSystem.caching",    "repository" to BUILD_SYSTEM_INFO.cachingRepository.javaClass.simpleName))
            println()
        }
        if (moduleInfo) {
            println(translate("printInfo.project.header"))
            println()
            tryParseModule {
                println(translate("printInfo.project.name",    "name"    to PROJECT_INFO.header.name))
                println(translate("printInfo.project.version", "version" to PROJECT_INFO.header.version))
            }
            println()
        }
        if (moduleDependencyTree) {
            println(translate("printInfo.project.tree"))
            println()
            tryParseModule {
                try {
                    val builder = GraphBuilder {
                        val list = BUILD_SYSTEM_INFO.cachingRepository.find(it.name, it.version)
                        if (list.isEmpty())
                            throw ModuleNotFoundException(it.name, it.version)
                        list.first().header()
                    }
                    val printer = GraphPrinter(builder.buildCombine(PROJECT_INFO.header), true)
                    val text = printer.print()
                    println(text)
                } catch (e: Exception) {
                    println(translate("printInfo.project.treeError", "error" to if (e is ITranslatedThrowable<*>) e.translate(LANG) else e.message ?: e.javaClass.name))
                }
            }
            println()
        }
    }

    private fun translate(key: String, vararg args: Pair<String, Any?>): String =
        SproutTranslate.of<App>(LANG, key, *args)

    private fun translationPair(group: String): TranslationPair =
        TranslationPair(TranslationKey("${App.javaClass.name}.$group"), SproutTranslate)

    private fun tryParseModule(block: () -> Unit) {
        try {
            if (PROJECT_INFO.status != ProjectInfo.Status.INITIALIZED)
                PROJECT_INFO.init(Path("."))
            block()
        } catch (e: Exception) {
            println(translate("printInfo.project.error", "error" to if (e is ITranslatedThrowable<*>) e.translate(LANG) else e.message ?: e.javaClass.name))
        }
    }

    init {
        ARGUMENTS_PARSER = ArgumentsParser(
            arrayOf(
                Command.short(
                    "h",
                    "help",
                    arrayOf(
                        CommandArgument.Definition(
                            "command",
                            translationPair("cmd.help.arg0"),
                            CommandArgument.Type.VARIATION,
                            CommandArgument.Variants.of { ARGUMENTS_PARSER.commands.map { it.long } },
                            true
                        )
                    ),
                    translationPair("cmd.help.desc")
                ),
                Command.short(
                    "v",
                    "version",
                    emptyArray(),
                    translationPair("cmd.version.desc")
                ),
                Command.long(
                    "module info",
                    emptyArray(),
                    translationPair("cmd.info.desc")
                ),
                Command.long(
                    "module tree",
                    emptyArray(),
                    translationPair("cmd.tree.desc")
                ),
                Command.long(
                    "shell",
                    emptyArray(),
                    translationPair("cmd.shell.desc")
                )
            )
        )
    }
}