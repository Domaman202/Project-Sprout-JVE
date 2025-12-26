package ru.pht.sprout.utils.exception

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import ru.DmN.cmd.style.FmtUtils.fmt
import ru.DmN.translate.Language
import kotlin.test.Test
import kotlin.test.assertEquals

@EnabledIf("ru.pht.sprout.TestConfigInternal#exceptionWithTranslateTest", disabledReason = "Тест выключен конфигурацией")
class NotValueExceptionTest {
    @Test
    @DisplayName("Перевод")
    fun translateTest() {
        val exception = NotValueException()
        assertEquals(
            "Value has not been set".fmt,
            exception.message
        )
        assertEquals(
            "Value has not been set".fmt,
            exception.translate(Language.ENGLISH)
        )
    }
}