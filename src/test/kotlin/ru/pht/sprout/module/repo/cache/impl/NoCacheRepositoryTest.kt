package ru.pht.sprout.module.repo.cache.impl

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.constraints.Constraint
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.pht.sprout.cli.build.BuildInfo
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.module.repo.cache.ICachingRepository
import java.nio.file.Path

class NoCacheRepositoryTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var mockRepository1: IRepository
    private lateinit var mockRepository2: IRepository
    private lateinit var mockCachingRepository: ICachingRepository
    private lateinit var mockDownloadable1: IDownloadable
    private lateinit var mockDownloadable2: IDownloadable
    private lateinit var mockDownloadable3: IDownloadable
    private lateinit var mockDownloadable4: IDownloadable

    @BeforeEach
    fun setUp() {
        mockRepository1 = mockk()
        mockRepository2 = mockk()
        mockCachingRepository = mockk()

        mockDownloadable1 = mockk {
            coEvery { name } returns "moduleA"
            coEvery { version } returns Version.parse("1.0.0")
            coEvery { hash } returns "hash1"
        }

        mockDownloadable2 = mockk {
            coEvery { name } returns "moduleA"
            coEvery { version } returns Version.parse("1.0.0")
            coEvery { hash } returns "hash2"
        }

        mockDownloadable3 = mockk {
            coEvery { name } returns "moduleB"
            coEvery { version } returns Version.parse("2.0.0")
            coEvery { hash } returns "hash3"
        }

        mockDownloadable4 = mockk {
            coEvery { name } returns "moduleA"
            coEvery { version } returns Version.parse("2.0.0")
            coEvery { hash } returns "hash4"
        }
    }

    @Test
    fun `constructor with BuildInfo should initialize correctly`() = runTest {
        val mockBuildInfo = mockk<BuildInfo> {
            every { repositories } returns listOf(mockRepository1, mockRepository2)
        }

        val repository = NoCacheRepository(mockBuildInfo)

        assertNotNull(repository)
    }

    @Test
    fun `constructor with repositories list should initialize correctly`() {
        val repository = NoCacheRepository(listOf(mockRepository1, mockRepository2))

        assertNotNull(repository)
    }

    @Test
    fun `findAsync should return filtered and verified modules`() = runTest {
        // Arrange
        coEvery { mockRepository1.findAllAsync() } returns listOf(
            mockDownloadable1,
            mockDownloadable3
        )
        coEvery { mockRepository2.findAllAsync() } returns listOf(
            mockDownloadable1,
            mockDownloadable2,
            mockDownloadable4
        )

        val repository = NoCacheRepository(listOf(mockRepository1, mockRepository2))
        val constraint = Constraint.parse("1.0.0")

        // Act
        val result = repository.findAsync("moduleA", constraint)

        // Assert
        assertEquals(1, result.size)
        assertEquals("moduleA", result[0].name)
        assertEquals(Version.parse("1.0.0"), result[0].version)
        // Should select the hash with most occurrences (hash1 has 2, hash2 has 1)
        assertEquals("hash1", result[0].hash)
    }

    @Test
    fun `findAsync should skip caching repositories`() = runTest {
        // Arrange
        coEvery { mockRepository1.findAllAsync() } returns listOf(mockDownloadable1)
        coEvery { mockCachingRepository.findAllAsync() } returns listOf(mockDownloadable2)
        coEvery { mockCachingRepository.findAllCached() } returns emptyList()

        val repository = NoCacheRepository(listOf(mockRepository1, mockCachingRepository))
        val constraint = Constraint.parse("1.0.0")

        // Act
        val result = repository.findAsync("moduleA", constraint)

        // Assert
        assertEquals(1, result.size)
        assertEquals("hash1", result[0].hash) // Should only include from non-caching repository
    }

    @Test
    fun `findAsync should return empty list when no modules found`() = runTest {
        // Arrange
        coEvery { mockRepository1.findAllAsync() } returns emptyList()
        coEvery { mockRepository2.findAllAsync() } returns emptyList()

        val repository = NoCacheRepository(listOf(mockRepository1, mockRepository2))
        val constraint = Constraint.parse("1.0.0")

        // Act
        val result = repository.findAsync("moduleA", constraint)

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findAsync should handle multiple versions correctly`() = runTest {
        // Arrange
        coEvery { mockRepository1.findAllAsync() } returns listOf(
            mockDownloadable1,
            mockDownloadable4
        )
        coEvery { mockRepository2.findAllAsync() } returns listOf(
            mockDownloadable1,
            mockDownloadable4
        )

        val repository = NoCacheRepository(listOf(mockRepository1, mockRepository2))
        val constraint = Constraint.parse(">=1.0.0")

        // Act
        val result = repository.findAsync("moduleA", constraint)

        // Assert
        assertEquals(2, result.size)
        assertTrue(result.any { it.version == Version.parse("1.0.0") })
        assertTrue(result.any { it.version == Version.parse("2.0.0") })
    }

    @Test
    fun `findAllAsync should return all verified modules`() = runTest {
        // Arrange
        coEvery { mockRepository1.findAllAsync() } returns listOf(
            mockDownloadable1,
            mockDownloadable3
        )
        coEvery { mockRepository2.findAllAsync() } returns listOf(
            mockDownloadable1,
            mockDownloadable2,
            mockDownloadable3,
            mockDownloadable4
        )

        val repository = NoCacheRepository(listOf(mockRepository1, mockRepository2))

        // Act
        val result = repository.findAllAsync()

        // Assert
        assertEquals(3, result.size) // moduleA v1.0.0 (hash1), moduleA v2.0.0, moduleB v2.0.0

        val moduleAV1 = result.find { it.name == "moduleA" && it.version == Version.parse("1.0.0") }
        assertNotNull(moduleAV1)
        assertEquals("hash1", moduleAV1!!.hash) // Should select hash1 (2 occurrences vs hash2's 1)

        val moduleAV2 = result.find { it.name == "moduleA" && it.version == Version.parse("2.0.0") }
        assertNotNull(moduleAV2)

        val moduleB = result.find { it.name == "moduleB" && it.version == Version.parse("2.0.0") }
        assertNotNull(moduleB)
    }

    @Test
    fun `findAllAsync should skip caching repositories`() = runTest {
        // Arrange
        coEvery { mockRepository1.findAllAsync() } returns listOf(mockDownloadable1)
        coEvery { mockCachingRepository.findAllAsync() } returns listOf(mockDownloadable2)
        coEvery { mockCachingRepository.findAllCached() } returns emptyList()

        val repository = NoCacheRepository(listOf(mockRepository1, mockCachingRepository))

        // Act
        val result = repository.findAllAsync()

        // Assert
        assertEquals(1, result.size)
        assertEquals("hash1", result[0].hash) // Should only include from non-caching repository
    }

    @Test
    fun `findAllCached should return empty list`() {
        // Arrange
        val repository = NoCacheRepository(listOf(mockRepository1, mockRepository2))

        // Act
        val result = repository.findAllCached()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `findAllCachedAsync should return empty list`() = runTest {
        // Arrange
        val repository = NoCacheRepository(listOf(mockRepository1, mockRepository2))

        // Act
        val result = repository.findAllCachedAsync()

        // Assert
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should handle hash collisions correctly`() = runTest {
        // Arrange - create multiple downloadables with same version but different hashes
        val downloadableHash1_1 = mockk<IDownloadable> {
            coEvery { name } returns "moduleC"
            coEvery { version } returns Version.parse("1.0.0")
            coEvery { hash } returns "hashX"
        }

        val downloadableHash1_2 = mockk<IDownloadable> {
            coEvery { name } returns "moduleC"
            coEvery { version } returns Version.parse("1.0.0")
            coEvery { hash } returns "hashX"
        }

        val downloadableHash2 = mockk<IDownloadable> {
            coEvery { name } returns "moduleC"
            coEvery { version } returns Version.parse("1.0.0")
            coEvery { hash } returns "hashY"
        }

        coEvery { mockRepository1.findAllAsync() } returns listOf(
            downloadableHash1_1,
            downloadableHash1_2
        )
        coEvery { mockRepository2.findAllAsync() } returns listOf(
            downloadableHash2
        )

        val repository = NoCacheRepository(listOf(mockRepository1, mockRepository2))
        val constraint = Constraint.parse("1.0.0")

        // Act
        val result = repository.findAsync("moduleC", constraint)

        // Assert - should select hashX group (2 occurrences) over hashY (1 occurrence)
        assertEquals(1, result.size)
        assertEquals("hashX", result[0].hash)
    }

    @Test
    fun `should sort results by version in findAsync`() = runTest {
        // Arrange
        val downloadableV1 = mockk<IDownloadable> {
            coEvery { name } returns "moduleD"
            coEvery { version } returns Version.parse("1.0.0")
            coEvery { hash } returns "hash1"
        }

        val downloadableV2 = mockk<IDownloadable> {
            coEvery { name } returns "moduleD"
            coEvery { version } returns Version.parse("2.0.0")
            coEvery { hash } returns "hash2"
        }

        val downloadableV3 = mockk<IDownloadable> {
            coEvery { name } returns "moduleD"
            coEvery { version } returns Version.parse("3.0.0")
            coEvery { hash } returns "hash3"
        }

        coEvery { mockRepository1.findAllAsync() } returns listOf(
            downloadableV2,
            downloadableV1
        )
        coEvery { mockRepository2.findAllAsync() } returns listOf(
            downloadableV3,
            downloadableV2
        )

        val repository = NoCacheRepository(listOf(mockRepository1, mockRepository2))
        val constraint = Constraint.parse(">=1.0.0")

        // Act
        val result = repository.findAsync("moduleD", constraint)

        // Assert - results should be sorted by version
        assertEquals(3, result.size)
        assertEquals(Version.parse("1.0.0"), result[0].version)
        assertEquals(Version.parse("2.0.0"), result[1].version)
        assertEquals(Version.parse("3.0.0"), result[2].version)
    }
}