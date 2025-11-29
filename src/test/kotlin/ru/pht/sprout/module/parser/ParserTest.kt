package ru.pht.sprout.module.parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import ru.pht.sprout.module.Module
import ru.pht.sprout.module.lexer.Lexer
import ru.pht.sprout.module.lexer.LexerException

class ParserTest {

    private fun createParser(source: String): Parser {
        val lexer = Lexer(source)
        return Parser(lexer)
    }

    @Nested
    @DisplayName("Module Parsing")
    inner class ModuleParsing {
        @Test
        fun `should parse minimal module`() {
            val source = """(module "pht/module" {[name "test/module"]} {[vers "1.0.0"]})"""
            val parser = createParser(source)
            val module = parser.parse()

            assertEquals("test/module", module.name)
            assertEquals("1.0.0", module.version)
        }

        @Test
        fun `should parse module with all attributes`() {
            val source = """
                (module "pht/module"
                    {[name "test/module"]}
                    {[vers "1.0.0"]}
                    {[desc "Test module"]}
                    {[auth ["Author1" "Author2"]]}
                    {[deps ["pht/core"]]}
                    {[uses ["pht/core"]]}
                    {[inject-from allow]}
                    {[inject-into deny]}
                    {[inject-into-deps allow]}
                    {[imports [*]]}
                    {[exports [adapters plugins]]}
                    {[features [["log" default] ["fast" optional]]]}
                    {[src ["src/*"]]}
                    {[res ["res/*"]]}
                    {[plg ["plg/*"]]}
                )
            """.trimIndent()

            val parser = createParser(source)
            val module = parser.parse()

            assertEquals("test/module", module.name)
            assertEquals("1.0.0", module.version)
            assertEquals("Test module", module.description)
            assertEquals(listOf("Author1", "Author2"), module.authors)
            assertEquals(1, module.dependencies.size)
            assertEquals("pht/core", module.dependencies[0].name)
            assertEquals(listOf("pht/core"), module.uses)
            assertTrue(module.injectFrom)
            assertFalse(module.injectInto)
            assertTrue(module.injectIntoDependencies)
            assertTrue(module.imports.isAny)
            assertFalse(module.exports.isAny)
            assertEquals(2, module.exports.list().size)
            assertEquals(2, module.features.size)
            assertEquals("log" to true, module.features[0])
            assertEquals("fast" to false, module.features[1])
            assertEquals(1, module.sources.size)
            assertEquals(1, module.resources.size)
            assertEquals(1, module.plugins.size)
        }

        @Test
        fun `should throw on unsupported module format`() {
            val source = """(module "unknown/format" {[name "test"]} {[vers "1.0.0"]})"""
            val parser = createParser(source)

            assertThrows(ParserException.Unsupported::class.java) {
                parser.parse()
            }
        }
    }

    @Nested
    @DisplayName("Dependencies Parsing")
    inner class DependenciesParsing {
        @Test
        fun `should parse string dependency`() {
            val source = """
                (module "pht/module"
                    {[name "test/module"]}
                    {[vers "1.0.0"]}
                    {[deps ["pht/core"]]}
                )
            """.trimIndent()

            val parser = createParser(source)
            val module = parser.parse()

            assertEquals(1, module.dependencies.size)
            assertEquals("pht/core", module.dependencies[0].name)
            assertTrue(module.dependencies[0].version.isAny)
            assertFalse(module.dependencies[0].uses)
        }

        @Test
        fun `should parse full dependency definition`() {
            val source = """
                (module "pht/module"
                    {[name "test/module"]}
                    {[vers "1.0.0"]}
                    {[deps [
                        (module
                            {[name "pht/math"]}
                            {[vers [*]]}
                            {[uses allow]}
                            {[adapters [*]]}
                            {[inject-into allow]}
                            {[inject-into-deps deny]}
                            {[inject-from allow]}
                            {[features ["fast-math"]]}
                            {[no-features ["slow-math"]]}
                        )
                    ]]}
                )
            """.trimIndent()

            val parser = createParser(source)
            val module = parser.parse()

            assertEquals(1, module.dependencies.size)
            val dependency = module.dependencies[0]
            assertEquals("pht/math", dependency.name)
            assertTrue(dependency.version.isAny)
            assertTrue(dependency.uses)
            assertTrue(dependency.adapters.isAny)
            assertTrue(dependency.injectInto)
            assertFalse(dependency.injectIntoDependencies)
            assertTrue(dependency.injectFrom)
            assertFalse(dependency.features.isAny)
            assertEquals(listOf("fast-math"), dependency.features.list())
            assertFalse(dependency.disableFeatures.isAny)
            assertEquals(listOf("slow-math"), dependency.disableFeatures.list())
        }
    }

