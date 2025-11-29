package ru.pht.sprout.module.parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.pht.sprout.module.lexer.Lexer
import ru.pht.sprout.module.lexer.LexerException
import ru.pht.sprout.module.parser.ParserException.*

class ParserTest {

    private fun getRootCause(throwable: Throwable): Throwable {
        var cause = throwable
        while (cause.cause != null) {
            cause = cause.cause!!
        }
        return cause
    }

    @Test
    @DisplayName("Парсинг минимального модуля")
    fun parseMinimalModule() {
        val input = """
            (module "pht/module"
                {[name "test/module"]}
                {[vers "1.0.0"]})
        """.trimIndent()

        val module = Parser(Lexer(input)).parse()

        assertEquals("test/module", module.name)
        assertEquals("1.0.0", module.version)
    }

    @Test
    @DisplayName("Парсинг полного модуля")
    fun parseFullModule() {
        val input = """
            (module "pht/module"
                {[name "full/module"]}
                {[vers "2.1.3-beta"]}
                {[desc "Описание модуля"]}
                {[auth ["author1" "author2"]]}
                {[deps ["dep1" "dep2"]]}
                {[uses ["use1" "use2"]]}
                {[inject-into allow]}
                {[inject-into-deps deny]}
                {[inject-from allow]}
                {[imports [*]]}
                {[exports [adapters plugins]]}
                {[features [["feat1" default] ["feat2" optional]]]}
                {[src ["src/*.pht" "main.pht"]]}
                {[res ["res/*"]]}
                {[plg ["plg/*"]]})
        """.trimIndent()

        val module = Parser(Lexer(input)).parse()

        assertEquals("full/module", module.name)
        assertEquals("2.1.3-beta", module.version)
        assertEquals("Описание модуля", module.description)
        assertEquals(listOf("author1", "author2"), module.authors)
        assertEquals(2, module.dependencies.size)
        assertEquals(listOf("use1", "use2"), module.uses)
        assertTrue(module.injectInto)
        assertFalse(module.injectIntoDependencies)
        assertTrue(module.injectFrom)
        assertTrue(module.imports.isAny)
        assertEquals(2, module.exports.list().size)
        assertEquals(2, module.features.size)
        // Исправлено: было 3, но в тестовых данных только 2 пути в src
        assertEquals(2, module.sources.size)
    }

    @Nested
    @DisplayName("Парсинг зависимостей")
    inner class DependenciesParsing {

        @Test
        @DisplayName("Простая зависимость строкой")
        fun parseStringDependency() {
            val input = """
                (module "pht/module"
                    {[name "test"]}
                    {[vers "1.0.0"]}
                    {[deps ["simple/dep"]]})
            """.trimIndent()

            val module = Parser(Lexer(input)).parse()

            assertEquals(1, module.dependencies.size)
            assertEquals("simple/dep", module.dependencies[0].name)
        }

        @Test
        @DisplayName("Расширенная зависимость с атрибутами")
        fun parseExtendedDependency() {
            val input = """
                (module "pht/module"
                    {[name "test"]}
                    {[vers "1.0.0"]}
                    {[deps [
                        (module
                            {[name "ext/dep"]}
                            {[vers "1.5.0"]}
                            {[uses allow]}
                            {[adapters [*]]}
                            {[inject-into deny]}
                            {[features ["fast" "secure"]]}
                            {[no-features ["legacy"]]})
                    ]]})
            """.trimIndent()

            val module = Parser(Lexer(input)).parse()
            val dep = module.dependencies[0]

            assertEquals("ext/dep", dep.name)
            assertEquals("1.5.0", dep.version.string())
            assertTrue(dep.uses)
            assertTrue(dep.adapters.isAny)
            assertFalse(dep.injectInto)
            assertEquals(2, dep.features.list().size)
            assertEquals(1, dep.disableFeatures.list().size)
        }
    }

