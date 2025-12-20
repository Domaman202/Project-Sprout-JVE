package ru.pht.sprout.cli.args

import ru.DmN.translate.TranslationPair

class Command(
    val definition: Definition,
    val arguments: List<CommandArgument>
) {
    fun argument(name: String): Any? =
        this.arguments.find { it.definition.name == name }?.value

    class Definition(
        val short: String?,
        val long: String,
        val arguments: Array<CommandArgument.Definition>,
        val description: TranslationPair?
    )
}