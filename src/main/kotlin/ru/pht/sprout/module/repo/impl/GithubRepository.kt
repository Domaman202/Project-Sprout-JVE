package ru.pht.sprout.module.repo.impl

import io.github.z4kn4fein.semver.constraints.Constraint
import io.github.z4kn4fein.semver.toVersion
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.pht.sprout.module.Module
import ru.pht.sprout.module.parser.ParserUtils
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

class GithubRepository(private val client: HttpClient = HttpClient(CIO)) : IRepository {
    override fun find(name: String, version: Constraint): List<IDownloadable> = runBlocking {
        withContext(Dispatchers.IO) {
            findAsync(name, version)
        }
    }

    override suspend fun findAsync(name: String, version: Constraint): List<IDownloadable> {
        return this
            .getAs<List<Repository>>("https://github.com/Domaman202/Project-Sprout-Module-List-Github/raw/refs/heads/master/verified.json")
            .filter { it.name == name && version.isSatisfiedBy(it.version.toVersion()) }
            .map { GithubDownloadable(it.name, it.hash, it.file) }
    }

    private suspend inline fun <reified T> getAs(url: String): T {
        try {
            return Json.decodeFromString<T>(this.client.get(url).bodyAsText(Charsets.UTF_8))
        } catch (e: NoTransformationFoundException) {
            throw IOException(e)
        }
    }

    private suspend fun getAsBytes(url: String): ByteArray =
        this.client.get(url).readRawBytes()

    private suspend fun getAsStream(url: String): InputStream =
        this.client.get(url).bodyAsChannel().toInputStream()

    @Serializable
    private data class Repository(
        val name: String,
        val version: String,
        val git: String,
        val hash: String,
        val file: String
    )

    private inner class GithubDownloadable(private val name: String, private val hash: String, private val file: String) : IDownloadable {
        override fun header(): Module = runBlocking {
            withContext(Dispatchers.IO) {
                headerAsync()
            }
        }

        override suspend fun headerAsync(): Module {
            ZipInputStream(this@GithubRepository.getAsStream(this.file)).use {
                while (true) {
                    val entry = it.nextEntry ?: throw IOException("File 'module.pht' not founded")
                    if (!entry.isDirectory && entry.name == "module.pht")
                        return ParserUtils.parseString(it.readBytes().toString(Charsets.UTF_8))
                    it.closeEntry()
                }
            }
        }

        override fun download(dir: Path) = runBlocking {
            withContext(Dispatchers.IO) {
                downloadAsync(dir)
            }
        }

        override suspend fun downloadAsync(dir: Path) {
            val bytes = this@GithubRepository.getAsBytes(this.file)
            if (MessageDigest.getInstance("SHA-512").digest(bytes).toHexString() != this.hash)
                throw IOException("Hash check failed")
            ZipInputStream(ByteArrayInputStream(bytes)).use {
                val dir = dir.resolve(this.name).normalize()
                if (dir.notExists())
                    dir.createDirectories()
                while (true) {
                    val entry = it.nextEntry ?: break
                    val target = dir.resolve(entry.name).normalize()
                    if (entry.isDirectory) {
                        target.createDirectories()
                    } else {
                        target.parent.createDirectories()
                        Files.copy(it, target)
                    }
                    it.closeEntry()
                }
            }
        }
    }
}