    @Nested
    @DisplayName("Валидация строк")
    inner class StringValidation {

        @Test
        @DisplayName("Корректное имя модуля")
        fun validModuleName() {
            val names = listOf(
                "test", "test-module", "test/module", "test123", "TEST", "test_module",
                "org/example/module", "com-company-project"
            )

            names.forEach { name ->
                val input = """
                    (module "pht/module"
                        {[name "$name"]}
                        {[vers "1.0.0"]})
                """.trimIndent()

                assertDoesNotThrow {
                    Parser(Lexer(input)).parse()
                }
            }
        }

        @Test
        @DisplayName("Некорректное имя модуля")
        fun invalidModuleName() {
            val names = listOf(
                "", "-test", "test-", "/test", "test/", "test.", ".test",
                "test@module", "test module", "test*"
            )

            names.forEach { name ->
                val input = """
                    (module "pht/module"
                        {[name "$name"]}
                        {[vers "1.0.0"]})
                """.trimIndent()

                val exception = assertThrows<Wrapped.FromParser> {
                    Parser(Lexer(input)).parse()
                }
                val rootCause = getRootCause(exception)
                assertTrue(rootCause is ValidationException)
            }
        }

        @Test
        @DisplayName("Корректная версия модуля")
        fun validModuleVersion() {
            val versions = listOf(
                "1", "1.0", "1.0.0", "1.0.0-alpha", "2.1.3-beta",
                "0.0.1", "10.20.30", "1.0.0-release"
            )

            versions.forEach { version ->
                val input = """
                    (module "pht/module"
                        {[name "test"]}
                        {[vers "$version"]})
                """.trimIndent()

                assertDoesNotThrow {
                    Parser(Lexer(input)).parse()
                }
            }
        }

        @Test
        @DisplayName("Корректные пути")
        fun validPaths() {
            val paths = listOf(
                "src", "src/main", "src/*", "src/*.pht", "main.pht",
                "res/images", "plg/plugins", "file-name.txt", "file123"
            )

            paths.forEach { path ->
                val input = """
                    (module "pht/module"
                        {[name "test"]}
                        {[vers "1.0.0"]}
                        {[src ["$path"]]})
                """.trimIndent()

                assertDoesNotThrow {
                    Parser(Lexer(input)).parse()
                }
            }
        }
    }

    @Nested
    @DisplayName("Обработка ошибок")
    inner class ErrorHandling {

        @Test
        @DisplayName("Отсутствует обязательный атрибут name")
        fun missingNameAttribute() {
            val input = """
                (module "pht/module"
                    {[vers "1.0.0"]})
            """.trimIndent()

            val exception = assertThrows<Wrapped.FromParser> {
                Parser(Lexer(input)).parse()
            }
            val rootCause = getRootCause(exception)
            assertTrue(rootCause is NotInitializedException)
            assertTrue(rootCause.message?.contains("name") == true)
        }

        @Test
        @DisplayName("Отсутствует обязательный атрибут version")
        fun missingVersionAttribute() {
            val input = """
                (module "pht/module"
                    {[name "test"]})
            """.trimIndent()

            val exception = assertThrows<Wrapped.FromParser> {
                Parser(Lexer(input)).parse()
            }
            val rootCause = getRootCause(exception)
            assertTrue(rootCause is NotInitializedException)
            assertTrue(rootCause.message?.contains("vers") == true)
        }

        @Test
        @DisplayName("Неожиданный токен")
        fun unexpectedToken() {
            val input = """
        (module "pht/module"
            {[invalid "test"]}
            {[vers "1.0.0"]})
    """.trimIndent()

            val exception = assertThrows<Wrapped.FromParser> {
                Parser(Lexer(input)).parse()
            }

            // Проверяем, что где-то в цепочке есть LexerException.InvalidIdentifier
            var current: Throwable? = exception
            var foundInvalidIdentifier = false
            while (current != null) {
                if (current is LexerException.InvalidIdentifier) {
                    foundInvalidIdentifier = true
                    break
                }
                current = current.cause
            }
            assertTrue(foundInvalidIdentifier, "Должно содержать LexerException.InvalidIdentifier в цепочке исключений")
        }

        @Test
        @DisplayName("Незакрытый список")
        fun unclosedList() {
            val input = """
                (module "pht/module"
                    {[name "test"]}
                    {[vers "1.0.0"]}
                    {[auth ["author1" "author2"])
            """.trimIndent()

            val exception = assertThrows<Wrapped.FromParser> {
                Parser(Lexer(input)).parse()
            }
            // Проверяем, что где-то в цепочке есть UnexpectedToken
            var current: Throwable? = exception
            var foundUnexpectedToken = false
            while (current != null) {
                if (current is UnexpectedToken) {
                    foundUnexpectedToken = true
                    break
                }
                current = current.cause
            }
            assertTrue(foundUnexpectedToken)
        }

        @Test
        @DisplayName("Незакрытая инструкция")
        fun unclosedInstruction() {
            val input = """
                (module "pht/module"
                    {[name "test"]}
                    {[vers "1.0.0"]
            """.trimIndent()

            val exception = assertThrows<Wrapped.FromParser> {
                Parser(Lexer(input)).parse()
            }
            // Проверяем, что где-то в цепочке есть UnexpectedToken
            var current: Throwable? = exception
            var foundUnexpectedToken = false
            while (current != null) {
                if (current is UnexpectedToken) {
                    foundUnexpectedToken = true
                    break
                }
                current = current.cause
            }
            assertTrue(foundUnexpectedToken)
        }
    }

