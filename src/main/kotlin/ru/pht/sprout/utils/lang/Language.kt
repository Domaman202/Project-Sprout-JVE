package ru.pht.sprout.utils.lang

import com.github.pwittchen.kirai.library.Kirai
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.pht.sprout.utils.fmt.fmt
import java.util.*

@Serializable
class Language(
    val name: String,
    val code: String,
    val translate: MutableMap<String, String>
) {
    fun translate(key: String, vararg args: Pair<String, Any?>): String {
        val kirai = Kirai.from(this.translate[key] ?: throw RuntimeException("Перевод не найден"))
        for ((key, value) in args)
            kirai.put(key, value)
        return kirai.format().toString().fmt
    }

    companion object {
        private val CACHE: MutableMap<String, Language> = HashMap()
        private val RESOLVERS: MutableMap<Class<*>, (code: String) -> String?> = WeakHashMap()

        @Synchronized
        fun addResolver(klass: Class<*>, resolver: (code: String) -> String?): Companion {
            RESOLVERS[klass] = resolver
            CACHE.forEach { (code, lang) ->
                resolver(code)?.let {
                    lang.translate += Json.decodeFromString<Language>(it).translate
                }
            }
            return Companion
        }

        @Synchronized
        fun of(locale: Locale): Language {
            val code = locale.language
            CACHE[code]?.let { return it }
            val resolved = RESOLVERS.asSequence().map { it.value(code) }.filterNotNull().toMutableList()
            if (resolved.isEmpty()) {
                if (locale == Locale.ENGLISH)
                    throw RuntimeException("Английский перевод не найден")
                return of(Locale.ENGLISH)
            }
            val language = Json.decodeFromString<Language>(resolved.removeFirst())
            resolved.forEach { language.translate += Json.decodeFromString<Language>(it).translate }
            CACHE[code] = language
            return language
        }
    }
}