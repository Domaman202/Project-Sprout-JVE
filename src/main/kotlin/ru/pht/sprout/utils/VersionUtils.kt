package ru.pht.sprout.utils

typealias VersionTriple = Triple<Int, Int, Int>

object VersionUtils {

    private val MODULE_VERSION_REGEX = Regex("^(0|[1-9]\\d*)\\.(0|[1-9]\\d*)\\.(0|[1-9]\\d*)$")

    private fun parseVersion(version: String): VersionTriple? {
        return MODULE_VERSION_REGEX.matchEntire(version)?.destructured?.let { (major, minor, patch) ->
            Triple(major.toInt(), minor.toInt(), patch.toInt())
        }
    }

    fun isValidModuleVersion(version: String): Boolean {
        return parseVersion(version) != null
    }

    fun isValidDependencyVersion(version: String): Boolean {
        if (isValidModuleVersion(version)) return true
        val prefix = version.firstOrNull()
        if (prefix == '^' || prefix == '~' || prefix == '>' || prefix == '<' || prefix == '=') {
            val versionPart = if (version.startsWith(">=") || version.startsWith("<=")) {
                version.substring(2)
            } else {
                version.substring(1)
            }
            return isValidModuleVersion(versionPart)
        }
        return false
    }

    private fun compareVersions(v1: VersionTriple, v2: VersionTriple): Int {
        if (v1.first != v2.first) {
            return v1.first.compareTo(v2.first)
        }
        if (v1.second != v2.second) {
            return v1.second.compareTo(v2.second)
        }
        return v1.third.compareTo(v2.third)
    }

    fun isCompatible(moduleVersionStr: String, dependencyVersionStr: String): Boolean {
        val moduleVersion = parseVersion(moduleVersionStr) ?: return false

        if (isValidModuleVersion(dependencyVersionStr)) {
            val dependencyVersion = parseVersion(dependencyVersionStr) ?: return false
            return compareVersions(moduleVersion, dependencyVersion) == 0
        }
        
        val operator: String
        val versionPart: String

        if (dependencyVersionStr.startsWith(">=") || dependencyVersionStr.startsWith("<=")) {
            operator = dependencyVersionStr.take(2)
            versionPart = dependencyVersionStr.substring(2)
        } else {
            operator = dependencyVersionStr.take(1)
            versionPart = dependencyVersionStr.substring(1)
        }
        
        val dependencyVersion = parseVersion(versionPart) ?: return false
        val comparison = compareVersions(moduleVersion, dependencyVersion)

        return when (operator) {
            "^" -> {
                when {
                    dependencyVersion.first != 0 ->
                        comparison >= 0 && moduleVersion.first == dependencyVersion.first
                    dependencyVersion.second != 0 ->
                        comparison >= 0 && moduleVersion.first == 0 && moduleVersion.second == dependencyVersion.second
                    else ->
                        comparison == 0
                }
            }
            "~" -> {
                comparison >= 0 && moduleVersion.first == dependencyVersion.first && moduleVersion.second == dependencyVersion.second
            }
            ">" -> comparison > 0
            ">=" -> comparison >= 0
            "<" -> comparison < 0
            "<=" -> comparison <= 0
            "=" -> comparison == 0
            else -> false
        }
    }
}

class NotInitializedException(val field: String) : RuntimeException("Неинициализированное обязательное поле '$field'")