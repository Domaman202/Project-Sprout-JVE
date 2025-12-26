package ru.pht.sprout.cli.args

import ru.DmN.translate.Language
import ru.pht.sprout.cli.args.CommandArgument.Type.*
import ru.pht.sprout.utils.SproutTranslate

class ArgumentsParser(
    val args: Array<String>,
    val commands: Array<Command.Definition>,
    val lang: Language
) {
    @Throws(ArgumentsParserException::class)
    fun findCommand(): Command {
        // Первичная проверка команды
        if (this.args.isEmpty())
            throw ArgumentsParserException(SproutTranslate.of<ArgumentsParser>(lang, "noCommand"))
        // Нахождение команды
        val definition = run {
            val first = this.args.first()
            when {
                first.startsWith("--") -> {
                    val name = first.drop(2)
                    commands@for (it in this.commands) {
                        it.split ?: if (it.long == name) return@run it else continue@commands
                        if (it.split.size <= this.args.size) {
                            if (it.split.first() != name)
                                continue@commands
                            for (i in 1 until it.split.size)
                                if (it.split[i] != this.args[i])
                                    continue@commands
                            return@run it
                        }
                    }
                    throw ArgumentsParserException(SproutTranslate.of<ArgumentsParser>(lang, "notFoundedCommand", "command" to first))
                }

                first.startsWith("-") -> {
                    val name = first.drop(1)
                    return@run this.commands.find { it.short == name } ?: throw ArgumentsParserException(SproutTranslate.of<ArgumentsParser>(lang, "notFoundedCommand", "command" to first))
                }

                else -> throw ArgumentsParserException(SproutTranslate.of<ArgumentsParser>(lang, "notCommand", "command" to first))
            }
        }
        // Парсинг аргументов
        val arguments = ArrayList<CommandArgument>()
        var argI = definition.split?.size ?: 1
        var cmdI = 0
        while (argI < this.args.size) {
            val cmdArg = definition.arguments[cmdI]
            try {
                arguments += CommandArgument(
                    cmdArg,
                    when (cmdArg.type) {
                        INT -> this.args[argI].toInt()
                        FLOAT -> this.args[argI].toFloat()
                        STRING -> this.args[argI]
                        STRING_VARARG -> {
                            val list = ArrayList<String>()
                            while (argI < this.args.size)
                                list += this.args[argI++]
                            list
                        }
                        VARIATION -> {
                            cmdArg.variants!!.parse(this.args[argI])
                                ?: throw ArgumentsParserException(SproutTranslate.of<ArgumentsParser>(lang, "notFoundedVariant", "variant" to this.args[argI], "argument" to cmdArg.displayedName.translate(lang)))
                        }
                    }
                )
            } catch (e: NumberFormatException) {
                throw ArgumentsParserException(SproutTranslate.of<ArgumentsParser>(lang, "formattingError", "argument" to cmdArg.displayedName.translate(lang)), e)
            }
            argI++
            cmdI++
        }
        // Проверка аргументов
        val definedArgs = arguments.map { it.definition.name }
        val undefinedArgs = definition.arguments.filter { !it.optional && !definedArgs.contains(it.name) }
        if (undefinedArgs.isNotEmpty())
            throw ArgumentsParserException(SproutTranslate.of<ArgumentsParser>(lang, "requiredArguments", "arguments" to undefinedArgs.joinToString(", ") { it.name }))
        //
        return Command(definition, arguments)
    }
}