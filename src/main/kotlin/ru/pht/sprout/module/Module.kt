@file:Suppress("UNUSED")
package ru.pht.sprout.module

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.constraints.Constraint
import ru.pht.sprout.utils.NotInitializedException

data class Module(
    val name: String,
    val version: Version,
    val description: String,
    val authors: List<String>,
    val dependencies: List<Dependency>,
    val uses: List<String>,
    val injectInto: Boolean,
    val injectIntoDependencies: Boolean,
    val injectFrom: Boolean,
    val imports: ValueOrAny<List<IntermoduleData>>,
    val exports: ValueOrAny<List<IntermoduleData>>,
    val features: List<Pair<String, Boolean>>,
    val sources: List<PathOrDependency>,
    val resources: List<PathOrDependency>,
    val plugins: List<PathOrDependency>,
) {
    data class Dependency(
        val name: String,
        val version: ValueOrAny<Constraint>,
        val uses: Boolean,
        val adapters: ValueOrAny<List<String>>,
        val injectInto: Boolean,
        val injectIntoDependencies: Boolean,
        val injectFrom: Boolean,
        val features: ValueOrAny<List<String>>,
        val disableFeatures: ValueOrAny<List<String>>
    ) {
        class Builder {
            var name: String? = null
            var version: ValueOrAny<Constraint>? = null
            var uses: Boolean? = null
            var adapters: ValueOrAny<List<String>>? = null
            var injectInto: Boolean? = null
            var injectIntoDependencies: Boolean? = null
            var injectFrom: Boolean? = null
            var features: ValueOrAny<List<String>>? = null
            var disableFeatures: ValueOrAny<List<String>>? = null

            fun name(value: String): Builder {
                this.name = value
                return this
            }

            fun name(): String? {
                return this.name
            }

            fun version(value: ValueOrAny<Constraint>): Builder {
                this.version = value
                return this
            }

            fun version(): ValueOrAny<Constraint>? {
                return this.version
            }

            fun uses(value: Boolean): Builder {
                this.uses = value
                return this
            }

            fun uses(): Boolean? {
                return this.uses
            }

            fun adapters(value: ValueOrAny<List<String>>): Builder {
                this.adapters = this.adapters with value
                return this
            }

            fun adapters(): ValueOrAny<List<String>>? {
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

            fun features(value: ValueOrAny<List<String>>): Builder {
                this.features = this.features with value
                return this
            }

            fun features(): ValueOrAny<List<String>>? {
                return this.features
            }

            fun disableFeatures(value: ValueOrAny<List<String>>): Builder {
                this.disableFeatures = this.disableFeatures with value
                return this
            }

            fun disableFeatures(): ValueOrAny<List<String>>? {
                return this.disableFeatures
            }

            fun build(): Dependency {
                return Dependency(
                    this.name ?: throw NotInitializedException("name"),
                    this.version ?: ValueOrAny.any(),
                    this.uses ?: false,
                    this.adapters ?: ValueOrAny.of(emptyList()),
                    this.injectInto ?: false,
                    this.injectIntoDependencies ?: false,
                    this.injectFrom ?: false,
                    this.features ?: ValueOrAny.of(emptyList()),
                    this.disableFeatures ?: ValueOrAny.of(emptyList()),
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

    class ValueOrAny <T> private constructor(private val value: T?, private val any: Boolean) {
        val isValue: Boolean
            get() = !this.any
        val isAny: Boolean
            get() = this.any

        fun value(): T {
            return this.value!!
        }

        companion object {
            val ANY = ValueOrAny(null, true)

            fun <T> of(value: T): ValueOrAny<T> =
                ValueOrAny(value, false)

            @Suppress("UNCHECKED_CAST")
            fun <T> any(): ValueOrAny<T> =
                ANY as ValueOrAny<T>
        }
    }

    class Builder {
        var name: String? = null
        var version: Version? = null
        var description: String? = null
        var authors: List<String>? = null
        var dependencies: List<Dependency>? = null
        var uses: List<String>? = null
        var injectInto: Boolean? = null
        var injectIntoDependencies: Boolean? = null
        var injectFrom: Boolean? = null
        var imports: ValueOrAny<List<IntermoduleData>>? = null
        var exports: ValueOrAny<List<IntermoduleData>>? = null
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

        fun version(value: Version): Builder {
            this.version = value
            return this
        }

        fun version(): Version? {
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

        fun imports(value: ValueOrAny<List<IntermoduleData>>): Builder {
            this.imports = this.imports with value
            return this
        }

        fun imports(): ValueOrAny<List<IntermoduleData>>? {
            return this.imports
        }

        fun exports(value: ValueOrAny<List<IntermoduleData>>): Builder {
            this.exports = this.exports with value
            return this
        }

        fun exports(): ValueOrAny<List<IntermoduleData>>? {
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
                this.name ?: throw NotInitializedException("name"),
                this.version ?: throw NotInitializedException("vers"),
                this.description ?: "",
                this.authors ?: emptyList(),
                this.dependencies ?: emptyList(),
                this.uses ?: emptyList(),
                this.injectInto ?: false,
                this.injectIntoDependencies ?: false,
                this.injectFrom ?: false,
                this.imports ?: ValueOrAny.of(emptyList()),
                this.exports ?: ValueOrAny.of(emptyList()),
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

        infix fun <E> ValueOrAny<List<E>>?.with(second: ValueOrAny<List<E>>): ValueOrAny<List<E>> {
            this ?: return second
            if (this.isAny)
                return this
            if (second.isAny)
                return second
            val new = ArrayList<E>()
            new.addAll(this.value())
            new.addAll(second.value())
            return ValueOrAny.of(new)
        }
    }
}