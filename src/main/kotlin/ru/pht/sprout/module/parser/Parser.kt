package ru.pht.sprout.module.parser

import ru.pht.sprout.module.Module
import ru.pht.sprout.module.lexer.Lexer
import ru.pht.sprout.module.lexer.LexerException
import ru.pht.sprout.module.lexer.Token
import ru.pht.sprout.module.lexer.Token.Type.*
import ru.pht.sprout.module.parser.ParserException.ExceptionWrapContext

/**
 * Парсер заголовков модулей.
 */
class Parser(val lexer: Lexer) {
    // ===== API ===== //

    @Throws(ParserException::class)
    fun parse(): Module {
        return parseModuleInstruction()
    }

    // ===== PARSE INSTRUCTION ===== //

    private fun parseModuleInstruction(): Module =
        wrapException(null, "Парсинг заголовка") {
            popHeadTk(INSTR_START) // (
            popTk(ID_MODULE) // module
            val token = popTk(STRING)
            return when (token.value) {
                "pht/module" -> parsePhtModuleInstruction()
                // todo: Поддержка кастомных модулей
                else -> throw ParserException.Unsupported(token, "Неподдерживаемый формат модуля")
            }
        }

    // INSTR_START, ID_MODULE, STRING - на момент вызова должны быть получены и проверены.
    private fun ExceptionWrapContext.parsePhtModuleInstruction(): Module =
        wrapExceptionSaveCtx("Парсинг заголовка 'pht/module'") {
            val module = Module.Builder()
            while (true) {
                val token = popTk()
                when (token.type) {
                    ATTR_START -> parseModuleAttribute(token, module) // {[
                    INSTR_END -> break // )
                    else -> throw ParserException.UnexpectedToken(token, listOf(ATTR_START, INSTR_END))
                }
            }
            return module.build()
        }

    // INSTR_START - на момент вызова должен быть получен и проверен.
    private fun parseDependency(token: Token): Module.Dependency =
        wrapException(token, "Парсинг зависимости") {
            val dependency = Module.Dependency.Builder()
            popTk(ID_MODULE) // module
            while (true) {
                val token = popTk()
                when (token.type) {
                    ATTR_START -> parseDependencyAttribute(token, dependency) // {[
                    INSTR_END -> break // )
                    else -> throw ParserException.UnexpectedToken(token, listOf(ATTR_START, INSTR_END))
                }
            }
            return dependency.build()
        }

    // ===== PARSE ATTRIBUTE ===== //

    // ATTR_START - на момент вызова должен быть получен и проверен.
    private fun parseModuleAttribute(token: Token, module: Module.Builder) =
        wrapException(token, "Парсинг аттрибутов модуля") {
            val token = popTk() // ID_X
            when (token.type) {
                ID_NAME -> module.name(parseNameString())
                ID_VERSION -> module.version(parseDefinitionVersionString())
                ID_DESCRIPTION -> module.description(parseString().second)
                ID_AUTHORS -> module.authors(parseStringList())
                ID_DEPENDENCIES -> module.dependencies(parseDependenciesList())
                ID_USES -> module.uses(parseStringList())
                ID_INJECT_FROM -> module.injectFrom(parseAllowDeny())
                ID_INJECT_INTO -> module.injectInto(parseAllowDeny())
                ID_INJECT_INTO_DEPENDENCIES -> module.injectIntoDependencies(parseAllowDeny())
                ID_IMPORTS -> module.imports(parseIntermoduleDataListOrAll())
                ID_EXPORTS -> module.exports(parseIntermoduleDataListOrAll())
                ID_FEATURES -> module.features(parseFeatureDefinitionList())
                ID_SOURCE -> module.sources(parsePathOrDependenciesList())
                ID_RESOURCE -> module.resources(parsePathOrDependenciesList())
                ID_PLUGIN -> module.plugins(parsePathOrDependenciesList())
                else -> throw ParserException.UnexpectedToken(token, listOf(ID_NAME, ID_VERSION, ID_DESCRIPTION, ID_AUTHORS, ID_DEPENDENCIES, ID_USES, ID_INJECT_FROM, ID_INJECT_INTO, ID_INJECT_INTO_DEPENDENCIES, ID_IMPORTS, ID_EXPORTS, ID_FEATURES, ID_SOURCE, ID_RESOURCE, ID_PLUGINS))
            }
            popTk(ATTR_END) // ]}
        }

