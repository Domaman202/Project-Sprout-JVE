package ru.pht.sprout.utils.lang

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import ru.pht.sprout.utils.fmt.FmtUtils.fmt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TranslatedRuntimeExceptionTest {
    @Test
    @DisplayName("Тестирование перевода с помощью translate")
    fun translateTest() {
        assertEquals(
            TranslatedRuntimeException(Translation.of<TranslatedRuntimeExceptionTest>("basic")).translate(Language.ENGLISH),
            "Exception text".fmt
        )
    }

    @Test
    @DisplayName("Тестирование перевода с аргументами помощью translate")
    fun translateWithValuesTest() {
        assertEquals(
            TranslatedRuntimeException(
                Translation.of<TranslatedRuntimeExceptionTest>("value"),
                Pair("value", "something value"),
            ).translate(Language.ENGLISH),
            "Exception text with something value".fmt
        )
    }

    @Test
    @DisplayName("Тестирование перевода с помощью message")
    fun translateMessageTest() {
        assertEquals(
            TranslatedRuntimeException(Translation.of<TranslatedRuntimeExceptionTest>("basic")).message,
            "Exception text".fmt
        )
    }

    @Test
    @DisplayName("Тестирование перевода с аргументами помощью message")
    fun translateMessageWithValuesTest() {
        assertEquals(
            TranslatedRuntimeException(
                Translation.of<TranslatedRuntimeExceptionTest>("value"),
                Pair("value", "something value"),
            ).message,
            "Exception text with something value".fmt
        )
    }

    @Test
    @DisplayName("Конструктор с message")
    fun messageCtorTest() {
        val exception = TranslatedRuntimeException("Message", Translation.of<TranslatedRuntimeExceptionTest>("basic"))
        assertEquals(exception.message, "Message")
        assertEquals(exception.translate(Language.ENGLISH), "Exception text".fmt)
    }


    @Test
    @DisplayName("Конструктор с message и cause")
    fun messageAndCauseCtorTest() {
        val exception = TranslatedRuntimeException("Message", Exception("Cause"), Translation.of<TranslatedRuntimeExceptionTest>("basic"))
        assertEquals(exception.message, "Message")
        assertEquals(exception.cause?.message, "Cause")
        assertEquals(exception.translate(Language.ENGLISH), "Exception text".fmt)
    }

    @Test
    @DisplayName("Конструктор с cause")
    fun causeCtorTest() {
        val exception = TranslatedRuntimeException(Exception("Cause"), Translation.of<TranslatedRuntimeExceptionTest>("basic"))
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
            translation = Translation.of<TranslatedRuntimeExceptionTest>("value"),
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