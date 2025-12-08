package ru.pht.sprout.cli.lang

import com.github.pwittchen.kirai.library.Kirai
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.pht.sprout.cli.fmt.fmt
import java.util.*

@Serializable
class Language(
    val name: String,
    val code: String,
    private val translate: Map<String, String>
) {
    fun translate(key: String, vararg args: Pair<String, Any?>): String {
        val kirai = Kirai.from(this.translate[key] ?: throw RuntimeException("Перевод не найден"))
        for ((key, value) in args)
            kirai.put(key, value)
        return kirai.format().toString().fmt
    }

    fun translateWithoutFormatting(key: String): String? =
        this.translate[key]

    companion object {
        private val CACHE: MutableMap<String, Language> = HashMap()

        @Synchronized
        fun of(locale: Locale): Language {
            CACHE[locale.language]?.let { return it }
            val stream = Language::class.java.classLoader.getResourceAsStream(locale.language + ".json")
                ?: if (locale == Locale.ENGLISH)
                    throw RuntimeException("Английский перевод не найден")
                else return of(Locale.ENGLISH)
            val lang = Json.decodeFromString<Language>(stream.readBytes().toString(Charsets.UTF_8))
            CACHE[locale.language] = lang
            return lang
        }
    }
}