    @Nested
    @DisplayName("Lists Parsing")
    inner class ListsParsing {
        @Test
        fun `should parse string list`() {
            val source = """
                (module "pht/module"
                    {[name "test/module"]}
                    {[vers "1.0.0"]}
                    {[auth ["Author1" "Author2"]]}
                )
            """.trimIndent()

            val parser = createParser(source)
            val module = parser.parse()

            assertEquals(listOf("Author1", "Author2"), module.authors)
        }

        @Test
        fun `should parse empty string list`() {
            val source = """
                (module "pht/module"
                    {[name "test/module"]}
                    {[vers "1.0.0"]}
                    {[auth []]}
                )
            """.trimIndent()

            val parser = createParser(source)
            val module = parser.parse()

            assertEquals(emptyList<String>(), module.authors)
        }

        @Test
        fun `should parse intermodule data list with any`() {
            val source = """
                (module "pht/module"
                    {[name "test/module"]}
                    {[vers "1.0.0"]}
                    {[imports [*]]}
                )
            """.trimIndent()

            val parser = createParser(source)
            val module = parser.parse()

            assertTrue(module.imports.isAny)
        }

        @Test
        fun `should parse intermodule data list with specific values`() {
            val source = """
                (module "pht/module"
                    {[name "test/module"]}
                    {[vers "1.0.0"]}
                    {[exports [adapters plugins macros types functions]]}
                )
            """.trimIndent()

            val parser = createParser(source)
            val module = parser.parse()

            assertFalse(module.exports.isAny)
            val exportList = module.exports.list()
            assertEquals(5, exportList.size)
            assertTrue(exportList.contains(Module.IntermoduleData.ADAPTERS))
            assertTrue(exportList.contains(Module.IntermoduleData.PLUGINS))
            assertTrue(exportList.contains(Module.IntermoduleData.MACROS))
            assertTrue(exportList.contains(Module.IntermoduleData.TYPES))
            assertTrue(exportList.contains(Module.IntermoduleData.FUNCTIONS))
        }
    }

