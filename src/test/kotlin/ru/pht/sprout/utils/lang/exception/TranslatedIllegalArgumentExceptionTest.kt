package ru.pht.sprout.utils.lang.exception

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import ru.pht.sprout.utils.fmt.FmtUtils.fmt
import ru.pht.sprout.utils.lang.Language
import ru.pht.sprout.utils.lang.Translation
import kotlin.test.Test
import kotlin.test.assertEquals

@EnabledIf("ru.pht.sprout.TestConfigInternal#translateTest", disabledReason = "Тест выключен конфигурацией")
class TranslatedIllegalArgumentExceptionTest {
    @Test
    @DisplayName("Перевод с помощью translate")
    fun translateTest() {
        assertEquals(
            TranslatedException(Translation.of<TranslatedExceptionTest>("basic")).translate(Language.ENGLISH),
            "Exception text".fmt
        )
    }

    @Test
    @DisplayName("Перевод с аргументами помощью translate")
    fun translateWithValuesTest() {
        assertEquals(
            TranslatedIllegalArgumentException(
                Translation.of<TranslatedExceptionTest>("value"),
                Pair("value", "something value"),
            ).translate(Language.ENGLISH),
            "Exception text with something value".fmt
        )
    }

    @Test
    @DisplayName("Перевод с помощью message")
    fun translateMessageTest() {
        assertEquals(
            TranslatedIllegalArgumentException(Translation.of<TranslatedExceptionTest>("basic")).message,
            "Exception text".fmt
        )
    }

    @Test
    @DisplayName("Перевод с аргументами помощью message")
    fun translateMessageWithValuesTest() {
        assertEquals(
            TranslatedIllegalArgumentException(
                Translation.of<TranslatedExceptionTest>("value"),
                Pair("value", "something value"),
            ).message,
            "Exception text with something value".fmt
        )
    }

    @Test
    @DisplayName("Конструктор с message")
    fun messageCtorTest() {
        val exception = TranslatedIllegalArgumentException("Message", Translation.of<TranslatedExceptionTest>("basic"))
        assertEquals(exception.message, "Message")
        assertEquals(exception.translate(Language.ENGLISH), "Exception text".fmt)
    }


    @Test
    @DisplayName("Конструктор с message и cause")
    fun messageAndCauseCtorTest() {
        val exception = TranslatedIllegalArgumentException("Message", Exception("Cause"), Translation.of<TranslatedExceptionTest>("basic"))
        assertEquals(exception.message, "Message")
        assertEquals(exception.cause?.message, "Cause")
        assertEquals(exception.translate(Language.ENGLISH), "Exception text".fmt)
    }

    @Test
    @DisplayName("Конструктор с cause")
    fun causeCtorTest() {
        val exception = TranslatedIllegalArgumentException(Exception("Cause"), Translation.of<TranslatedExceptionTest>("basic"))
        assertEquals(exception.message, "java.lang.Exception: Cause")
        assertEquals(exception.cause?.message, "Cause")
        assertEquals(exception.translate(Language.ENGLISH), "Exception text".fmt)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun init() {
            Language.addResolver(TranslatedIllegalArgumentExceptionTest::class.java) { TranslatedIllegalArgumentExceptionTest::class.java.getResource("/sprout/test/lang/$it.json")?.readText(Charsets.UTF_8) }
        }
    }
}