package ru.pht.sprout.cli.args

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.EnabledIf
import ru.DmN.cmd.style.FmtUtils.fmt
import ru.DmN.translate.Language
import ru.pht.sprout.utils.exception.SproutIllegalArgumentException
import kotlin.test.Test
import kotlin.test.assertEquals

@EnabledIf("ru.pht.sprout.TestConfigInternal#argsParseTest", disabledReason = "Тест выключен конфигурацией")
class CommandTest {
    @Test
    @DisplayName("Проверка имён команды")
    fun definitionNameTest() {
        assertNotNull(Command.short("teST123", "teST123", emptyArray(), null))
        assertNotNull(Command.long("teST123", emptyArray(), null))
        assertNotNull(Command.long("teST123 321tsET", emptyArray(), null))
        assertEquals(
            "Split syntax not allowed for short commands".fmt,
            assertThrows<SproutIllegalArgumentException> {
                Command.short("test", "test some", emptyArray(), null)
            }.translate(Language.ENGLISH)
        )
        assertEquals(
            "Invalid command name '§sb$$$§sr'".fmt,
            assertThrows<SproutIllegalArgumentException> {
                Command.short("$$$", "test", emptyArray(), null)
            }.translate(Language.ENGLISH)
        )
        assertEquals(
            "Invalid command name '§sb$$$§sr'".fmt,
            assertThrows<SproutIllegalArgumentException> {
                Command.short("test", "$$$", emptyArray(), null)
            }.translate(Language.ENGLISH)
        )
        assertEquals(
            "Invalid command name '§sb$$$§sr'".fmt,
            assertThrows<SproutIllegalArgumentException> {
                Command.long("$$$", emptyArray(), null)
            }.translate(Language.ENGLISH)
        )
        assertEquals(
            "Invalid command name '§sb$$$§sr'".fmt,
            assertThrows<SproutIllegalArgumentException> {
                Command.long("test $$$", emptyArray(), null)
            }.translate(Language.ENGLISH)
        )
    }

    @Test
    @DisplayName("Поиск аргумента")
    fun argumentTest() {
        val aDef = CommandArgument.Definition("value", null, CommandArgument.Type.STRING, null, false)
        val argument = CommandArgument(aDef, "text")
        val cDef = Command.long("test", arrayOf(aDef), null)
        val command = Command(cDef, listOf(argument))
        assertEquals("text", command.argument("value"))
        assertNull(command.argument("unknown"))
    }
}