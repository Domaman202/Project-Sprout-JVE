package ru.pht.sprout.module.graph

import ru.pht.sprout.module.header.ModuleHeader

class GraphNode internal constructor(
    modules: MutableList<ModuleHeader>,
    dependencies: MutableList<Dependency>,
    dependent: MutableList<Dependency>
) {
    internal val modules0: MutableList<ModuleHeader> = modules
    val modules: List<ModuleHeader>
        get() = this.modules0
    internal val dependencies0: MutableList<Dependency> = dependencies
    val dependencies: List<Dependency>
        get() = this.dependencies0
    internal val dependent0: MutableList<Dependency> = dependent
    val dependent: List<Dependency>
        get() = this.dependent0

    class Dependency internal constructor(
        node: GraphNode,
        val original: ModuleHeader,
        val dependency: ModuleHeader.Dependency
    ) {
        var node = node
            internal set
    }
}