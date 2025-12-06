package ru.pht.sprout.module.header.parser

import io.github.z4kn4fein.semver.constraints.toConstraint
import io.github.z4kn4fein.semver.toVersion
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import ru.pht.sprout.module.header.ModuleHeader
import ru.pht.sprout.module.header.lexer.Lexer
import ru.pht.sprout.module.header.lexer.LexerException
import ru.pht.sprout.module.header.lexer.Token

class ParserTest {
    @Test
    fun testMinimalModule() {
        val source = """
            (module "pht/module"
                {[name "pht/minimal"]}
                {[vers "0.0.1"]}
            )
        """.trimIndent()
        val parser = Parser(Lexer(source))
        val module = parser.parse()
        assertEquals("pht/minimal", module.name)
        assertEquals("0.0.1".toVersion(), module.version)
    }

    @Test
    fun testFullModule() {
        val source = """
            (module "pht/module"
                {[name "pht/full-example"]}
                {[vers "1.2.3"]}
                {[desc "This is a full example"]}
                {[auth ["author1" "author2"]]}
                {[deps [
                    "pht/core"
                    (module
                        {[name "pht/lib"]}
                        {[vers "1.0.0"]}
                        {[uses allow]}
                        {[adapters [*]]}
                        {[inject-into allow]}
                        {[inject-into-deps allow]}
                        {[inject-from allow]}
                        {[features ["f1"]]}
                        {[no-features ["f2"]]}
                    )
                ]]}
                {[inject-into allow]}
                {[inject-into-deps deny]}
                {[inject-from allow]}
                {[imports [adapters types]]}
                {[exports [*]]}
                {[features [["feat1" default] ["feat2" optional]]]}
                {[src ["src/*.pht" (module {[name "pht/more-src"]})]]}
                {[res ["res"]]}
                {[plg ["plg/main.pht"]]}
            )
        """.trimIndent()
        val parser = Parser(Lexer(source))
        val module = parser.parse()

        assertEquals("pht/full-example", module.name)
        assertEquals("1.2.3".toVersion(), module.version)
        assertEquals("This is a full example", module.description)
        assertEquals(listOf("author1", "author2"), module.authors)
        assertEquals(2, module.dependencies.size)
        assertEquals("pht/core", module.dependencies[0].name)
        val dep = module.dependencies[1]
        assertEquals("pht/lib", dep.name)
        assertTrue(dep.version.isValue)
        assertEquals("1.0.0".toConstraint(), dep.version.value())
        assertTrue(dep.uses)
        assertTrue(dep.adapters.isAny)
        assertTrue(dep.injectInto)
        assertTrue(dep.injectIntoDependencies)
        assertTrue(dep.injectFrom)
        assertTrue(dep.features.isValue)
        assertEquals(listOf("f1"), dep.features.value())
        assertTrue(dep.disableFeatures.isValue)
        assertEquals(listOf("f2"), dep.disableFeatures.value())

        assertTrue(module.injectInto)
        assertFalse(module.injectIntoDependencies)
        assertTrue(module.injectFrom)

        assertTrue(module.imports.isValue)
        assertEquals(listOf(ModuleHeader.IntermoduleData.ADAPTERS, ModuleHeader.IntermoduleData.TYPES), module.imports.value())
        assertTrue(module.exports.isAny)
        assertEquals(listOf("feat1" to true, "feat2" to false), module.features)

        assertEquals(2, module.sources.size)
        assertTrue(module.sources[0].isPath)
        assertEquals("src/*.pht", module.sources[0].path())
        assertTrue(module.sources[1].isDependency)
        assertEquals("pht/more-src", module.sources[1].dependency().name)

        assertEquals(1, module.resources.size)
        assertTrue(module.resources[0].isPath)
        assertEquals("res", module.resources[0].path())

        assertEquals(1, module.plugins.size)
        assertTrue(module.plugins[0].isPath)
        assertEquals("plg/main.pht", module.plugins[0].path())
    }
    
    @Test
    fun testMissingName() {
        val source = """
            (module "pht/module"
                {[vers "1.0.0"]}
            )
        """.trimIndent()
        val parser = Parser(Lexer(source))
        val exception = assertThrows(ParserException.Wrapped.FromParser::class.java) {
            parser.parse()
        }
        assertTrue(exception.exception is ParserException.NotInitializedException)
    }

    @Test
    fun testMissingVersion() {
        val source = """
            (module "pht/module"
                {[name "pht/missing-version"]}
            )
        """.trimIndent()
        val parser = Parser(Lexer(source))
        val exception = assertThrows(ParserException.Wrapped.FromParser::class.java) {
            parser.parse()
        }
        assertTrue(exception.exception is ParserException.NotInitializedException)
    }