    // ATTR_START - на момент вызова должен быть получен и проверен.
    private fun parseDependencyAttribute(token: Token, dependency: Module.Dependency.Builder) =
        wrapException(token, "Парсинг аттрибутов зависимости") {
            val token = popTk() // ID_X
            when (token.type) {
                ID_NAME -> dependency.name(parseNameString())
                ID_VERSION -> dependency.version(parseVersionStringOrAny())
                ID_USES -> dependency.uses(parseAllowDeny())
                ID_ADAPTERS -> dependency.adapters(parseNamesListOrAny())
                ID_INJECT_INTO -> dependency.injectInto(parseAllowDeny())
                ID_INJECT_INTO_DEPENDENCIES -> dependency.injectIntoDependencies(parseAllowDeny())
                ID_INJECT_FROM -> dependency.injectFrom(parseAllowDeny())
                ID_FEATURES -> dependency.features(parseNamesListOrAny())
                ID_NO_FEATURES -> dependency.disableFeatures(parseNamesListOrAny())
                else -> throw ParserException.UnexpectedToken(token, listOf(ID_NAME, ID_VERSION, ID_USES, ID_ADAPTERS, ID_INJECT_INTO, ID_INJECT_INTO_DEPENDENCIES, ID_INJECT_FROM, ID_FEATURES, ID_NO_FEATURES))
            }
            popTk(ATTR_END) // ]}
        }

    // ===== PARSE LIST ===== //

    // Никаких токенов на момент вызова не должен быть получено и проверено.
    private fun parseStringList(): List<String> {
        val list = ArrayList<String>()
        popTk(LIST_START) // [
        while (true) {
            val token = popTk()
            when (token.type) {
                STRING -> list += token.value
                LIST_END -> return list // ]
                else -> throw ParserException.UnexpectedToken(token, listOf(STRING, LIST_END))
            }
        }
    }

    // Никаких токенов на момент вызова не должен быть получено и проверено.
    private fun parseDependenciesList(): List<Module.Dependency> {
        val list = ArrayList<Module.Dependency>()
        popTk(LIST_START) // [
        while (true) {
            val token = popTk()
            list += when (token.type) {
                STRING -> Module.Dependency.Builder().name(checkNameString(token)).build() // "module"
                INSTR_START -> parseDependency(token) // (module ...)
                LIST_END -> return list // ]
                else -> throw ParserException.UnexpectedToken(token, listOf(STRING, INSTR_START, LIST_END))
            }
        }
    }

    // Никаких токенов на момент вызова не должен быть получено и проверено.
    private fun parseIntermoduleDataListOrAll(): Module.ListOrAny<Module.IntermoduleData> {
        val token = popTk()
        when (token.type) {
            ANY -> return Module.ListOrAny.any() // [*]
            LIST_START -> { // [
                val list = ArrayList<Module.IntermoduleData>()
                while (true) {
                    val token = popTk()
                    list += when (token.type) {
                        ID_ADAPTERS ->  Module.IntermoduleData.ADAPTERS // adapters
                        ID_PLUGINS ->  Module.IntermoduleData.PLUGINS // plugins
                        ID_MACROS -> Module.IntermoduleData.MACROS // macros
                        ID_TYPES -> Module.IntermoduleData.TYPES // types
                        ID_FUNCTIONS -> Module.IntermoduleData.FUNCTIONS // functions
                        LIST_END -> return Module.ListOrAny.ofList(list) // ]
                        else -> throw ParserException.UnexpectedToken(token, listOf(ID_ADAPTERS, ID_PLUGINS, ID_MACROS, ID_TYPES, ID_FUNCTIONS, LIST_END))
                    }
                }
            }
            else -> throw ParserException.UnexpectedToken(token, listOf(ANY, LIST_START))
        }
    }

    // Никаких токенов на момент вызова не должен быть получено и проверено.
    private fun parsePathOrDependenciesList(): List<Module.PathOrDependency> {
        val list = ArrayList<Module.PathOrDependency>()
        popTk(LIST_START) // [
        while (true) {
            val token = popTk()
            list += when (token.type) {
                STRING -> Module.PathOrDependency.ofPath(checkPathString(token)) // "src/*"
                INSTR_START -> Module.PathOrDependency.ofDependency(parseDependency(token)) // (module ...)
                LIST_END -> return list // ]
                else -> throw ParserException.UnexpectedToken(token, listOf(STRING, INSTR_START, LIST_END))
            }
        }
    }

    private fun parseNamesListOrAny(): Module.ListOrAny<String> {
        val token = popTk()
        when (token.type) {
            ANY -> return Module.ListOrAny.any() // [*]
            LIST_START -> { // [
                val list = ArrayList<String>()
                while (true) {
                    val token = popTk()
                    when (token.type) {
                        STRING -> list += checkNameString(token) // "name"
                        LIST_END -> return Module.ListOrAny.ofList(list) // ]
                        else -> throw ParserException.UnexpectedToken(token, listOf(STRING, LIST_END))
                    }
                }
            }
            else -> throw ParserException.UnexpectedToken(token, listOf(ANY, LIST_START))
        }
    }