    @Nested
    @DisplayName("Validation")
    inner class Validation {
        @Test
        fun `should accept valid name`() {
            val source = """
                (module "pht/module"
                    {[name "valid-name/module"]}
                    {[vers "1.0.0"]}
                )
            """.trimIndent()

            val parser = createParser(source)
            assertDoesNotThrow { parser.parse() }
        }

        @Test
        fun `should reject invalid name with special characters`() {
            val source = """
                (module "pht/module"
                    {[name "invalid@name"]}
                    {[vers "1.0.0"]}
                )
            """.trimIndent()

            val parser = createParser(source)
            assertThrows(ParserException.ValidationException::class.java) {
                parser.parse()
            }
        }

        @Test
        fun `should accept valid version`() {
            val sources = listOf(
                """(module "pht/module" {[name "test"]} {[vers "1.2.3"]})""",
                """(module "pht/module" {[name "test"]} {[vers "1.0"]})""",
                """(module "pht/module" {[name "test"]} {[vers "1"]})"""
            )

            for (source in sources) {
                val parser = createParser(source)
                assertDoesNotThrow("Should accept version: $source") { parser.parse() }
            }
        }

        @Test
        fun `should reject invalid version with letters`() {
            val source = """
                (module "pht/module"
                    {[name "test"]}
                    {[vers "invalid-version"]}
                )
            """.trimIndent()

            val parser = createParser(source)
            assertThrows(ParserException.ValidationException::class.java) {
                parser.parse()
            }
        }

        @Test
        fun `should accept valid dependency version`() {
            val source = """
                (module "pht/module"
                    {[name "test/module"]}
                    {[vers "1.0.0"]}
                    {[deps [
                        (module
                            {[name "test"]}
                            {[vers "1.2.3"]}
                        )
                    ]]}
                )
            """.trimIndent()

            val parser = createParser(source)
            assertDoesNotThrow { parser.parse() }
        }

        @Test
        fun `should accept valid path`() {
            val sources = listOf(
                """(module "pht/module" {[name "test"]} {[vers "1.0.0"]} {[src ["src/*"]]})""",
                """(module "pht/module" {[name "test"]} {[vers "1.0.0"]} {[src ["src/main.pht"]]})""",
                """(module "pht/module" {[name "test"]} {[vers "1.0.0"]} {[src ["path/to/file"]]})"""
            )

            for (source in sources) {
                val parser = createParser(source)
                assertDoesNotThrow("Should accept path: $source") { parser.parse() }
            }
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {
        @Test
        fun `should throw on unexpected token`() {
            // Используем неправильную структуру - пропускаем ATTR_START
            val source = """(module "pht/module" name "test/module" {[vers "1.0.0"]})"""

            val parser = createParser(source)
            assertThrows(ParserException.UnexpectedToken::class.java) {
                parser.parse()
            }
        }

        @Test
        fun `should throw on lexer exception`() {
            // Создаем невалидный источник для лексера - незакрытая строка
            val source = """(module "pht/module" {[name "test])"""

            val parser = createParser(source)
            assertThrows(LexerException::class.java) {
                parser.parse()
            }
        }

        @Test
        fun `should throw on invalid attribute`() {
            // Используем существующий идентификатор, но в неправильном контексте
            val source = """
                (module "pht/module"
                    {[name "test"]}
                    {[vers "1.0.0"]}
                    {[adapters "value"]} ; adapters не допустим для модуля, только для зависимостей
                )
            """.trimIndent()

            val parser = createParser(source)
            assertThrows(ParserException.UnexpectedToken::class.java) {
                parser.parse()
            }
        }
    }

    @Nested
    @DisplayName("Complex Features")
    inner class ComplexFeatures {
        @Test
        fun `should parse features with defaults`() {
            val source = """
                (module "pht/module"
                    {[name "test/module"]}
                    {[vers "1.0.0"]}
                    {[features [
                        ["log" default]
                        ["fast" optional]
                        ["debug" default]
                    ]]}
                )
            """.trimIndent()

            val parser = createParser(source)
            val module = parser.parse()

            assertEquals(3, module.features.size)
            assertEquals("log" to true, module.features[0])
            assertEquals("fast" to false, module.features[1])
            assertEquals("debug" to true, module.features[2])
        }

        @Test
        fun `should parse allow-deny attributes`() {
            val source = """
                (module "pht/module"
                    {[name "test/module"]}
                    {[vers "1.0.0"]}
                    {[inject-from allow]}
                    {[inject-into deny]}
                    {[inject-into-deps allow]}
                )
            """.trimIndent()

            val parser = createParser(source)
            val module = parser.parse()

            assertTrue(module.injectFrom)
            assertFalse(module.injectInto)
            assertTrue(module.injectIntoDependencies)
        }

        @Test
        fun `should parse path or dependency lists`() {
            val source = """
                (module "pht/module"
                    {[name "test/module"]}
                    {[vers "1.0.0"]}
                    {[src [
                        "src/*.pht"
                        (module 
                            {[name "external/sources"]}
                            {[vers "1.0.0"]}
                        )
                    ]]}
                )
            """.trimIndent()

            val parser = createParser(source)
            val module = parser.parse()

            assertEquals(2, module.sources.size)
            assertTrue(module.sources[0].isPath)
            assertTrue(module.sources[1].isDependency)
            assertEquals("src/*.pht", module.sources[0].path())
            assertEquals("external/sources", module.sources[1].dependency().name)
            assertEquals("1.0.0", module.sources[1].dependency().version.string())
        }

        @Test
        fun `should parse names list or any`() {
            val source = """
                (module "pht/module"
                    {[name "test/module"]}
                    {[vers "1.0.0"]}
                    {[deps [
                        (module
                            {[name "test"]}
                            {[adapters [*]]}
                            {[features ["feat1" "feat2"]]}
                        )
                    ]]}
                )
            """.trimIndent()

            val parser = createParser(source)
            val module = parser.parse()

            val dependency = module.dependencies[0]
            assertTrue(dependency.adapters.isAny)
            assertFalse(dependency.features.isAny)
            assertEquals(listOf("feat1", "feat2"), dependency.features.list())
        }
    }

    @Test
    fun `should parse complete example`() {
        val source = """
            (module "pht/module"
                ; Имя и версия модуля
                {[name "pht/example"]}
                {[vers "1.0.0"]}
                
                ; Описание и авторы
                {[desc "Пример модуля Пихты"]}
                {[auth ["DomamaN202" "Phantom"]]}
                
                ; Зависимости (автоматически)
                {[deps ["pht/core"]]}
                {[uses ["pht/core"]]}
                
                ; Зависимости (продвинутая работа)
                {[deps [
                    (module
                        ; Имя модуля
                        {[name "pht/math"]}
                        ; Версия модуля [по умолчанию]
                        {[vers [*]]}
                        ; Включаем использование
                        {[uses allow]}
                        ; Выключаем все адаптеры
                        {[adapters []]}
                        ; Разрешаем инъекции в модуль
                        {[inject-into allow]}
                        ; Разрешаем инъекции в зависимости модуля
                        {[inject-into-deps allow]}
                        ; Включаем фичу fast-math
                        {[features ["fast-math"]]})
                    (module
                        ; Имя модуля
                        {[name "pht/util"]}
                        ; Версия модуля
                        {[vers "1.0.0"]}
                        ; Выключаем использование [по умолчанию]
                        {[uses deny]}
                        ; Включаем все адаптеры [по умолчанию]
                        {[adapters [*]]}
                        ; Разрешаем инъекции из модуля
                        {[inject-from allow]}
                        ; Выключаем все фичи
                        {[no-features [*]]})]]}
                
                ; Инъекции
                ; - Из модуля в зависимые модули
                {[inject-into allow]}
                ; - Из модуля в зависимости зависимых модулей
                {[inject-into-deps allow]}
                ; - В модуль из зависимостей
                {[inject-from allow]}
                
                ; Импорт и экспорт [по умолчанию]
                {[imports [*]]}
                {[exports [*]]}
                
                ; Импорт и экспорт (конкретно)
                ; - Импортирует адаптеры, типы и функции
                {[imports [adapters types functions]]}
                ; - Экспортирует плагины и макросы
                {[exports [plugins macros]]}
                
                ; Доступные фичи
                {[features [
                    ; Фича по умолчанию
                    ["log" default]
                    ; Фича опциональная
                    ["fast" optional]]]}
                
                ; Исходный код, ресурсы и код плагинов (директории) [по умолчанию]
                {[src ["src/*"]]}
                {[res ["res/*"]]}
                {[plg ["plg/*"]]}
            )
        """.trimIndent()

        val parser = createParser(source)
        val module = parser.parse()

        assertEquals("pht/example", module.name)
        assertEquals("1.0.0", module.version)
        assertEquals("Пример модуля Пихты", module.description)
        assertEquals(listOf("DomamaN202", "Phantom"), module.authors)

        // Проверяем зависимости
        assertEquals(3, module.dependencies.size) // pht/core + pht/math + pht/util

        // Проверяем uses
        assertEquals(listOf("pht/core"), module.uses)

        // Проверяем инъекции
        assertTrue(module.injectInto)
        assertTrue(module.injectIntoDependencies)
        assertTrue(module.injectFrom)

        // Проверяем импорт/экспорт
        assertTrue(module.imports.isAny)
        assertTrue(module.exports.isAny)

        // Проверяем фичи
        assertEquals(2, module.features.size)
        assertEquals("log" to true, module.features[0])
        assertEquals("fast" to false, module.features[1])

        // Проверяем пути
        assertEquals(1, module.sources.size)
        assertEquals(1, module.resources.size)
        assertEquals(1, module.plugins.size)
    }

    @Test
    fun `should handle comments correctly`() {
        val source = """
            (module "pht/module"
                ; Это комментарий
                {[name "test/module"]} ; И еще комментарий
                ; Много комментариев
                ; подряд
                {[vers "1.0.0"]}
            )
        """.trimIndent()

        val parser = createParser(source)
        val module = parser.parse()

        assertEquals("test/module", module.name)
        assertEquals("1.0.0", module.version)
    }
}