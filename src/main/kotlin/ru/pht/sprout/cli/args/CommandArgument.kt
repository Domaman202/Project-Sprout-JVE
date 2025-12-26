package ru.pht.sprout.cli.args

import ru.DmN.translate.TranslationKey
import ru.DmN.translate.TranslationPair
import ru.pht.sprout.utils.exception.SproutIllegalArgumentException
import java.util.function.Supplier

/**
 * Аргумент команды.
 *
 * @param definition Определение.
 * @param value Значение.
 */
class CommandArgument(
    val definition: Definition,
    val value: Any?
) {
    /**
     * Определение аргумента команды.
     *
     * @param name Название.
     * @param displayedName Отображаемое имя.
     * @param type Тип.
     * @param variants Варианты.
     * @param optional Опциональность.
     */
    class Definition(
        val name: String,
        val displayedName: TranslationPair,
        val type: Type,
        val variants: Variants?,
        val optional: Boolean
    )

    /**
     * Тип аргумента.
     */
    enum class Type {
        INT,
        FLOAT,
        STRING,
        STRING_VARARG,
        VARIATION
    }

    /**
     * Вариант аргумента.
     *
     * @param stringsV Строковые варианты.
     * @param supplierV Поставщик строковых вариантов.
     * @param enumsV Перечисление вариантов.
     */
    class Variants private constructor(
        val stringsV: List<String>?,
        val supplierV: Supplier<List<String>>?,
        val enumsV: Array<Enum<*>>?,
    ) {
        /// Вариант содержит строки?
        val strings: Boolean get() = this.stringsV != null
        /// Вариант лениво получает строки?
        val supplier: Boolean get() = this.supplierV != null
        /// Вариант содержит перечисления?
        val enums: Boolean get() = this.enumsV != null

        /**
         * Парсинг в нужный тип варианта.
         *
         * @param value Значение варианта.
         * @return Вариант.
         */
        fun parse(value: String): Any? {
            if (this.stringsV != null)
                return if (this.stringsV.contains(value)) value else null
            if (this.supplierV != null)
                return if (this.supplierV.get().contains(value)) value else null
            if (this.enumsV != null)
                return this.enumsV.find { it.name.equals(value, ignoreCase = true) }
            throw Error("Unreachable code")
        }

        /**
         * Парсинг строкового варианта.
         *
         * @param value Значение варианта.
         * @return Вариант.
         * @throws SproutIllegalArgumentException Вызван неверный метод парсинга.
         */
        @Throws(SproutIllegalArgumentException::class)
        fun parseString(value: String): String? {
            if (this.stringsV != null)
                return if (this.stringsV.contains(value)) value else null
            if (this.supplierV != null)
                return if (this.supplierV.get().contains(value)) value else null
            throw SproutIllegalArgumentException(TranslationKey.of<CommandArgument>("invalidParsing"))
        }

        /**
         * Парсинг перечисления варианта.
         *
         * @param value Значение варианта.
         * @return Вариант.
         * @throws SproutIllegalArgumentException Вызван неверный метод парсинга.
         */
        @Throws(SproutIllegalArgumentException::class)
        fun parseEnum(value: String): Enum<*>? {
            if (this.enumsV != null)
                return this.enumsV.find { it.name.equals(value, ignoreCase = true) }
            throw SproutIllegalArgumentException(TranslationKey.of<CommandArgument>("invalidParsing"))
        }

        companion object {
            fun of(strings: List<String>): Variants =
                Variants(strings, null, null)
            fun of(supplier: Supplier<List<String>>): Variants =
                Variants(null, supplier, null)
            fun of(enums: Array<Enum<*>>): Variants =
                Variants(null, null, enums)
        }
    }
}