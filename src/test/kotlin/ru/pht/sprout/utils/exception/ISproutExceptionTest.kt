package ru.pht.sprout.utils.exception

import kotlinx.io.files.FileNotFoundException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.condition.EnabledIf
import ru.DmN.cmd.style.FmtUtils.fmt
import ru.DmN.translate.Language
import ru.DmN.translate.TranslationKey
import ru.pht.sprout.utils.SproutTranslate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@EnabledIf("ru.pht.sprout.TestConfigInternal#exceptionWithTranslateTest", disabledReason = "Тест выключен конфигурацией")
class ISproutExceptionTest {
    @Nested
    inner class SproutFileNotFoundExceptionTest {
        @Test
        @DisplayName("Наследование & Перевод")
        fun test() {
            val exception = SproutFileNotFoundException(TranslationKey.of<SproutTranslate>("test"), "i" to 123)
            assertIs<FileNotFoundException>(exception)
            assertEquals(
                "I = §sb123".fmt,
                exception.message
            )
            assertEquals(
                "I = §sb123".fmt,
                exception.translate(Language.ENGLISH)
            )
        }
    }

    @Nested
    inner class SproutIllegalArgumentExceptionTest {
        @Test
        @DisplayName("Наследование & Перевод")
        fun test() {
            val exception = SproutIllegalArgumentException(TranslationKey.of<SproutTranslate>("test"), "i" to 321)
            assertIs<IllegalArgumentException>(exception)
            assertEquals(
                "I = §sb321".fmt,
                exception.message
            )
            assertEquals(
                "I = §sb321".fmt,
                exception.translate(Language.ENGLISH)
            )
        }
    }

    @Nested
    inner class SproutIllegalStateExceptionTest {
        @Test
        @DisplayName("Наследование & Перевод")
        fun test() {
            val exception = SproutIllegalStateException(TranslationKey.of<SproutTranslate>("test"), "i" to 777)
            assertIs<IllegalStateException>(exception)
            assertEquals(
                "I = §sb777".fmt,
                exception.message
            )
            assertEquals(
                "I = §sb777".fmt,
                exception.translate(Language.ENGLISH)
            )
        }
    }
}