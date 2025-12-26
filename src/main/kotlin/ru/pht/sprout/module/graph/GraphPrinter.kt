package ru.pht.sprout.module.graph

import io.github.z4kn4fein.semver.constraints.Constraint
import ru.DmN.cmd.style.FmtUtils.fmt
import ru.pht.sprout.module.header.ModuleHeader
import java.util.*

class GraphPrinter(val graph: GraphNode, val formatting: Boolean) {
    private val printing: Stack<Pair<GraphNode, Int>> = Stack()
    private val text = StringBuilder()
    private val output = lazy {
        printNode(this.graph, null, null)
        this.text.deleteAt(this.text.lastIndex)
        if (this.formatting)
            this.text.fmt
        else this.text.toString()
    }

    fun print(): String {
        return this.output.value
    }

    private fun printNode(node: GraphNode, original: ModuleHeader?, version: Constraint?) {
        this.text.appendIndent()
        if (node.modules.size == 1) {
            this.text.append(node.modules.first(), version)
        } else {
            this.text.append(if (this.formatting) "§f1[combine]§sr " else "[combine] ")
            for (i in 0 until node.modules.lastIndex) {
                this.text
                    .append(node.modules[i], original, version)
                    .append(if (this.formatting) " §sb|§sr " else " | ")
            }
            this.text.append(node.modules.last(), original, version)
        }
        this.text.append('\n')
        for (i in node.dependencies.indices) {
            val dependency = node.dependencies[i]
            this.printing.push(Pair(node, i))
            if (this.printing.any { (it, _) -> it == dependency.node }) {
                this.text
                    .appendIndent()
                    .append(if (this.formatting) "§f5[cyclic]§sr " else "[cyclic] ")
                    .append(dependency.original.name)
                    .append(if (this.formatting) "§sb@§sr" else "@")
                    .append(dependency.original.version)
                    .append(if (this.formatting) " (§f3" else " (")
                    .append(dependency.dependency.version)
                    .append(if (this.formatting) "§sr)\n" else ")\n")
            } else printNode(dependency.node, dependency.original, dependency.dependency.version)
            this.printing.pop()
        }
    }

    private fun StringBuilder.append(module: ModuleHeader, original: ModuleHeader?, version: Constraint?): StringBuilder {
        if (formatting) {
            if (module == original) {
                append("§sb").append(module.name).append('@').append(module.version).append("§sr")
                append(" (§f3").append(version!!).append("§sr)")
            } else append(module.name).append("§sb@§sr").append(module.version)
        } else append(module.name).append('@').append(module.version)
        return this
    }

    private fun StringBuilder.append(module: ModuleHeader, version: Constraint?): StringBuilder {
        append(module.name)
        append(if (formatting) "§sr§sb@§sr" else "@")
        append(module.version)
        if (version != null) {
            append(if (formatting) " (§f3" else " (")
            append(version)
            append(if (formatting) "§sr)" else ")")
        }
        return this
    }

    private fun StringBuilder.appendIndent(): StringBuilder {
        if (printing.isEmpty())
            return this
        if (formatting)
            append("§f7")
        for (i in 0 until printing.lastIndex) {
            val (node, index) = printing[i]
            append(if (node.dependencies.lastIndex == index) ' ' else '│')
            append("   ")
        }
        val (node, index) = printing.last()
        append(if (node.dependencies.lastIndex == index) '└' else '├')
        append("── ")
        if (formatting)
            append("§sr")
        return this
    }
}