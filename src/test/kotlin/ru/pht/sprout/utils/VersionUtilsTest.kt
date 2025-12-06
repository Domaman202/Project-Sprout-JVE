package ru.pht.sprout.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VersionUtilsTest {

    @Test
    fun `test valid module versions`() {
        assertTrue(VersionUtils.isValidModuleVersion("1.0.0"))
        assertTrue(VersionUtils.isValidModuleVersion("0.1.0"))
        assertTrue(VersionUtils.isValidModuleVersion("12.34.56"))
        assertFalse(VersionUtils.isValidModuleVersion("1.0"))
        assertFalse(VersionUtils.isValidModuleVersion("1.0.0-alpha"))
        assertFalse(VersionUtils.isValidModuleVersion("v1.0.0"))
        assertFalse(VersionUtils.isValidModuleVersion("1.0.0.0"))
    }

    @Test
    fun `test valid dependency versions`() {
        assertTrue(VersionUtils.isValidDependencyVersion("1.0.0"))
        assertTrue(VersionUtils.isValidDependencyVersion("^1.0.0"))
        assertTrue(VersionUtils.isValidDependencyVersion("~1.2.3"))
        assertTrue(VersionUtils.isValidDependencyVersion(">0.1.0"))
        assertTrue(VersionUtils.isValidDependencyVersion("<=2.3.4"))
        assertTrue(VersionUtils.isValidDependencyVersion(">=2.3.4"))
        assertFalse(VersionUtils.isValidDependencyVersion("1.0"))
        assertFalse(VersionUtils.isValidDependencyVersion("=1.0.0-alpha"))
        assertFalse(VersionUtils.isValidDependencyVersion("><1.0.0"))
    }

    @Test
    fun `test exact version compatibility`() {
        assertTrue(VersionUtils.isCompatible("1.2.3", "1.2.3"))
        assertFalse(VersionUtils.isCompatible("1.2.4", "1.2.3"))
    }

    @Test
    fun `test caret version compatibility`() {
        // Major version > 0
        assertTrue(VersionUtils.isCompatible("1.2.3", "^1.2.3"))
        assertTrue(VersionUtils.isCompatible("1.3.0", "^1.2.3"))
        assertTrue(VersionUtils.isCompatible("1.9.9", "^1.2.3"))
        assertFalse(VersionUtils.isCompatible("2.0.0", "^1.2.3"))
        assertFalse(VersionUtils.isCompatible("1.2.2", "^1.2.3"))

        // Major version is 0, minor > 0
        assertTrue(VersionUtils.isCompatible("0.2.3", "^0.2.3"))
        assertTrue(VersionUtils.isCompatible("0.2.4", "^0.2.3"))
        assertFalse(VersionUtils.isCompatible("0.3.0", "^0.2.3"))

        // Major and minor are 0
        assertTrue(VersionUtils.isCompatible("0.0.3", "^0.0.3"))
        assertFalse(VersionUtils.isCompatible("0.0.4", "^0.0.3"))
    }

    @Test
    fun `test tilde version compatibility`() {
        assertTrue(VersionUtils.isCompatible("1.2.3", "~1.2.3"))
        assertTrue(VersionUtils.isCompatible("1.2.4", "~1.2.3"))
        assertTrue(VersionUtils.isCompatible("1.2.9", "~1.2.3"))
        assertFalse(VersionUtils.isCompatible("1.3.0", "~1.2.3"))
        assertFalse(VersionUtils.isCompatible("1.2.2", "~1.2.3"))
    }

    @Test
    fun `test comparison operators`() {
        assertTrue(VersionUtils.isCompatible("2.0.0", ">1.0.0"))
        assertFalse(VersionUtils.isCompatible("1.0.0", ">1.0.0"))

        assertTrue(VersionUtils.isCompatible("1.0.0", ">=1.0.0"))
        assertTrue(VersionUtils.isCompatible("1.0.1", ">=1.0.0"))

        assertTrue(VersionUtils.isCompatible("0.9.0", "<1.0.0"))
        assertFalse(VersionUtils.isCompatible("1.0.0", "<1.0.0"))

        assertTrue(VersionUtils.isCompatible("1.0.0", "<=1.0.0"))
        assertTrue(VersionUtils.isCompatible("0.9.9", "<=1.0.0"))
    }
}