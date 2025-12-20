package ru.pht.sprout.cli.args

import ru.DmN.translate.TranslationPair
import java.util.function.Supplier

class CommandArgument(
    val definition: Definition,
    val value: Any?
) {
    class Definition(
        val name: String,
        val displayedName: TranslationPair,
        val type: Type,
        val variants: Variants?,
        val optional: Boolean
    )

    enum class Type {
        INT,
        FLOAT,
        STRING,
        STRING_VARARG,
        VARIATION
    }

    class Variants private constructor(
        val stringsV: List<String>?,
        val supplierV: Supplier<List<String>>?,
        val enumsV: Array<Enum<*>>?,
    ) {
        val strings: Boolean get() = this.stringsV != null
        val supplier: Boolean get() = this.supplierV != null
        val enums: Boolean get() = this.enumsV != null

        fun parse(value: String): Any? {
            if (this.stringsV != null)
                return if (this.stringsV.contains(value)) value else null
            if (this.supplierV != null)
                return if (this.supplierV.get().contains(value)) value else null
            if (this.enumsV != null)
                return this.enumsV.find { it.name.equals(value, ignoreCase = true) }
            throw Error("Unreachable code")
        }

        fun parseString(value: String): String? {
            if (this.stringsV != null)
                return if (this.stringsV.contains(value)) value else null
            if (this.supplierV != null)
                return if (this.supplierV.get().contains(value)) value else null
            throw RuntimeException("Invoked invalid parse method")
        }

        fun parseEnum(value: String): Enum<*>? {
            if (this.enumsV != null)
                return this.enumsV.find { it.name.equals(value, ignoreCase = true) }
            throw RuntimeException("Invoked invalid parse method")
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