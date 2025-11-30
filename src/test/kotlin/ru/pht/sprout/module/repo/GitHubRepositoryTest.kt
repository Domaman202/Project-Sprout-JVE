package ru.pht.sprout.module.repo

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import ru.pht.sprout.module.repo.github.GitHubRepository
import java.io.File

// This test requires a valid GitHub token in ~/.github_token
// and makes live network requests.
@Disabled("Requires network and authentication")
class GitHubRepositoryTest {

    @Test
    fun `test find modules`() {
        val repository = GitHubRepository()
        val downloadables = runBlocking {
            repository.findAsync("sprout")
        }
        assertTrue(downloadables.isNotEmpty(), "Should find at least one downloadable")
    }

    @Test
    fun `test download and read header`() {
        val repository = GitHubRepository()
        val downloadables = runBlocking {
            repository.findAsync("Project-Sprout-Example-Module")
        }
        val downloadable = downloadables.firstOrNull()
        assertNotNull(downloadable, "No downloadable found for Project-Sprout-Example-Module")

        val module = runBlocking {
            downloadable!!.headerAsync()
        }
        assertNotNull(module, "Module header should not be null")
        assertEquals("pht/example/example-module", module.name)

        val tempDir = createTempDir("Project-Sprout-Example-Module")
        try {
            runBlocking {
                downloadable.downloadAsync(tempDir)
            }
            val moduleFile = File(tempDir, "module.pht")
            assertTrue(moduleFile.exists(), "module.pht should be downloaded")
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
