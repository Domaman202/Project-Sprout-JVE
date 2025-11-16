@file:Suppress("UNUSED")
package ru.pht.sprout.module

data class Module(
    val name: String,
    val version: String,
    val description: String,
    val authors: List<String>,
    val dependencies: List<Dependency>,
    val uses: List<String>,
    val injectInto: Boolean,
    val injectIntoDependencies: Boolean,
    val injectFrom: Boolean,
    val imports: List<IntermoduleData>,
    val exports: List<IntermoduleData>,
    val features: List<Pair<String, Boolean>>,
    val sources: List<PathOrDependency>,
    val resources: List<PathOrDependency>,
    val plugins: List<PathOrDependency>,
) {
    data class Dependency(
        val name: String,
        val version: StringOrAny,
        val uses: List<String>,
        val adapters: List<String>,
        val injectInto: Boolean,
        val injectIntoDependencies: Boolean,
        val injectFrom: Boolean,
        val features: StringListOrAny,
        val disableFeatures: StringListOrAny
    ) {
        class Builder {
            var name: String? = null
            var version: StringOrAny? = null
            var uses: List<String>? = null
            var adapters: List<String>? = null
            var injectInto: Boolean? = null
            var injectIntoDependencies: Boolean? = null
            var injectFrom: Boolean? = null
            var features: StringListOrAny? = null
            var disableFeatures: StringListOrAny? = null

            fun name(value: String): Builder {
                this.name = value
                return this
            }

            fun name(): String? {
                return this.name
            }

            fun version(value: StringOrAny): Builder {
                this.version = value
                return this
            }

            fun version(): StringOrAny? {
                return this.version
            }

            fun uses(value: List<String>): Builder {
                this.uses = value
                return this
            }

            fun uses(): List<String>? {
                return this.uses
            }

            fun adapters(value: List<String>): Builder {
                this.adapters = value
                return this
            }

            fun adapters(): List<String>? {
                return this.adapters
            }

            fun injectInto(value: Boolean): Builder {
                this.injectInto = value
                return this
            }

            fun injectInto(): Boolean? {
                return this.injectInto
            }

            fun injectIntoDependencies(value: Boolean): Builder {
                this.injectIntoDependencies = value
                return this
            }

            fun injectIntoDependencies(): Boolean? {
                return this.injectIntoDependencies
            }

            fun injectFrom(value: Boolean): Builder {
                this.injectFrom = value
                return this
            }

            fun injectFrom(): Boolean? {
                return this.injectFrom
            }

            fun features(value: StringListOrAny): Builder {
                this.features = value
                return this
            }

            fun features(): StringListOrAny? {
                return this.features
            }

            fun disableFeatures(value: StringListOrAny): Builder {
                this.disableFeatures = value
                return this
            }

            fun disableFeatures(): StringListOrAny? {
                return this.disableFeatures
            }

            fun build(): Dependency {
                return Dependency(
                    this.name ?: throw RuntimeException("Неинициализированное обязательное поле: name"),
                    this.version ?: StringOrAny.ANY,
                    this.uses ?: emptyList(),
                    this.adapters ?: emptyList(),
                    this.injectInto ?: false,
                    this.injectIntoDependencies ?: false,
                    this.injectFrom ?: false,
                    this.features ?: StringListOrAny.EMPTY,
                    this.disableFeatures ?: StringListOrAny.EMPTY,
                )
            }
        }
    }

    enum class IntermoduleData {
        PLUGINS,
        ADAPTERS,
        MACROS,
        TYPES,
        FUNCTIONS
    }

    class PathOrDependency private constructor(private val path: String?, private val dependency: Dependency?) {
        val isPath: Boolean
            get() = this.path != null
        val isDependency: Boolean
            get() = this.path == null

        fun path(): String {
            return this.path!!
        }

        fun dependency(): Dependency {
            return this.dependency!!
        }

        companion object {
            fun ofDirectory(directory: String): PathOrDependency {
                return PathOrDependency(directory, null)
            }

            fun ofDependency(dependency: Dependency): PathOrDependency {
                return PathOrDependency(null, dependency)
            }
        }
    }

    class StringOrAny private constructor(private val string: String?, private val any: Boolean) {
        val isString: Boolean
            get() = !this.any
        val isAny: Boolean
            get() = this.any

        fun string(): String {
            return this.string!!
        }

        companion object {
            val ANY = StringOrAny(null, true)

            fun ofString(string: String): StringOrAny {
                return StringOrAny(string, false)
            }
        }
    }

    class StringListOrAny private constructor(private val list: List<String>?, private val any: Boolean) {
        val isList: Boolean
            get() = !this.any
        val isAny: Boolean
            get() = this.any

        fun list(): List<String> {
            return this.list!!
        }

        companion object {
            val ANY = StringListOrAny(null, true)
            val EMPTY = StringListOrAny(emptyList(), false)

            fun ofList(list: List<String>): StringListOrAny {
                return StringListOrAny(list, false)
            }
        }
    }

    class Builder {
        var name: String? = null
        var version: String? = null
        var description: String? = null
        var authors: List<String>? = null
        var dependencies: List<Dependency>? = null
        var uses: List<String>? = null
        var injectInto: Boolean? = null
        var injectIntoDependencies: Boolean? = null
        var injectFrom: Boolean? = null
        var imports: List<IntermoduleData>? = null
        var exports: List<IntermoduleData>? = null
        var features: List<Pair<String, Boolean>>? = null
        var sources: List<PathOrDependency>? = null
        var resources: List<PathOrDependency>? = null
        var plugins: List<PathOrDependency>? = null

        fun name(value: String): Builder {
            this.name = value
            return this
        }

        fun name(): String? {
            return this.name
        }

        fun version(value: String): Builder {
            this.version = value
            return this
        }

        fun version(): String? {
            return this.version
        }

        fun description(value: String): Builder {
            this.description = value
            return this
        }

        fun description(): String? {
            return this.description
        }

        fun authors(value: List<String>): Builder {
            this.authors = value
            return this
        }

        fun authors(): List<String>? {
            return this.authors
        }

        fun dependencies(value: List<Dependency>): Builder {
            this.dependencies = value
            return this
        }

        fun dependencies(): List<Dependency>? {
            return this.dependencies
        }

        fun uses(value: List<String>): Builder {
            this.uses = value
            return this
        }

        fun uses(): List<String>? {
            return this.uses
        }

        fun imports(value: List<IntermoduleData>): Builder {
            this.imports = value
            return this
        }

        fun imports(): List<IntermoduleData>? {
            return this.imports
        }

        fun exports(value: List<IntermoduleData>): Builder {
            this.exports = value
            return this
        }

        fun exports(): List<IntermoduleData>? {
            return this.exports
        }

        fun features(value: List<Pair<String, Boolean>>): Builder {
            this.features = value
            return this
        }

        fun features(): List<Pair<String, Boolean>>? {
            return this.features
        }

        fun sources(value: List<PathOrDependency>): Builder {
            this.sources = value
            return this
        }

        fun sources(): List<PathOrDependency>? {
            return this.sources
        }

        fun resources(value: List<PathOrDependency>): Builder {
            this.resources = value
            return this
        }

        fun resources(): List<PathOrDependency>? {
            return this.resources
        }

        fun plugins(value: List<PathOrDependency>): Builder {
            this.plugins = value
            return this
        }

        fun plugins(): List<PathOrDependency>? {
            return this.plugins
        }

        fun build(): Module {
            return Module(
                this.name ?: throw RuntimeException("Неинициализированное обязательное поле: name"),
                this.version ?: throw RuntimeException("Неинициализированное обязательное поле: version"),
                this.description ?: "",
                this.authors ?: emptyList(),
                this.dependencies ?: emptyList(),
                this.uses ?: emptyList(),
                this.injectInto ?: false,
                this.injectIntoDependencies ?: false,
                this.injectFrom ?: false,
                this.imports ?: emptyList(),
                this.exports ?: emptyList(),
                this.features ?: emptyList(),
                this.sources ?: emptyList(),
                this.resources ?: emptyList(),
                this.plugins ?: emptyList(),
            )
        }
    }
}