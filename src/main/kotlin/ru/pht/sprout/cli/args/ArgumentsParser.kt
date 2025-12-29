package ru.pht.sprout.cli.args

import ru.DmN.translate.Language
import ru.pht.sprout.cli.args.CommandArgument.Type.*
import ru.pht.sprout.utils.SproutTranslate

class ArgumentsParser(val commands: Array<Command.Definition>) {
    @Throws(ArgumentsParserException::class)
    fun parse(language: Language, args: Array<String>): Command {
        // Первичная проверка команды
        if (args.isEmpty())
            throw ArgumentsParserException(translateOf(language, "noCommand"))
        // Нахождение команды
        val definition = run {
            val first = args.first()
            when {
                first.startsWith("--") -> {
                    val name = first.drop(2)
                    commands@for (it in this.commands) {
                        it.split ?: if (it.long == name) return@run it else continue@commands
                        if (it.split.size <= args.size) {
                            if (it.split.first() != name)
                                continue@commands
                            for (i in 1 until it.split.size)
                                if (it.split[i] != args[i])
                                    continue@commands
                            return@run it
                        }
                    }
                    throw ArgumentsParserException(translateOf(language, "notFoundedCommand", "command" to first))
                }

                first.startsWith("-") -> {
                    val name = first.drop(1)
                    return@run this.commands.find { it.short == name } ?: throw ArgumentsParserException(translateOf(language, "notFoundedCommand", "command" to first))
                }

                else -> throw ArgumentsParserException(translateOf(language, "notCommand", "command" to first))
            }
        }
        // Парсинг аргументов
        val arguments = ArrayList<CommandArgument>()
        var argI = definition.split?.size ?: 1
        var cmdI = 0
        while (argI < args.size) {
            val cmdArg = definition.arguments[cmdI]
            try {
                arguments += CommandArgument(
                    cmdArg,
                    when (cmdArg.type) {
                        INT -> args[argI].toInt()
                        FLOAT -> args[argI].toFloat()
                        STRING -> args[argI]
                        STRING_VARARG -> {
                            val list = ArrayList<String>()
                            while (argI < args.size)
                                list += args[argI++]
                            list
                        }
                        VARIATION -> {
                            cmdArg.variants!!.parse(args[argI])
                                ?: throw ArgumentsParserException(translateOf(language, "notFoundedVariant", "variant" to args[argI], "argument" to cmdArg.displayedName(language)))
                        }
                    }
                )
            } catch (e: NumberFormatException) {
                throw ArgumentsParserException(translateOf(language, "formattingError", "argument" to cmdArg.displayedName(language)), e)
            }
            argI++
            cmdI++
        }
        // Проверка аргументов
        val definedArgs = arguments.map { it.definition.name }
        val undefinedArgs = definition.arguments.filter { !it.optional && !definedArgs.contains(it.name) }
        if (undefinedArgs.isNotEmpty())
            throw ArgumentsParserException(translateOf(language, "requiredArguments", "arguments" to undefinedArgs.joinToString(", ") { it.name }))
        //
        return Command(definition, arguments)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun translateOf(language: Language, category: String, vararg args: Pair<String, Any?>): String =
        SproutTranslate.of<ArgumentsParser>(language, category, *args)
}