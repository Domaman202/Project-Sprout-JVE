@file:Suppress("UNUSED")
package ru.pht.sprout.module

import ru.pht.sprout.module.parser.ParserException

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
    val imports: ListOrAny<IntermoduleData>,
    val exports: ListOrAny<IntermoduleData>,
    val features: List<Pair<String, Boolean>>,
    val sources: List<PathOrDependency>,
    val resources: List<PathOrDependency>,
    val plugins: List<PathOrDependency>,
) {
    data class Dependency(
        val name: String,
        val version: StringOrAny,
        val uses: Boolean,
        val adapters: ListOrAny<String>,
        val injectInto: Boolean,
        val injectIntoDependencies: Boolean,
        val injectFrom: Boolean,
        val features: ListOrAny<String>,
        val disableFeatures: ListOrAny<String>
    ) {
        class Builder {
            var name: String? = null
            var version: StringOrAny? = null
            var uses: Boolean? = null
            var adapters: ListOrAny<String>? = null
            var injectInto: Boolean? = null
            var injectIntoDependencies: Boolean? = null
            var injectFrom: Boolean? = null
            var features: ListOrAny<String>? = null
            var disableFeatures: ListOrAny<String>? = null

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

            fun uses(value: Boolean): Builder {
                this.uses = value
                return this
            }

            fun uses(): Boolean? {
                return this.uses
            }

            fun adapters(value: ListOrAny<String>): Builder {
                this.adapters = this.adapters with value
                return this
            }

            fun adapters(): ListOrAny<String>? {
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

            fun features(value: ListOrAny<String>): Builder {
                this.features = this.features with value
                return this
            }

            fun features(): ListOrAny<String>? {
                return this.features
            }

            fun disableFeatures(value: ListOrAny<String>): Builder {
                this.disableFeatures = this.disableFeatures with value
                return this
            }

            fun disableFeatures(): ListOrAny<String>? {
                return this.disableFeatures
            }

            fun build(): Dependency {
                return Dependency(
                    this.name ?: throw ParserException.NotInitializedException("name"),
                    this.version ?: StringOrAny.ANY,
                    this.uses ?: false,
                    this.adapters ?: ListOrAny.empty(),
                    this.injectInto ?: false,
                    this.injectIntoDependencies ?: false,
                    this.injectFrom ?: false,
                    this.features ?: ListOrAny.empty(),
                    this.disableFeatures ?: ListOrAny.empty(),
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
            fun ofPath(path: String): PathOrDependency {
                return PathOrDependency(path, null)
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

    class ListOrAny<T> private constructor(private val list: List<T>?, private val any: Boolean) {
        val isList: Boolean
            get() = !this.any
        val isAny: Boolean
            get() = this.any

        fun list(): List<T> {
            return this.list!!
        }

        companion object {
            fun <T> any(): ListOrAny<T> {
                return ListOrAny(null, true)
            }

            fun <T> empty(): ListOrAny<T> {
                return ListOrAny(emptyList(), false)
            }

            fun <T> ofList(list: List<T>): ListOrAny<T> {
                return ListOrAny(list, false)
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
        var imports: ListOrAny<IntermoduleData>? = null
        var exports: ListOrAny<IntermoduleData>? = null
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
            this.authors = this.authors with value
            return this
        }

        fun authors(): List<String>? {
            return this.authors
        }

        fun dependencies(value: List<Dependency>): Builder {
            this.dependencies = this.dependencies with value
            return this
        }

        fun dependencies(): List<Dependency>? {
            return this.dependencies
        }

        fun uses(value: List<String>): Builder {
            this.uses = this.uses with value
            return this
        }

        fun uses(): List<String>? {
            return this.uses
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

        fun imports(value: ListOrAny<IntermoduleData>): Builder {
            this.imports = this.imports with value
            return this
        }

        fun imports(): ListOrAny<IntermoduleData>? {
            return this.imports
        }

        fun exports(value: ListOrAny<IntermoduleData>): Builder {
            this.exports = this.exports with value
            return this
        }

        fun exports(): ListOrAny<IntermoduleData>? {
            return this.exports
        }

        fun features(value: List<Pair<String, Boolean>>): Builder {
            this.features = this.features with value
            return this
        }

        fun features(): List<Pair<String, Boolean>>? {
            return this.features
        }

        fun sources(value: List<PathOrDependency>): Builder {
            this.sources = this.sources with value
            return this
        }

        fun sources(): List<PathOrDependency>? {
            return this.sources
        }

        fun resources(value: List<PathOrDependency>): Builder {
            this.resources = this.resources with value
            return this
        }

        fun resources(): List<PathOrDependency>? {
            return this.resources
        }

        fun plugins(value: List<PathOrDependency>): Builder {
            this.plugins = this.plugins with value
            return this
        }

        fun plugins(): List<PathOrDependency>? {
            return this.plugins
        }

        fun build(): Module {
            return Module(
                this.name ?: throw ParserException.NotInitializedException("name"),
                this.version ?: throw ParserException.NotInitializedException("vers"),
                this.description ?: "",
                this.authors ?: emptyList(),
                this.dependencies ?: emptyList(),
                this.uses ?: emptyList(),
                this.injectInto ?: false,
                this.injectIntoDependencies ?: false,
                this.injectFrom ?: false,
                this.imports ?: ListOrAny.empty(),
                this.exports ?: ListOrAny.empty(),
                this.features ?: emptyList(),
                this.sources ?: emptyList(),
                this.resources ?: emptyList(),
                this.plugins ?: emptyList(),
            )
        }
    }

    companion object {
        infix fun <E> List<E>?.with(second: List<E>): List<E> {
            this ?: return second
            val new = ArrayList<E>()
            new.addAll(this)
            new.addAll(second)
            return new
        }

        infix fun <E> ListOrAny<E>?.with(second: ListOrAny<E>): ListOrAny<E> {
            this ?: return second
            if (this.isAny)
                return this
            if (second.isAny)
                return second
            val new = ArrayList<E>()
            new.addAll(this.list())
            new.addAll(second.list())
            return ListOrAny.ofList(new)
        }
    }
}