package ru.pht.sprout.cli.args

import ru.pht.sprout.cli.lang.Translation

class Command(
    val definition: Definition,
    val arguments: List<CommandArgument>
) {
    class Definition(
        val short: String,
        val long: String,
        val arguments: Array<CommandArgument.Definition>,
        val description: Translation
    )
}