package ru.pht.sprout.cli

import ru.DmN.cmd.style.FmtUtils.fmt
import ru.DmN.translate.Language
import ru.DmN.translate.TranslationKey
import ru.DmN.translate.TranslationPair
import ru.DmN.translate.exception.ITranslatedThrowable
import ru.pht.sprout.cli.args.ArgumentsParser
import ru.pht.sprout.cli.args.Command
import ru.pht.sprout.cli.args.CommandArgument
import ru.pht.sprout.cli.build.BuildSystemInfo
import ru.pht.sprout.cli.build.ProjectInfo
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
    val COMMANDS: Array<Command.Definition>
    // ===== RUNTIME ===== //
    val LANG = Language.of(Locale.getDefault())
    val BUILD_SYSTEM_INFO = BuildSystemInfo()
    val PROJECT_INFO = ProjectInfo()

    // ===== MAIN ===== //

    @JvmStatic
    fun main(args: Array<String>) {
        val command = ArgumentsParser(args, COMMANDS, LANG).findCommand()
        when (command.definition.long) {
            "help" -> printHelp(command.argument("command") as String?)
            "version" -> printAllInfo(buildSystemInfo = false, moduleInfo = false, moduleDependencyTree = false)
            "module info" -> printAllInfo(version = false, systemInfo = false, moduleDependencyTree = false)
            "module tree" -> printAllInfo(version = false, systemInfo = false)
            "shell" -> runShell()
        }
    }

    private fun runShell() {
        printAllInfo()
        TODO()
    }

    private fun printHelp(command: String?) {
        printAllInfo(buildSystemInfo = false, moduleInfo = false)
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

    private fun printAllInfo(
        logo: Boolean = true,
        version: Boolean = true,
        systemInfo: Boolean = true,
        buildSystemInfo: Boolean = true,
        moduleInfo: Boolean = true,
        moduleDependencyTree: Boolean = true,
    ) {
        if (logo) {
            println(translate("printInfo.header"))
            println()
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
            if (version) {
                println(translate("printInfo.name"))
                println(translate("printInfo.version", "version" to VERSION))
                println(translate("printInfo.author",  "author"  to AUTHOR))
                println()
            }
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
                println()
                if (moduleDependencyTree) {
                    try {
                        val builder = GraphBuilder {
                            val list = BUILD_SYSTEM_INFO.cachingRepository.find(it.name, it.version)
                            if (list.isEmpty())
                                throw ModuleNotFoundException(it.name, it.version)
                            list.first().header()
                        }
                        val printer = GraphPrinter(builder.buildCombine(PROJECT_INFO.header), true)
                        val text = printer.print()
                        println(translate("printInfo.project.tree"))
                        println()
                        println(text)
                        println()
                    } catch (e: Exception) {
                        println(translate("printInfo.project.treeError", "error" to if (e is ITranslatedThrowable<*>) e.translate(LANG) else e.message ?: e.javaClass.name))
                    }
                }
            }
        }
    }

    private fun translate(key: String, vararg args: Pair<String, Any?>): String =
        SproutTranslate.of<App>(LANG, key, *args)

    private fun translationPair(group: String): TranslationPair =
        TranslationPair(TranslationKey("${App.javaClass.name}.$group"), SproutTranslate)

    private fun tryParseModule(block: () -> Unit) {
        try {
            PROJECT_INFO.init(Path("."))
            block()
        } catch (e: Exception) {
            println(translate("printInfo.project.error", "error" to if (e is ITranslatedThrowable<*>) e.translate(LANG) else e.message ?: e.javaClass.name))
        }
    }

    init {
        COMMANDS = arrayOf(
            Command.short(
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
    }
}