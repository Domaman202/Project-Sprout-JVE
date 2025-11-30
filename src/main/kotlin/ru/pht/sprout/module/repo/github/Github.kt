package ru.pht.sprout.module.repo.github

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.pht.sprout.module.Module
import ru.pht.sprout.module.lexer.Lexer
import ru.pht.sprout.module.parser.Parser
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipInputStream

private const val GITHUB_API_URL = "https://api.github.com"
private val GITHUB_TOKEN by lazy {
    val tokenPath = Paths.get(System.getProperty("user.home"), ".github_token")
    if (Files.exists(tokenPath)) {
        Files.readString(tokenPath).trim()
    } else {
        null
    }
}

@Serializable
private data class GitHubRepo(
    @SerialName("full_name")
    val fullName: String
)

@Serializable
private data class GitHubBranch(
    val name: String
)

@Serializable
private data class GitHubFile(
    val name: String,
    val path: String,
    val type: String
)

@Serializable
private data class GitHubContent(
    val content: String,
    val encoding: String
)

@Serializable
private data class GitHubSearchResponse(
    val items: List<GitHubRepo>
)

class GitHubRepository : IRepository {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
    }

    override fun find(name: String): List<IDownloadable> = runBlocking {
        findAsync(name)
    }

    override suspend fun findAsync(name: String): List<IDownloadable> {
        val repos = mutableListOf<GitHubRepo>()
        for (topic in listOf("pihta-module", "sprout-module")) {
            val response: GitHubSearchResponse = client.get("$GITHUB_API_URL/search/repositories") {
                header("Authorization", "token ${GITHUB_TOKEN}")
                parameter("q", "$name in:name topic:$topic fork:true")
            }.body()
            repos.addAll(response.items)
        }

        val downloadables = mutableListOf<IDownloadable>()
        for (repo in repos) {
            val branches: List<GitHubBranch> = client.get("$GITHUB_API_URL/repos/${repo.fullName}/branches") {
                header("Authorization", "token ${GITHUB_TOKEN}")
            }.body()
            for (branch in branches) {
                downloadables.add(GitHubDownloadable(repo.fullName, branch.name, client))
            }
        }
        return downloadables
    }
}

class GitHubDownloadable(
    private val repoFullName: String,
    private val branchName: String,
    private val client: HttpClient
) : IDownloadable {

    override fun header(): Module = runBlocking {
        headerAsync()
    }

    override suspend fun headerAsync(): Module {
        val files: List<GitHubFile> = client.get("$GITHUB_API_URL/repos/$repoFullName/contents") {
            header("Authorization", "token ${GITHUB_TOKEN}")
            parameter("ref", branchName)
        }.body()

        val moduleFile = files.find { it.type == "file" && it.name.endsWith(".pht") }
            ?: throw IOException("No module file with .pht extension found in the repository root.")

        val content: GitHubContent = client.get("$GITHUB_API_URL/repos/$repoFullName/contents/${moduleFile.path}") {
            header("Authorization", "token ${GITHUB_TOKEN}")
            parameter("ref", branchName)
        }.body()

        if (content.encoding != "base64") {
            throw IOException("Unsupported encoding for module file: ${content.encoding}")
        }
        val decodedContent = java.util.Base64.getDecoder().decode(content.content).toString(Charsets.UTF_8)
        val lexer = Lexer(decodedContent)
        val parser = Parser(lexer)
        return parser.parse()
    }

    override fun download(dir: File) = runBlocking {
        downloadAsync(dir)
    }

    override suspend fun downloadAsync(dir: File) {
        val response: ByteArray = client.get("$GITHUB_API_URL/repos/$repoFullName/zipball/$branchName") {
            header("Authorization", "token ${GITHUB_TOKEN}")
        }.body()

        ZipInputStream(response.inputStream()).use { zipInputStream ->
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                val newFile = File(dir, entry.name.substring(entry.name.indexOf('/') + 1))
                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    newFile.parentFile.mkdirs()
                    newFile.outputStream().use { fileOut ->
                        zipInputStream.copyTo(fileOut)
                    }
                }
                entry = zipInputStream.nextEntry
            }
        }
    }
}
