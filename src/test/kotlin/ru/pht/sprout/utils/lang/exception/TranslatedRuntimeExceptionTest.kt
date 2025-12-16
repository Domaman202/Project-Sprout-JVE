package ru.pht.sprout.utils.lang.exception

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import ru.pht.sprout.utils.fmt.FmtUtils.fmt
import ru.pht.sprout.utils.lang.Language
import ru.pht.sprout.utils.lang.Translation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@EnabledIf("ru.pht.sprout.TestConfigInternal#translateTest", disabledReason = "Тест выключен конфигурацией")
class TranslatedRuntimeExceptionTest {
    @Test
    @DisplayName("Перевод с помощью translate")
    fun translateTest() {
        assertEquals(
            TranslatedRuntimeException(Translation.of<TranslatedExceptionTest>("basic")).translate(
                Language.ENGLISH),
            "Exception text".fmt
        )
    }

    @Test
    @DisplayName("Перевод с аргументами помощью translate")
    fun translateWithValuesTest() {
        assertEquals(
            TranslatedRuntimeException(
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
            TranslatedRuntimeException(Translation.of<TranslatedExceptionTest>("basic")).message,
            "Exception text".fmt
        )
    }

    @Test
    @DisplayName("Перевод с аргументами помощью message")
    fun translateMessageWithValuesTest() {
        assertEquals(
            TranslatedRuntimeException(
                Translation.of<TranslatedExceptionTest>("value"),
                Pair("value", "something value"),
            ).message,
            "Exception text with something value".fmt
        )
    }

    @Test
    @DisplayName("Конструктор с message")
    fun messageCtorTest() {
        val exception = TranslatedRuntimeException("Message", Translation.of<TranslatedExceptionTest>("basic"))
        assertEquals(exception.message, "Message")
        assertEquals(exception.translate(Language.ENGLISH), "Exception text".fmt)
    }


    @Test
    @DisplayName("Конструктор с message и cause")
    fun messageAndCauseCtorTest() {
        val exception = TranslatedRuntimeException(
            "Message",
            Exception("Cause"),
            Translation.of<TranslatedExceptionTest>("basic")
        )
        assertEquals(exception.message, "Message")
        assertEquals(exception.cause?.message, "Cause")
        assertEquals(exception.translate(Language.ENGLISH), "Exception text".fmt)
    }

    @Test
    @DisplayName("Конструктор с cause")
    fun causeCtorTest() {
        val exception =
            TranslatedRuntimeException(Exception("Cause"), Translation.of<TranslatedExceptionTest>("basic"))
        assertEquals(exception.message, "java.lang.Exception: Cause")
        assertEquals(exception.cause?.message, "Cause")
        assertEquals(exception.translate(Language.ENGLISH), "Exception text".fmt)
    }

    @Test
    @DisplayName("Полный конструктор")
    fun fullCtorTest() {
        val exception = object : TranslatedRuntimeException(
            message = "Message",
            cause = Exception("Cause"),
            enableSuppression = false,
            writableStackTrace = false,
            translation = Translation.of<TranslatedExceptionTest>("value"),
            args = arrayOf(Pair("value", "something value"))
        ) { }
        assertEquals(exception.message, "Message")
        assertEquals(exception.cause?.message, "Cause")
        assertTrue(exception.suppressed.isEmpty())
        assertTrue(exception.stackTrace.isEmpty())
        assertEquals(exception.translate(Language.ENGLISH), "Exception text with something value".fmt)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun init() {
            Language.addResolver(TranslatedRuntimeExceptionTest::class.java) { TranslatedRuntimeExceptionTest::class.java.getResource("/sprout/test/lang/$it.json")?.readText(Charsets.UTF_8) }
        }
    }
}