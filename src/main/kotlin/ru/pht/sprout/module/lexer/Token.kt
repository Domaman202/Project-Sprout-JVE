package ru.pht.sprout.module.lexer

class Token(val position: Position, val type: Type, val value: String) {
    data class Position(
        val start: Int,
        val end: Int,
        val line: Int,
        val column: Int
    )

    enum class Type {
        // ()
        INSTR_START,
        INSTR_END,

        // {[]}
        ATTR_START,
        ATTR_END,

        // []
        LIST_START,
        LIST_END,

        // [*]
        ANY,
        // "text"
        STRING,

        // module
        ID_MODULE,

        // name
        ID_NAME,
        // vers
        ID_VERSION,

        // desc
        ID_DESCRIPTION,
        // auth
        ID_AUTHORS,

        // deps
        ID_DEPENDENCIES,
        // uses
        ID_USES,

        // inject-from
        ID_INJECT_FROM,
        // inject-into
        ID_INJECT_INTO,
        // inject-into-deps
        ID_INJECT_INTO_DEPENDENCIES,

        // features
        ID_FEATURES,
        // no-features
        ID_NO_FEATURES,

        // imports
        ID_IMPORTS,
        // exports
        ID_EXPORTS,

        // src
        ID_SOURCE,
        // res
        ID_RESOURCE,
        // plg
        ID_PLUGIN,

        // default
        ID_DEFAULT,
        // optional
        ID_OPTIONAL,

        // allow
        ID_ALLOW,
        // deny
        ID_DENY,

        // plugins
        ID_PLUGINS,
        // adapters
        ID_ADAPTERS,
        // macros
        ID_MACROS,
        // types
        ID_TYPES,
        // functions
        ID_FUNCTIONS
    }
}