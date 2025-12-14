package ru.pht.sprout.utils.lang

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import ru.pht.sprout.cli.App
import ru.pht.sprout.utils.NotValueException
import ru.pht.sprout.utils.fmt.FmtUtils.fmt
import kotlin.test.Test
import kotlin.test.assertEquals

@EnabledIf("ru.pht.sprout.TestConfigInternal#translateTest", disabledReason = "Тест выключен конфигурацией")
class SproutTranslateTest {
    @Test
    @DisplayName("Проверка метода of")
    fun ofTest() {
        assertEquals(
            SproutTranslate
                .of<NotValueException>()
                .translate(Language.ENGLISH),
            "Value has not been set".fmt
        )
    }

    @Test
    @DisplayName("Проверка метода of с параметром key")
    fun ofKeyTest() {
        assertEquals(
            SproutTranslate
                .of<App>("cmd.help.desc")
                .translate(Language.ENGLISH),
            "Print it text / Help about command".fmt
        )
    }

    @Test
    @DisplayName("Проверка метода translate")
    fun translateTest() {
        assertEquals(
            SproutTranslate.translate<App>(Language.ENGLISH, "printHelp.command", Pair("long", "help")),
            "§sb[§f4help§sr§sb]".fmt
        )
    }
}