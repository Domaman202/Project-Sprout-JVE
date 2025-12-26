package ru.pht.sprout.cli.args

import ru.DmN.translate.TranslationKey
import ru.DmN.translate.TranslationPair
import ru.pht.sprout.cli.args.Command.Companion.long
import ru.pht.sprout.cli.args.Command.Companion.short
import ru.pht.sprout.utils.exception.SproutIllegalArgumentException

/**
 * Команда.
 *
 * @param definition Определение.
 * @param arguments Аргументы.
 */
class Command(
    val definition: Definition,
    val arguments: List<CommandArgument>
) {
    /**
     * Получение значения аргументы команды.
     *
     * @param name Название аргумента.
     * @return Значение аргументы.
     */
    fun argument(name: String): Any? =
        this.arguments.find { it.definition.name == name }?.value

    /**
     * Определение команды.
     *
     * @param short Короткий вариант.
     * @param long Длинный вариант.
     * @param split Раздельный вариант.
     * @param arguments Аргументы.
     * @param description Описание.
     */
    class Definition internal constructor(
        val short: String?,
        val long: String,
        val split: List<String>?,
        val arguments: Array<CommandArgument.Definition>,
        val description: TranslationPair?
    )

    companion object {
        /**
         * Короткая команда.
         * Не может разбиваться на части.
         *
         * @param short Короткий вариант.
         * @param long Длинный вариант.
         * @param arguments Аргументы.
         * @param description Описание.
         * @return Определение команды.
         * @throws SproutIllegalArgumentException Длинный вариант содержит разделение.
         */
        @Throws(SproutIllegalArgumentException::class)
        fun short(
            short: String,
            long: String,
            arguments: Array<CommandArgument.Definition>,
            description: TranslationPair?
        ): Definition {
            if (long.contains(' '))
                throw SproutIllegalArgumentException(TranslationKey.of<Command>("splitNotAllowed"))
            return Definition(short, long, null, arguments, description)
        }

        /**
         * Длинная команда.
         * Может разбиваться на части.
         *
         * @param long Длинный вариант.
         * @param arguments Аргументы.
         * @param description Описание.
         * @return Определение команды.
         */
        fun long(
            long: String,
            arguments: Array<CommandArgument.Definition>,
            description: TranslationPair?
        ): Definition =
            if (long.contains(' '))
                Definition(null, long, long.split(' '), arguments, description)
            else Definition(null, long, null, arguments, description)
    }
}