    @Nested
    @DisplayName("Специальные случаи")
    inner class SpecialCases {

        @Test
        @DisplayName("Импорт всего")
        fun importAll() {
            val input = """
                (module "pht/module"
                    {[name "test"]}
                    {[vers "1.0.0"]}
                    {[imports [*]]}
                    {[exports [*]]})
            """.trimIndent()

            val module = Parser(Lexer(input)).parse()

            assertTrue(module.imports.isAny)
            assertTrue(module.exports.isAny)
        }

        @Test
        @DisplayName("Версия зависимости - любая")
        fun dependencyVersionAny() {
            val input = """
                (module "pht/module"
                    {[name "test"]}
                    {[vers "1.0.0"]}
                    {[deps [
                        (module
                            {[name "dep"]}
                            {[vers [*]]})
                    ]]})
            """.trimIndent()

            val module = Parser(Lexer(input)).parse()
            val dep = module.dependencies[0]

            assertTrue(dep.version.isAny)
        }

        @Test
        @DisplayName("Адаптеры зависимости - все")
        fun dependencyAdaptersAll() {
            val input = """
                (module "pht/module"
                    {[name "test"]}
                    {[vers "1.0.0"]}
                    {[deps [
                        (module
                            {[name "dep"]}
                            {[vers "1.0.0"]}
                            {[adapters [*]]})
                    ]]})
            """.trimIndent()

            val module = Parser(Lexer(input)).parse()
            val dep = module.dependencies[0]

            assertTrue(dep.adapters.isAny)
        }

        @Test
        @DisplayName("Источники из зависимости")
        fun sourcesFromDependency() {
            val input = """
                (module "pht/module"
                    {[name "test"]}
                    {[vers "1.0.0"]}
                    {[src [
                        (module
                            {[name "source/module"]}
                            {[vers "1.0.0"]})
                    ]]})
            """.trimIndent()

            val module = Parser(Lexer(input)).parse()

            assertEquals(1, module.sources.size)
            assertTrue(module.sources[0].isDependency)
            assertEquals("source/module", module.sources[0].dependency().name)
        }
    }

    @Test
    @DisplayName("Пустые списки")
    fun emptyLists() {
        val input = """
            (module "pht/module"
                {[name "test"]}
                {[vers "1.0.0"]}
                {[auth []]}
                {[deps []]}
                {[uses []]}
                {[src []]})
        """.trimIndent()

        val module = Parser(Lexer(input)).parse()

        assertTrue(module.authors.isEmpty())
        assertTrue(module.dependencies.isEmpty())
        assertTrue(module.uses.isEmpty())
        assertTrue(module.sources.isEmpty())
    }

    @Test
    @DisplayName("Смешанные источники")
    fun mixedSources() {
        val input = """
            (module "pht/module"
                {[name "test"]}
                {[vers "1.0.0"]}
                {[src [
                    "src/*.pht"
                    (module
                        {[name "external/sources"]}
                        {[vers "1.0.0"]})
                    "main.pht"
                ]]})
        """.trimIndent()

        val module = Parser(Lexer(input)).parse()

        assertEquals(3, module.sources.size)
        assertTrue(module.sources[0].isPath)
        assertTrue(module.sources[1].isDependency)
        assertTrue(module.sources[2].isPath)
    }
}