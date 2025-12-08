package ru.pht.sprout.cli.args

import ru.pht.sprout.cli.App
import ru.pht.sprout.cli.args.CommandArgument.Type.*
import ru.pht.sprout.cli.lang.Language

class ArgumentsParser(
    val args: Array<String>,
    val commands: Array<Command.Definition>,
    val lang: Language = App.LANG
) {
    fun findCommand(): Command {
        // Первичная проверка команды
        if (this.args.isEmpty())
            throw ArgumentsParserException("Команда отсутвует")
        val command = this.args.first()
        if (!command.startsWith('-'))
            throw ArgumentsParserException("'${command}' не является командой")
        // Нахождение команды
        val commandName = command.drop(1)
        val definition = this.commands.find { it.short == commandName || it.long == commandName }
            ?: throw ArgumentsParserException("Команда '${command} не найдена")
        // Парсинг аргументов
        val arguments = ArrayList<CommandArgument>()
        var argI = 1
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
                                ?: throw ArgumentsParserException("Неопознанный вариант '${this.args[argI]}' аргумента '${cmdArg.displayedName.translate(lang)}'")
                        }
                    }
                )
            } catch (e: NumberFormatException) {
                throw ArgumentsParserException("Ошибка форматирования аргумента '${cmdArg.displayedName.translate(lang)}'", e)
            }
            argI++
            cmdI++
        }
        // Проверка аргументов

        //
        return Command(definition, arguments)
    }
}