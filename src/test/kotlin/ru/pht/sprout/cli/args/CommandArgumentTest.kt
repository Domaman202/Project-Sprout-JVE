package ru.pht.sprout.cli.args

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.condition.EnabledIf
import ru.DmN.cmd.style.FmtUtils.fmt
import ru.DmN.translate.Language
import ru.DmN.translate.TranslationKey
import ru.DmN.translate.TranslationPair
import ru.pht.sprout.utils.SproutTranslate
import ru.pht.sprout.utils.exception.SproutIllegalArgumentException
import kotlin.test.*

@EnabledIf("ru.pht.sprout.TestConfigInternal#argsParseTest", disabledReason = "Тест выключен конфигурацией")
class CommandArgumentTest {
    @Test
    @DisplayName("Проверка имени аргумента и отображаемого имени в определении")
    fun definitionNameTest() {
        assertEquals(
            "teST123",
            CommandArgument.Definition(
                "teST123",
                null,
                CommandArgument.Type.STRING,
                null,
                false
            ).displayedName(Language.ENGLISH)
        )
        assertEquals(
            "I = 123".fmt,
            CommandArgument.Definition(
                "test",
                TranslationPair(TranslationKey.of<SproutTranslate>("test123"), SproutTranslate),
                CommandArgument.Type.STRING,
                null,
                false
            ).displayedName(Language.ENGLISH)
        )
        assertEquals(
            "Invalid argument name '§sb%$@§sr'".fmt,
            assertThrows<SproutIllegalArgumentException> {
                CommandArgument.Definition("%$@", null, CommandArgument.Type.STRING, null, false)
            }.translate(Language.ENGLISH)
        )
    }

    @Test
    @DisplayName("Проверка наличия варианта")
    fun variantTest() {
        assertEquals(
            "No argument variants specified".fmt,
            assertThrows<SproutIllegalArgumentException> {
                CommandArgument.Definition("test", null, CommandArgument.Type.VARIATION, null, false)
            }.translate(Language.ENGLISH)
        )
        assertNotNull(
            CommandArgument.Definition(
                "test",
                null,
                CommandArgument.Type.VARIATION,
                CommandArgument.Variants.of(listOf("yes", "no")),
                false
            ).variants
        )
    }

    @Test
    @DisplayName("Проверка вариантов")
    fun variantsTest() {
        val strings = CommandArgument.Variants.of(listOf("v1", "v2", "v3"))
        assertTrue(strings.strings)
        assertFalse(strings.supplier)
        assertFalse(strings.enums)
        assertEquals("v1", strings.parse("v1"))
        assertEquals("v1", strings.parseString("v1"))
        assertNull(strings.parse("V1"))
        assertNull(strings.parse("other"))
        assertNull(strings.parseString("V1"))
        assertNull(strings.parseString("other"))
        assertFails { strings.parseEnum("v1") }
        val supplier = CommandArgument.Variants.of { listOf("s1", "s2", "s3") }
        assertFalse(supplier.strings)
        assertTrue(supplier.supplier)
        assertFalse(supplier.enums)
        assertEquals("s1", supplier.parse("s1"))
        assertEquals("s1", supplier.parseString("s1"))
        assertNull(supplier.parse("S1"))
        assertNull(supplier.parse("other"))
        assertNull(supplier.parseString("S1"))
        assertNull(supplier.parseString("other"))
        assertFails { supplier.parseEnum("s1") }
        val enums = CommandArgument.Variants.of(CommandArgument.Type.entries.toTypedArray())
        assertFalse(enums.strings)
        assertFalse(enums.supplier)
        assertTrue(enums.enums)
        assertEquals(CommandArgument.Type.STRING, enums.parse("string"))
        assertEquals(CommandArgument.Type.STRING, enums.parse("STRING"))
        assertEquals(CommandArgument.Type.STRING, enums.parseEnum("string"))
        assertEquals(CommandArgument.Type.STRING, enums.parseEnum("STRING"))
        assertNull(enums.parse("other"))
        assertNull(enums.parseEnum("other"))
        assertFails { enums.parseString("v1") }
    }
}