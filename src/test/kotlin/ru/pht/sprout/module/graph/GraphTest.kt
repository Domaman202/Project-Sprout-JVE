package ru.pht.sprout.module.graph

import io.github.z4kn4fein.semver.constraints.toConstraint
import io.github.z4kn4fein.semver.toVersion
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.condition.EnabledIf
import ru.DmN.cmd.style.FmtUtils.fmt
import ru.pht.sprout.module.header.ModuleHeader
import kotlin.test.Test
import kotlin.test.assertEquals

@EnabledIf("ru.pht.sprout.TestConfigInternal#graphTest", disabledReason = "Тест выключен конфигурацией")
class GraphTest {
    @Nested
    inner class Linear {
        @Test
        @DisplayName("Никаких зависимостей")
        fun emptyNoFmtTest() {
            val moduleA = module("example/a")
            val graphBuilder = GraphBuilder(moduleA)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    example/a@1.0.0
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Никаких зависимостей")
        fun emptyTest() {
            val moduleA = module("example/a")
            val graphBuilder = GraphBuilder(moduleA)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    example/a§sr§sb@§sr1.0.0
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Список зависимостей")
        fun listNoFmtTest() {
            val moduleA = module("example/a", listOf(dependency("example/b"), dependency("example/c"), dependency("example/d")))
            val moduleB = module("example/b")
            val moduleC = module("example/c")
            val moduleD = module("example/d")
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC, moduleD)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    example/a@1.0.0
                    ├── example/b@1.0.0 (>=0.0.0)
                    ├── example/c@1.0.0 (>=0.0.0)
                    └── example/d@1.0.0 (>=0.0.0)
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Список зависимостей")
        fun listTest() {
            val moduleA = module("example/a", listOf(dependency("example/b"), dependency("example/c"), dependency("example/d")))
            val moduleB = module("example/b")
            val moduleC = module("example/c")
            val moduleD = module("example/d")
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC, moduleD)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    example/a§sr§sb@§sr1.0.0
                    §f7├── §srexample/b§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7├── §srexample/c§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7└── §srexample/d§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Линейная цепочка зависимостей")
        fun linearChainNoFmtTest() {
            val moduleA = module("example/a", listOf(dependency("example/b")))
            val moduleB = module("example/b", listOf(dependency("example/c")))
            val moduleC = module("example/c")
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    example/a@1.0.0
                    └── example/b@1.0.0 (>=0.0.0)
                        └── example/c@1.0.0 (>=0.0.0)
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Линейная цепочка зависимостей")
        fun linearChainTest() {
            val moduleA = module("example/a", listOf(dependency("example/b")))
            val moduleB = module("example/b", listOf(dependency("example/c")))
            val moduleC = module("example/c")
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    example/a§sr§sb@§sr1.0.0
                    §f7└── §srexample/b§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7    └── §srexample/c§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Линейная цепочка с разделением на двое")
        fun linearChainTwoSplitNoFmtTest() {
            val moduleA = module("example/a", listOf(dependency("example/b"), dependency("example/c")))
            val moduleB = module("example/b", listOf(dependency("example/d")))
            val moduleC = module("example/c", listOf(dependency("example/e")))
            val moduleD = module("example/d")
            val moduleE = module("example/e")
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC, moduleD, moduleE)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    example/a@1.0.0
                    ├── example/b@1.0.0 (>=0.0.0)
                    │   └── example/d@1.0.0 (>=0.0.0)
                    └── example/c@1.0.0 (>=0.0.0)
                        └── example/e@1.0.0 (>=0.0.0)
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Линейная цепочка с разделением на двое")
        fun linearChainTwoSplitTest() {
            val moduleA = module("example/a", listOf(dependency("example/b"), dependency("example/c")))
            val moduleB = module("example/b", listOf(dependency("example/d")))
            val moduleC = module("example/c", listOf(dependency("example/e")))
            val moduleD = module("example/d")
            val moduleE = module("example/e")
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC, moduleD, moduleE)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    example/a§sr§sb@§sr1.0.0
                    §f7├── §srexample/b§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7│   └── §srexample/d§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7└── §srexample/c§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7    └── §srexample/e§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с одним элементом")
        fun cyclicChainOneElementNoFmtTest() {
            val moduleA = module("example/a", listOf(dependency("example/a")))
            val graphBuilder = GraphBuilder(moduleA)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    example/a@1.0.0
                    └── [cyclic] example/a@1.0.0 (>=0.0.0)
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с одним элементом")
        fun cyclicChainOneElementTest() {
            val moduleA = module("example/a", listOf(dependency("example/a")))
            val graphBuilder = GraphBuilder(moduleA)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    example/a§sr§sb@§sr1.0.0
                    §f7└── §sr§f5[cyclic]§sr example/a§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с двумя элементами")
        fun cyclicChainTwoElementsNoFmtTest() {
            val moduleA = module("example/a", listOf(dependency("example/b")))
            val moduleB = module("example/b", listOf(dependency("example/a")))
            val graphBuilder = GraphBuilder(moduleA, moduleB)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    example/a@1.0.0
                    └── example/b@1.0.0 (>=0.0.0)
                        └── [cyclic] example/a@1.0.0 (>=0.0.0)
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с двумя элементами")
        fun cyclicChainTwoElementsTest() {
            val moduleA = module("example/a", listOf(dependency("example/b")))
            val moduleB = module("example/b", listOf(dependency("example/a")))
            val graphBuilder = GraphBuilder(moduleA, moduleB)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    example/a§sr§sb@§sr1.0.0
                    §f7└── §srexample/b§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7    └── §sr§f5[cyclic]§sr example/a§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с тремя элементами")
        fun cyclicChainThreeElementsNoFmtTest() {
            val moduleA = module("example/a", listOf(dependency("example/b")))
            val moduleB = module("example/b", listOf(dependency("example/c")))
            val moduleC = module("example/c", listOf(dependency("example/a")))
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    example/a@1.0.0
                    └── example/b@1.0.0 (>=0.0.0)
                        └── example/c@1.0.0 (>=0.0.0)
                            └── [cyclic] example/a@1.0.0 (>=0.0.0)
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с тремя элементами")
        fun cyclicChainThreeElementsTest() {
            val moduleA = module("example/a", listOf(dependency("example/b")))
            val moduleB = module("example/b", listOf(dependency("example/c")))
            val moduleC = module("example/c", listOf(dependency("example/a")))
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    example/a§sr§sb@§sr1.0.0
                    §f7└── §srexample/b§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7    └── §srexample/c§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7        └── §sr§f5[cyclic]§sr example/a§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с двумя зависимостями")
        fun cyclicChainToTwoSplitNoFmtTest() {
            val moduleA = module("example/a", listOf(dependency("example/b")))
            val moduleB = module("example/b", listOf(dependency("example/c"), dependency("example/d")))
            val moduleC = module("example/c", listOf(dependency("example/b"), dependency("example/e")))
            val moduleD = module("example/d")
            val moduleE = module("example/e")
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC, moduleE, moduleD)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    example/a@1.0.0
                    └── example/b@1.0.0 (>=0.0.0)
                        ├── example/c@1.0.0 (>=0.0.0)
                        │   ├── [cyclic] example/b@1.0.0 (>=0.0.0)
                        │   └── example/e@1.0.0 (>=0.0.0)
                        └── example/d@1.0.0 (>=0.0.0)
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с двумя зависимостями")
        fun cyclicChainToTwoSplitTest() {
            val moduleA = module("example/a", listOf(dependency("example/b")))
            val moduleB = module("example/b", listOf(dependency("example/c"), dependency("example/d")))
            val moduleC = module("example/c", listOf(dependency("example/b"), dependency("example/e")))
            val moduleD = module("example/d")
            val moduleE = module("example/e")
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC, moduleE, moduleD)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    example/a§sr§sb@§sr1.0.0
                    §f7└── §srexample/b§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7    ├── §srexample/c§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7    │   ├── §sr§f5[cyclic]§sr example/b§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7    │   └── §srexample/e§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7    └── §srexample/d§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с двумя зависимыми")
        fun linearTwoSplitToCyclicChainNoFmtTest() {
            val moduleA = module("example/a", listOf(dependency("example/b"), dependency("example/c")))
            val moduleB = module("example/b", listOf(dependency("example/d")))
            val moduleC = module("example/c", listOf(dependency("example/e")))
            val moduleD = module("example/d", listOf(dependency("example/e")))
            val moduleE = module("example/e", listOf(dependency("example/d")))
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC, moduleE, moduleD)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    example/a@1.0.0
                    ├── example/b@1.0.0 (>=0.0.0)
                    │   └── example/d@1.0.0 (>=0.0.0)
                    │       └── example/e@1.0.0 (>=0.0.0)
                    │           └── [cyclic] example/d@1.0.0 (>=0.0.0)
                    └── example/c@1.0.0 (>=0.0.0)
                        └── example/e@1.0.0 (>=0.0.0)
                            └── example/d@1.0.0 (>=0.0.0)
                                └── [cyclic] example/e@1.0.0 (>=0.0.0)
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с двумя зависимыми")
        fun linearTwoSplitToCyclicChainTest() {
            val moduleA = module("example/a", listOf(dependency("example/b"), dependency("example/c")))
            val moduleB = module("example/b", listOf(dependency("example/d")))
            val moduleC = module("example/c", listOf(dependency("example/e")))
            val moduleD = module("example/d", listOf(dependency("example/e")))
            val moduleE = module("example/e", listOf(dependency("example/d")))
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC, moduleE, moduleD)
            val graph = graphBuilder.buildLinear(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    example/a§sr§sb@§sr1.0.0
                    §f7├── §srexample/b§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7│   └── §srexample/d§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7│       └── §srexample/e§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7│           └── §sr§f5[cyclic]§sr example/d§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7└── §srexample/c§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7    └── §srexample/e§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7        └── §srexample/d§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7            └── §sr§f5[cyclic]§sr example/e§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }
    }

    @Nested
    inner class Combine {
        @Test
        @DisplayName("Никаких зависимостей")
        fun emptyNoFmtTest() {
            val moduleA = module("example/a")
            val graphBuilder = GraphBuilder(moduleA)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    example/a@1.0.0
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Никаких зависимостей")
        fun emptyTest() {
            val moduleA = module("example/a")
            val graphBuilder = GraphBuilder(moduleA)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    example/a§sr§sb@§sr1.0.0
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Список зависимостей")
        fun listNoFmtTest() {
            val moduleA = module("example/a", listOf(dependency("example/b"), dependency("example/c"), dependency("example/d")))
            val moduleB = module("example/b")
            val moduleC = module("example/c")
            val moduleD = module("example/d")
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC, moduleD)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    example/a@1.0.0
                    ├── example/b@1.0.0 (>=0.0.0)
                    ├── example/c@1.0.0 (>=0.0.0)
                    └── example/d@1.0.0 (>=0.0.0)
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Список зависимостей")
        fun listTest() {
            val moduleA = module("example/a", listOf(dependency("example/b"), dependency("example/c"), dependency("example/d")))
            val moduleB = module("example/b")
            val moduleC = module("example/c")
            val moduleD = module("example/d")
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC, moduleD)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    example/a§sr§sb@§sr1.0.0
                    §f7├── §srexample/b§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7├── §srexample/c§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7└── §srexample/d§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Линейная цепочка зависимостей")
        fun linearChainNoFmtTest() {
            val moduleA = module("example/a", listOf(dependency("example/b")))
            val moduleB = module("example/b", listOf(dependency("example/c")))
            val moduleC = module("example/c")
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    example/a@1.0.0
                    └── example/b@1.0.0 (>=0.0.0)
                        └── example/c@1.0.0 (>=0.0.0)
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Линейная цепочка зависимостей")
        fun linearChainTest() {
            val moduleA = module("example/a", listOf(dependency("example/b")))
            val moduleB = module("example/b", listOf(dependency("example/c")))
            val moduleC = module("example/c")
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    example/a§sr§sb@§sr1.0.0
                    §f7└── §srexample/b§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7    └── §srexample/c§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Линейная цепочка с разделением на двое")
        fun linearChainTwoSplitNoFmtTest() {
            val moduleA = module("example/a", listOf(dependency("example/b"), dependency("example/c")))
            val moduleB = module("example/b", listOf(dependency("example/d")))
            val moduleC = module("example/c", listOf(dependency("example/e")))
            val moduleD = module("example/d")
            val moduleE = module("example/e")
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC, moduleD, moduleE)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    example/a@1.0.0
                    ├── example/b@1.0.0 (>=0.0.0)
                    │   └── example/d@1.0.0 (>=0.0.0)
                    └── example/c@1.0.0 (>=0.0.0)
                        └── example/e@1.0.0 (>=0.0.0)
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Линейная цепочка с разделением на двое")
        fun linearChainTwoSplitTest() {
            val moduleA = module("example/a", listOf(dependency("example/b"), dependency("example/c")))
            val moduleB = module("example/b", listOf(dependency("example/d")))
            val moduleC = module("example/c", listOf(dependency("example/e")))
            val moduleD = module("example/d")
            val moduleE = module("example/e")
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC, moduleD, moduleE)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    example/a§sr§sb@§sr1.0.0
                    §f7├── §srexample/b§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7│   └── §srexample/d§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7└── §srexample/c§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7    └── §srexample/e§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с одним элементом")
        fun cyclicChainOneElementNoFmtTest() {
            val moduleA = module("example/a", listOf(dependency("example/a")))
            val graphBuilder = GraphBuilder(moduleA)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    example/a@1.0.0
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с одним элементом")
        fun cyclicChainOneElementTest() {
            val moduleA = module("example/a", listOf(dependency("example/a")))
            val graphBuilder = GraphBuilder(moduleA)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    example/a§sr§sb@§sr1.0.0
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с двумя элементами")
        fun cyclicChainTwoElementsNoFmtTest() {
            val moduleA = module("example/a", listOf(dependency("example/b")))
            val moduleB = module("example/b", listOf(dependency("example/a")))
            val graphBuilder = GraphBuilder(moduleA, moduleB)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    [combine] example/a@1.0.0 | example/b@1.0.0
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с двумя элементами")
        fun cyclicChainTwoElementsTest() {
            val moduleA = module("example/a", listOf(dependency("example/b")))
            val moduleB = module("example/b", listOf(dependency("example/a")))
            val graphBuilder = GraphBuilder(moduleA, moduleB)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    §f1[combine]§sr example/a§sb@§sr1.0.0 §sb|§sr example/b§sb@§sr1.0.0
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с тремя элементами")
        fun cyclicChainThreeElementsNoFmtTest() {
            val moduleA = module("example/a", listOf(dependency("example/b")))
            val moduleB = module("example/b", listOf(dependency("example/c")))
            val moduleC = module("example/c", listOf(dependency("example/a")))
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    [combine] example/a@1.0.0 | example/c@1.0.0 | example/b@1.0.0
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с тремя элементами")
        fun cyclicChainThreeElementsTest() {
            val moduleA = module("example/a", listOf(dependency("example/b")))
            val moduleB = module("example/b", listOf(dependency("example/c")))
            val moduleC = module("example/c", listOf(dependency("example/a")))
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    §f1[combine]§sr example/a§sb@§sr1.0.0 §sb|§sr example/c§sb@§sr1.0.0 §sb|§sr example/b§sb@§sr1.0.0
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с двумя зависимостями")
        fun cyclicChainToTwoSplitNoFmtTest() {
            val moduleA = module("example/a", listOf(dependency("example/b")))
            val moduleB = module("example/b", listOf(dependency("example/c"), dependency("example/d")))
            val moduleC = module("example/c", listOf(dependency("example/b"), dependency("example/e")))
            val moduleD = module("example/d")
            val moduleE = module("example/e")
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC, moduleE, moduleD)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    example/a@1.0.0
                    └── [combine] example/b@1.0.0 | example/c@1.0.0
                        ├── example/d@1.0.0 (>=0.0.0)
                        └── example/e@1.0.0 (>=0.0.0)
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с двумя зависимостями")
        fun cyclicChainToTwoSplitTest() {
            val moduleA = module("example/a", listOf(dependency("example/b")))
            val moduleB = module("example/b", listOf(dependency("example/c"), dependency("example/d")))
            val moduleC = module("example/c", listOf(dependency("example/b"), dependency("example/e")))
            val moduleD = module("example/d")
            val moduleE = module("example/e")
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC, moduleE, moduleD)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    example/a§sr§sb@§sr1.0.0
                    §f7└── §sr§f1[combine]§sr §sbexample/b@1.0.0§sr (§f3>=0.0.0§sr) §sb|§sr example/c§sb@§sr1.0.0
                    §f7    ├── §srexample/d§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7    └── §srexample/e§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с двумя зависимыми")
        fun linearTwoSplitToCyclicChainNoFmtTest() {
            val moduleA = module("example/a", listOf(dependency("example/b"), dependency("example/c")))
            val moduleB = module("example/b", listOf(dependency("example/d")))
            val moduleC = module("example/c", listOf(dependency("example/e")))
            val moduleD = module("example/d", listOf(dependency("example/e")))
            val moduleE = module("example/e", listOf(dependency("example/d")))
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC, moduleE, moduleD)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, false)
            assertEquals(
                """
                    example/a@1.0.0
                    ├── example/b@1.0.0 (>=0.0.0)
                    │   └── [combine] example/d@1.0.0 | example/e@1.0.0
                    └── example/c@1.0.0 (>=0.0.0)
                        └── [combine] example/d@1.0.0 | example/e@1.0.0
                """.trimIndent(),
                graphPrinter.print()
            )
        }

        @Test
        @DisplayName("Циклическая цепочка с двумя зависимыми")
        fun linearTwoSplitToCyclicChainTest() {
            val moduleA = module("example/a", listOf(dependency("example/b"), dependency("example/c")))
            val moduleB = module("example/b", listOf(dependency("example/d")))
            val moduleC = module("example/c", listOf(dependency("example/e")))
            val moduleD = module("example/d", listOf(dependency("example/e")))
            val moduleE = module("example/e", listOf(dependency("example/d")))
            val graphBuilder = GraphBuilder(moduleA, moduleB, moduleC, moduleE, moduleD)
            val graph = graphBuilder.buildCombine(moduleA)
            val graphPrinter = GraphPrinter(graph, true)
            assertEquals(
                """
                    example/a§sr§sb@§sr1.0.0
                    §f7├── §srexample/b§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7│   └── §sr§f1[combine]§sr §sbexample/d@1.0.0§sr (§f3>=0.0.0§sr) §sb|§sr example/e§sb@§sr1.0.0
                    §f7└── §srexample/c§sr§sb@§sr1.0.0 (§f3>=0.0.0§sr)
                    §f7    └── §sr§f1[combine]§sr example/d§sb@§sr1.0.0 §sb|§sr §sbexample/e@1.0.0§sr (§f3>=0.0.0§sr)
                """.trimIndent().fmt,
                graphPrinter.print()
            )
        }
    }

    private fun GraphBuilder(vararg modules: ModuleHeader): GraphBuilder {
        return GraphBuilder { (name, version) ->
            modules
                .find { it.name == name && version.isSatisfiedBy(it.version) }
                ?: throw RuntimeException("${name}@${version} not found")
        }
    }

    private fun module(name: String): ModuleHeader {
        return ModuleHeader.Builder()
            .name(name)
            .version("1.0.0".toVersion())
            .build()
    }

    private fun module(name: String, dependencies: List<ModuleHeader.Dependency>): ModuleHeader {
        return ModuleHeader.Builder()
            .name(name)
            .version("1.0.0".toVersion())
            .dependencies(dependencies)
            .build()
    }

    private fun dependency(name: String): ModuleHeader.Dependency {
        return ModuleHeader.Dependency.Builder()
            .name(name)
            .version("*".toConstraint())
            .build()
    }
}