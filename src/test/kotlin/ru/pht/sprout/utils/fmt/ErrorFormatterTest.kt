package ru.pht.sprout.utils.fmt

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import kotlin.test.Test


@EnabledIf("ru.pht.sprout.TestConfigInternal#fmtTest", disabledReason = "Тест выключен конфигурацией")
class ErrorFormatterTest {
    @Test
    @DisplayName("Сообщение в одну строку")
    fun oneLineTest() {
        val source = """
            line1
            line2
            line3
        """.trimIndent()
        val expected = """
            [2, 1] line2
                   ^~~~~ Тут какая-то ошибка
        """.trimIndent()
        val actual = ErrorFormatter.formatErrorWithToken(source, 6, 5, 1, 0, "Тут какая-то ошибка")
        Assertions.assertEquals(expected, actual)
    }

    @Test
    @DisplayName("Сообщение в одну строку с табуляцией в исходном коде")
    fun tabInSourceOneLineTest() {
        val source = "line1\n22\tline2\nline3"
        val expected = """
            [2, 1] 22    line2
                         ^~~~~ Тут какая-то ошибка
        """.trimIndent()
        val actual = ErrorFormatter.formatErrorWithToken(source, 9, 5, 1, 0, "Тут какая-то ошибка")
        Assertions.assertEquals(expected, actual)
    }

    @Test
    @DisplayName("Сообщение в несколько строк")
    fun oneMultilineTest() {
        val source = """
            line1
            line2
            line3
        """.trimIndent()
        val expected = """
            [2, 1] line2
                   ^~~~~ Тут какая-то ошибка!
                         Вот и живи с этим.
                         А тут я не придумал...
        """.trimIndent()
        val actual = ErrorFormatter.formatErrorWithToken(source, 6, 5, 1, 0, "Тут какая-то ошибка!\nВот и живи с этим.\nА тут я не придумал...")
        Assertions.assertEquals(expected, actual)
    }

    @Test
    @DisplayName("Сообщение в несколько строк и табуляцией")
    fun oneMultilineAndTabTest() {
        val source = """
            line1
            line2
            line3
        """.trimIndent()
        val expected = """
            [2, 1] line2
                   ^~~~~ Тут	какая-то	ошибка!
                         Вот	и	живи	с	этим.
                         А		тут	я		не	придумал...
        """.trimIndent()
        val actual = ErrorFormatter.formatErrorWithToken(source, 6, 5, 1, 0, "Тут\tкакая-то\tошибка!\nВот\tи\tживи\tс\tэтим.\nА\t\tтут\tя\t\tне\tпридумал...")
        Assertions.assertEquals(expected, actual)
    }

    @Test
    @DisplayName("Сообщение в несколько строк и табуляцией с табуляцией в исходном коде")
    fun tabInSourceOneMultilineAndTabTest() {
        val source = "line1\n22\tline2\nline3"
        val expected = """
            [2, 1] 22    line2
                         ^~~~~ Тут	какая-то	ошибка!
                               Вот	и	живи	с	этим.
                               А		тут	я		не	придумал...
        """.trimIndent()
        val actual = ErrorFormatter.formatErrorWithToken(source, 9, 5, 1, 0, "Тут\tкакая-то\tошибка!\nВот\tи\tживи\tс\tэтим.\nА\t\tтут\tя\t\tне\tпридумал...")
        Assertions.assertEquals(expected, actual)
    }
}