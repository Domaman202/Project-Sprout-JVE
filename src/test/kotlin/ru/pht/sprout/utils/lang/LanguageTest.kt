package ru.pht.sprout.utils.lang

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.condition.EnabledIf
import ru.pht.sprout.utils.fmt.FmtUtils.fmt
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@EnabledIf("ru.pht.sprout.TestConfigInternal#translateTest", disabledReason = "Тест выключен конфигурацией")
class LanguageTest {
    @Test
    @DisplayName("Поиск английского перевода")
    fun languageOfEnglishTest() {
        val language = Language.of(Locale.ENGLISH)
        assertNotNull(language)
        assertEquals(language.name, "English")
        assertEquals(language.code, "en")
        assertTrue(language.translate.isNotEmpty())
    }


    @Test
    @DisplayName("Поиск итальянского перевода")
    fun languageOfItalyTest() {
        val language = Language.of(Locale.ITALY)
        assertNotNull(language)
        assertEquals(language.name, "English")
        assertEquals(language.code, "en")
        assertTrue(language.translate.isNotEmpty())
    }

    @Test
    @DisplayName("Поиск русского перевода")
    fun languageOfRussianTest() {
        assertNotNull(Language.of(Locale.forLanguageTag("ru")))
    }

    @Test
    @DisplayName("Простой перевод #0")
    fun simpleTranslateTest0() {
        val language = Language.of(Locale.ENGLISH)
        assertNotNull(language)
        assertEquals(Translation.of<LanguageTest>("simple").translate(language), "Simple translate".fmt)
    }

    @Test
    @DisplayName("Простой перевод #1")
    fun simpleTranslateTest1() {
        val language = Language.of(Locale.ENGLISH)
        assertNotNull(language)
        assertEquals(Translation.translate<LanguageTest>(language, "simple"), "Simple translate".fmt)
    }

    @Test
    @DisplayName("Перевод с подставкой значений #0")
    fun translateWithValuesTest0() {
        val language = Language.of(Locale.ENGLISH)
        assertNotNull(language)
        assertEquals(Translation.of<LanguageTest>("values").translate(language, Pair("i", 12), Pair("j", 21)), "I = 12, J = 21".fmt)
    }


    @Test
    @DisplayName("Перевод с подставкой значений #1")
    fun translateWithValuesTest1() {
        val language = Language.of(Locale.ENGLISH)
        assertNotNull(language)
        assertEquals(Translation.translate<LanguageTest>(language, "values", Pair("i", 12), Pair("j", 21)), "I = 12, J = 21".fmt)
    }

    @Test
    @DisplayName("Перевод со стилями")
    fun translateWithStyleAndColorTest() {
        val language = Language.of(Locale.ENGLISH)
        assertNotNull(language)
        assertEquals(Translation.translate<LanguageTest>(language, "styleAndColor"), "§f2Green §srAnd §sbBold".fmt)
    }

    @Test
    @DisplayName("Расширение перевода")
    fun expandTranslate() {
        Language.addResolver(LanguageTest::class.java) { LanguageTest::class.java.getResource("/sprout/test/lang/${it}_extend.json")?.readText(Charsets.UTF_8) }
        val language = Language.of(Locale.ENGLISH)
        assertNotNull(language)
        assertEquals(Translation.translate<LanguageTest>(language, "simple"), "Simple translate".fmt)
        assertEquals(Translation.translate<LanguageTest>(language, "extend"), "Extended translate".fmt)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun init() {
            Language.addResolver(LanguageTest::class.java) { LanguageTest::class.java.getResource("/sprout/test/lang/$it.json")?.readText(Charsets.UTF_8) }
        }
    }
}