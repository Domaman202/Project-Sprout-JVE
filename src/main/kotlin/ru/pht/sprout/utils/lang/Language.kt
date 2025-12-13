package ru.pht.sprout.utils.lang

import com.github.pwittchen.kirai.library.Kirai
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.pht.sprout.utils.fmt.FmtUtils.fmt
import java.util.*

/**
 * Язык.
 *
 * @param name Национальное название языка.
 * @param code Национальный код языка.
 * @param translate Слова перевода.
 */
@Serializable
class Language(
    val name: String,
    val code: String,
    val translate: MutableMap<String, String>
) {
    /**
     * Перевод с форматированием.
     *
     * @param key Ключ перевода.
     * @param args Аргументы форматирования.
     * @return Результат перевода и форматирования.
     */
    fun translate(key: String, vararg args: Pair<String, Any?>): String {
        val kirai = Kirai.from(this.translate[key] ?: throw RuntimeException("Перевод '${key}' не найден"))
        for ((key, value) in args)
            kirai.put(key, value)
        return kirai.format().toString().fmt
    }

    companion object {
        private val CACHE: MutableMap<String, Language> = HashMap()
        private val RESOLVERS: MutableMap<Class<*>, (code: String) -> String?> = WeakHashMap()

        /**
         * Добавления запросчика переводов.
         *
         * @param klass Класс к которому будет привязка.
         * @param resolver Запросчик.
         */
        @Synchronized
        fun addResolver(klass: Class<*>, resolver: (code: String) -> String?) {
            RESOLVERS[klass] = resolver
            CACHE.forEach { (code, lang) ->
                resolver(code)?.let {
                    lang.translate += Json.decodeFromString<Language>(it).translate
                }
            }
        }

        /**
         * Получение языка из локализации.
         *
         * @param locale Локализация.
         * @return Язык.
         */
        @Synchronized
        fun of(locale: Locale): Language {
            val code = locale.language
            CACHE[code]?.let { return it }
            val resolved = RESOLVERS.asSequence().mapNotNull { it.value(code) }.toMutableList()
            if (resolved.isEmpty())
                return of(Locale.ENGLISH)
            val language = Json.decodeFromString<Language>(resolved.removeFirst())
            resolved.forEach { language.translate += Json.decodeFromString<Language>(it).translate }
            CACHE[code] = language
            return language
        }

        init {
            this.CACHE["en"] = Language("English", "en", HashMap())
        }
    }
}