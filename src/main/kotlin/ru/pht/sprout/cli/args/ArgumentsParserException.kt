package ru.pht.sprout.cli.args

class ArgumentsParserException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}