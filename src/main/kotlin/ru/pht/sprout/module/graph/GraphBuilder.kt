package ru.pht.sprout.module.graph

import ru.pht.sprout.module.header.ModuleHeader
import java.util.*

class GraphBuilder(val resolver: (dependency: ModuleHeader.Dependency) -> ModuleHeader) {
    fun buildCombine(module: ModuleHeader): GraphNode {
        val graph = buildLineal(module, HashMap())
        while (true){
            if (buildCombine(graph, Stack()))
                continue
            break
        }
        return graph
    }

    fun buildLineal(module: ModuleHeader): GraphNode {
        return buildLineal(module, HashMap())
    }

    private fun buildCombine(last: GraphNode, parents: Stack<GraphNode>): Boolean {
        val loop = parents.find { it == last }
        if (loop != null) {
            while (true) {
                val pop = parents.pop()
                if (pop == loop && parents.none { it == last }) {
                    loop.dependencies0.removeIf { loop.modules.contains(it.original) }
                    loop.dependent0.forEach { it.node = loop }
                    return true
                }
                loop.modules0 += pop.modules
                loop.dependencies0 += pop.dependencies
                loop.dependent0 += pop.dependent
            }
        }

        parents.push(last)
        for (dependency in last.dependencies)
            if (buildCombine(dependency.node, parents))
                return true

        parents.pop()
        return false
    }

    private fun buildLineal(last: ModuleHeader, other: MutableMap<ModuleHeader, GraphNode>): GraphNode {
        return other[last] ?: run {
            val dependencies = ArrayList<GraphNode.Dependency>()
            GraphNode(mutableListOf(last), dependencies, ArrayList()).apply {
                other[last] = this
                for (dependency in last.dependencies) {
                    val module = resolver(dependency)
                    val graph = buildLineal(module, other)
                    GraphNode.Dependency(graph, module, dependency).let {
                        graph.dependent0 += it
                        dependencies += it
                    }
                }
            }
        }
    }
}