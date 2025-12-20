package ru.pht.sprout.utils

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import ru.DmN.cmd.style.FmtUtils.fmt
import ru.DmN.translate.Language
import ru.pht.sprout.cli.App
import kotlin.test.Test
import kotlin.test.assertEquals

@EnabledIf("ru.pht.sprout.TestConfigInternal#translateTest", disabledReason = "Тест выключен конфигурацией")
class SproutTranslateTest {
    @Test
    @DisplayName("Метод of")
    fun ofTest() {
        assertEquals(
            "Value has not been set".fmt,
            SproutTranslate.of<NotValueException>(Language.ENGLISH)
        )
    }

    @Test
    @DisplayName("Метод of с параметром key")
    fun ofKeyTest() {
        assertEquals(
            "Print it text / Help about command".fmt,
            SproutTranslate.of<App>(Language.ENGLISH, "cmd.help.desc")
        )
    }

    @Test
    @DisplayName("Метод translate")
    fun translateTest() {
        assertEquals(
            "§sb[§f4help§sr§sb]".fmt,
            SproutTranslate.of<App>(Language.ENGLISH, "printHelp.command", "long" to "help")
        )
    }
}