    @Test
    fun testInvalidVersion() {
        val source = """
            (module "pht/module"
                {[name "pht/invalid-version"]}
                {[vers "1.a.0"]}
            )
        """.trimIndent()
        val parser = Parser(Lexer(source))
        assertThrows(ParserException.Wrapped.FromParser::class.java) {
            parser.parse()
        }
    }

    @Test
    fun testInvalidName() {
        val source = """
            (module "pht/module"
                {[name "pht/invalid name"]}
                {[vers "1.0.0"]}
            )
        """.trimIndent()
        val parser = Parser(Lexer(source))
        assertThrows(ParserException.Wrapped.FromParser::class.java) {
            parser.parse()
        }
    }

    @Test
    fun testInvalidPath() {
        val source = """
            (module "pht/module"
                {[name "pht/invalid-path"]}
                {[vers "1.0.0"]}
                {[src ["./"]]}
            )
        """.trimIndent()
        val parser = Parser(Lexer(source))
        assertThrows(ParserException.Wrapped.FromParser::class.java) {
            parser.parse()
        }
    }

    @Test
    fun testUnsupportedModule() {
        val source = """
            (module "unsupported/module"
                {[name "pht/unsupported"]}
                {[vers "1.0.0"]}
            )
        """.trimIndent()
        val parser = Parser(Lexer(source))
        val exception = assertThrows(ParserException.Wrapped.FromParser::class.java) {
            parser.parse()
        }
        assertTrue(exception.exception is ParserException.UnsupportedHeader)
    }

    @Test
    fun testUnexpectedToken() {
        val source = """
            (module "pht/module"
                {[name "pht/unexpected"]}
                {[vers "1.0.0"]}
                {
            )
        """.trimIndent()
        val parser = Parser(Lexer(source))
        val exception = assertThrows(ParserException.Wrapped.FromLexer::class.java) {
            parser.parse()
        }
        assertTrue(exception.exception is LexerException.UnexpectedSymbol)
    }

    @Test
    fun testParserExceptionPrints() {
        val source = """
            (module "pht/module"
                {[vers "1.0.0"]}
            )
        """.trimIndent()
        val parser = Parser(Lexer(source))
        val notInitializedException = assertThrows(ParserException.Wrapped.FromParser::class.java) {
            parser.parse()
        }
        assertTrue(notInitializedException.print(parser).toString().isNotBlank())

        val source2 = """
            (module "pht/module"
                {[name "pht/invalid-version"]}
                {[vers "1.a.0"]}
            )
        """.trimIndent()
        val parser2 = Parser(Lexer(source2))
        val validationException = assertThrows(ParserException.Wrapped.FromParser::class.java) {
            parser2.parse()
        }
        assertTrue(validationException.print(parser2).toString().isNotBlank())

        val source3 = """
            (module "unsupported/module"
                {[name "pht/unsupported"]}
                {[vers "1.0.0"]}
            )
        """.trimIndent()
        val parser3 = Parser(Lexer(source3))
        val unsupportedException = assertThrows(ParserException.Wrapped.FromParser::class.java) {
            parser3.parse()
        }
        assertTrue(unsupportedException.print(parser3).toString().isNotBlank())

        val source4 = """
            (module "pht/module"
                {[name "pht/unexpected"]}
                {[vers "1.0.0"]}
                {
            )
        """.trimIndent()
        val parser4 = Parser(Lexer(source4))
        val unexpectedTokenException = assertThrows(ParserException.Wrapped.FromLexer::class.java) {
            parser4.parse()
        }
        assertTrue(unexpectedTokenException.print(parser4).toString().isNotBlank())
        
        // Test UnexpectedToken directly
        val source5 = "(module \"pht/module\" \"some string\")" // Parser expects ATTR_START or INSTR_END here, but gets STRING
        val lexer5 = Lexer(source5)
        lexer5.next() // (
        lexer5.next() // module
        val token0 = lexer5.next() // "pht/module"
        val token1 = lexer5.next() // This will be a STRING token
        val unexpectedTokenDirect = ParserException.UnexpectedToken(token1, listOf(Token.Type.ATTR_START))
        assertTrue(unexpectedTokenDirect.print(Parser(Lexer(source5))).toString().isNotBlank())

        // Test FromParser and FromLexer print methods
        val fromParser = ParserException.Wrapped.FromParser(ParserException.ExceptionWrapContext("Test stage", null), ParserException.NotInitializedException(token0, "testField"))
        assertTrue(fromParser.print(parser).toString().isNotBlank())

        val fromLexer = ParserException.Wrapped.FromLexer(ParserException.ExceptionWrapContext("Test stage", null), LexerException.EOF())
        assertTrue(fromLexer.print(parser).toString().isNotBlank())
    }
}