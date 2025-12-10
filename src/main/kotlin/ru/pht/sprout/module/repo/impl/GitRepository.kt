package ru.pht.sprout.module.repo.impl

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.constraints.Constraint
import io.github.z4kn4fein.semver.toVersion
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.pht.sprout.module.header.ModuleHeader
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.utils.HttpUtils
import ru.pht.sprout.utils.ZipUtils
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

/**
 * Git совместимый репозиторий.
 *
 * @param client HttpClient.
 * @param repository Ссылка на json файл репозиториев.
 */
open class GitRepository(
    private val client: HttpClient = HttpUtils.clientWithoutLogging(),
    private val repository: String
) : IRepository {
    override suspend fun findAsync(name: String, version: Constraint): List<IDownloadable> =
        this
            .getAllRepositories(this.repository)
            .asSequence()
            .map { Pair(it, it.version.toVersion()) }
            .filter { (m, v) -> m.name == name && version.isSatisfiedBy(v) }
            .map { (m, v) -> GitDownloadable(m.name, v, m.hash, m.file) }
            .toMutableList()
            .sortedBy { it.version }

    override suspend fun findAllAsync(): List<IDownloadable> =
        this
            .getAllRepositories(this.repository)
            .map { GitDownloadable(it.name, it.version.toVersion(), it.hash, it.file) }

    private suspend inline fun getAllRepositories(url: String): List<Repository> {
        try {
            return Json.decodeFromString<List<Repository>>(this.client.get(url).bodyAsText(Charsets.UTF_8))
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

    private inner class GitDownloadable(
        override val name: String,
        override val version: Version,
        override val hash: String,
        val file: String
    ) : IDownloadable {
        override suspend fun headerAsync(): ModuleHeader =
            ZipUtils.unzipHeader(this@GitRepository.getAsStream(this.file))

        override suspend fun downloadAsync(dir: Path) {
            val bytes = this@GitRepository.getAsBytes(this.file)
            if (MessageDigest.getInstance("SHA-512").digest(bytes).toHexString() != this.hash)
                throw IOException("Hash check failed")
            val dir = dir.resolve(this.name).normalize()
            if (dir.notExists())
                dir.createDirectories()
            ZipUtils.unzip(dir, bytes)
        }

        override suspend fun downloadZipAsync(file: Path) {
            val bytes = this@GitRepository.getAsBytes(this.file)
            if (MessageDigest.getInstance("SHA-512").digest(bytes).toHexString() != this.hash)
                throw IOException("Hash check failed")
            if (file.parent.notExists())
                file.parent.createDirectories()
            withContext(Dispatchers.IO) {
                Files.write(file, bytes)
            }
        }
    }
}