    // Никаких токенов на момент вызова не должен быть получено и проверено.
    private fun parseFeatureDefinitionList(): List<Pair<String, Boolean>> {
        val list = ArrayList<Pair<String, Boolean>>()
        popTk(LIST_START) // [
        while (true) {
            val token = popTk()
            when (token.type) {
                LIST_START -> { // [
                    val feature = parseNameString() // "feature"
                    val token = popTk()
                    val default = when (token.type) {
                        ID_DEFAULT -> true
                        ID_OPTIONAL -> false
                        else -> throw ParserException.UnexpectedToken(token, listOf(ID_DEFAULT, ID_OPTIONAL))
                    }
                    list += Pair(feature, default)
                    popTk(LIST_END)
                }
                LIST_END -> return list // ]
                else -> throw ParserException.UnexpectedToken(token, listOf(LIST_START, LIST_END))
            }
        }
    }

    // ===== PARSE PRIMITIVE ===== //

    // Никаких токенов на момент вызова не должен быть получено и проверено.
    private fun parseAllowDeny(): Boolean {
        val token = popTk()
        return when (token.type) {
            ID_ALLOW -> true
            ID_DENY -> false
            else -> throw ParserException.UnexpectedToken(token, listOf(ID_ALLOW, ID_DENY))
        }
    }

    private fun parseNameString(): String {
        val (token, string) = parseString()
        return checkNameString(token, string)
    }

    private fun parseDefinitionVersionString(): String {
        val (token, string) = parseString()
        if (Regex("^\\d+(?:\\.\\d+)*(?:-[a-zA-Z-]+)?$").matches(string))
            return string
        throw ParserException.ValidationException(token, string)
    }

    private fun parseVersionStringOrAny(): Module.StringOrAny {
        val token = popTk()
        return when (token.type) {
            ANY -> Module.StringOrAny.ANY // [*]
            STRING -> Module.StringOrAny.ofString(checkDependencyVersionString(token)) // "1.0.0"
            else -> throw ParserException.UnexpectedToken(token, listOf(ANY, STRING))
        }
    }

    private fun parseString(): Pair<Token, String> {
        val token = popTk(STRING)
        return Pair(token, token.value)
    }

    // ===== CHECK UTILS ===== //

    private fun checkNameString(token: Token): String =
        checkNameString(token, token.value)

    private fun checkNameString(token: Token?, string: String): String {
        if (Regex("^(?=.*[A-Za-z0-9_])[A-Za-z0-9_]+(?:[-/][A-Za-z0-9_]+)*$").matches(string))
            return string
        throw ParserException.ValidationException(token, string)
    }

    private fun checkDependencyVersionString(token: Token): String =
        checkDependencyVersionString(token, token.value)

    private fun checkDependencyVersionString(token: Token?, string: String): String {
        if (Regex("^\\d+(?:\\.\\d+)*(?:\\.\\+)?(?:-[a-zA-Z-]+)?$").matches(string))
            return string
        throw ParserException.ValidationException(token, string)
    }

    private fun checkPathString(token: Token): String =
        checkPathString(token, token.value)

    private fun checkPathString(token: Token?, string: String): String {
        if (Regex("^(?!\\.$)(?!.*/\\.$)([\\w.-]+/)*(\\*\\.\\w+|\\*|[\\w.-]+)$").matches(string))
            return string
        throw ParserException.ValidationException(token, string)
    }

    // ===== TOKEN UTILS ===== //

    private fun popTk(type: Token.Type): Token {
        val token = this.lexer.next()
        if (token.type == type)
            return token
        throw ParserException.UnexpectedToken(token, type)
    }

    private fun popTk(): Token =
        this.lexer.next()

    // ===== EXCEPTION WRAPPING ===== //

    private fun ExceptionWrapContext.popHeadTk(type: Token.Type): Token {
        val token = this@Parser.lexer.next()
        if (token.type == type) {
            this@popHeadTk.lastToken = token
            return token
        }
        throw ParserException.UnexpectedToken(token, type)
    }

    private inline fun <T> ExceptionWrapContext.wrapExceptionSaveCtx(stage: String, block: ExceptionWrapContext.() -> T): T {
        this@wrapExceptionSaveCtx.stage = stage
        return block(this@wrapExceptionSaveCtx)
    }

    private inline fun <T> wrapException(token: Token? = null, stage: String, block: ExceptionWrapContext.() -> T): T {
        val context = ExceptionWrapContext(stage, token)
        try {
            return block(context)
        } catch (exception: LexerException) {
            throw ParserException.Wrapped.FromLexer(context, exception)
        } catch (exception: ParserException) {
            throw ParserException.Wrapped.FromParser(context, exception)
        }
    }
}