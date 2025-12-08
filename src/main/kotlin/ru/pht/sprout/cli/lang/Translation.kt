package ru.pht.sprout.cli.lang

import kotlinx.serialization.Serializable
import ru.pht.sprout.cli.App

@JvmInline
@Serializable
value class Translation(val key: String) {
    fun translate(lang: Language = App.LANG): String =
        lang.translate(this.key)
    fun translateWithoutFormatting(lang: Language = App.LANG): String? =
        lang.translateWithoutFormatting(this.key)
}