package ru.pht.sprout.module.repo.impl

import io.github.z4kn4fein.semver.Version
import io.github.z4kn4fein.semver.constraints.Constraint
import io.github.z4kn4fein.semver.toVersion
import ru.pht.sprout.module.header.ModuleHeader
import ru.pht.sprout.module.header.lexer.Lexer
import ru.pht.sprout.module.header.parser.Parser
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.module.repo.cache.ICachingRepository
import ru.pht.sprout.module.utils.ZipUtils
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists
import kotlin.io.path.writeBytes

open class TestRepository(downloads: List<IDownloadable>) : IRepository {
    val downloads: List<IDownloadable> = downloads.sortedBy { it.version }

    override fun find(name: String, version: Constraint): List<IDownloadable> =
        this.downloads.filter { it.name == name && version.isSatisfiedBy(it.version) }

    override suspend fun findAsync(name: String, version: Constraint): List<IDownloadable> =
        this.find(name, version)

    override fun findAll(): List<IDownloadable> =
        this.downloads

    override suspend fun findAllAsync(): List<IDownloadable> =
        this.findAll()
}

open class TestDownloadable(
    override val name: String,
    override val version: Version,
    init: TestDownloadable.() -> Unit = TestDownloadable::initNormal
) : IDownloadable {
    protected lateinit var hash0: String
    protected lateinit var header0: ModuleHeader
    protected lateinit var zip0: ByteArray

    override val hash: String
        get() = this.hash0

    override fun header(): ModuleHeader =
        this.header0

    override suspend fun headerAsync(): ModuleHeader =
        this.header()

    override fun download(dir: Path) {
        if (MessageDigest.getInstance("SHA-512").digest(this.zip0).toHexString() != this.hash)
            throw IOException("Hash check failed")
        val dir = dir.resolve(this.name).normalize()
        if (dir.notExists())
            dir.createDirectories()
        ZipUtils.unzip(dir, this.zip0)
    }

    override suspend fun downloadAsync(dir: Path) =
        this.download(dir)

    override fun downloadZip(file: Path) {
        if (file.parent.notExists())
            file.parent.createDirectories()
        file.writeBytes(this.zip0)
    }

    override suspend fun downloadZipAsync(file: Path) =
        this.downloadZip(file)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TestDownloadable
        return this.hash0 == other.hash0
    }

    override fun hashCode(): Int =
        this.zip0.contentHashCode()

    init {
        init()
    }

    fun initNormal() {
        val header = "(module \"pht/module\" {[name \"${this.name}\"]} {[vers \"${this.version}\"]})"
        this.header0 = Parser(Lexer(header)).parse()
        val zip = ByteArrayOutputStream().use { bytes ->
            ZipOutputStream(bytes).use { zip ->
                zip.putNextEntry(
                    ZipEntry("module.pht").apply {
                        this.time = 0
                    }
                )
                zip.write(header.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            bytes.toByteArray()
        }
        this.zip0 = zip
        this.hash0 = MessageDigest.getInstance("SHA-512").digest(zip).toHexString()
    }

    fun initCrack() {
        val header = "(module \"pht/module\" {[name \"${this.name}\"]} {[vers \"${this.version}\"]} {[desc \"Crack!\"]})"
        this.header0 = Parser(Lexer(header)).parse()
        val zip = ByteArrayOutputStream().use { bytes ->
            ZipOutputStream(bytes).use { zip ->
                zip.putNextEntry(ZipEntry("module.pht"))
                zip.write(header.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            bytes.toByteArray()
        }
        this.zip0 = zip
        this.hash0 = ZipUtils.calcSHA512(zip)
    }
}

object AssertNoCacheRepository : ICachingRepository {
    override suspend fun findAllCachedAsync(): List<IDownloadable> {
        throw AssertionError("Using of cache repository")
    }

    override suspend fun findAsync(name: String, version: Constraint): List<IDownloadable> {
        throw AssertionError("Using of cache repository")
    }

    override suspend fun findAllAsync(): List<IDownloadable> {
        throw AssertionError("Using of cache repository")
    }
}

// Все модули
object TestRepositoryA : TestRepository(listOf(TestDownloadableA100A, TestDownloadableA110A, TestDownloadableA200A, TestDownloadableA300A, TestDownloadableB100A, TestDownloadableB200A, TestDownloadableC100A, TestDownloadableD100A))
// Все версии модулей A и B
object TestRepositoryB : TestRepository(listOf(TestDownloadableA100B, TestDownloadableA110B, TestDownloadableA200B, TestDownloadableA300B, TestDownloadableB100B, TestDownloadableB200B))
// Старые версии модулей A и B
object TestRepositoryC : TestRepository(listOf(TestDownloadableA100C, TestDownloadableA110C, TestDownloadableA200C, TestDownloadableB100C))
// Последние версии модулей A и B
object TestRepositoryD : TestRepository(listOf(TestDownloadableA300D, TestDownloadableB200D))
// Последние версии модулей A и B (скомпрометированные)
object TestRepositoryDCrack : TestRepository(listOf(TestDownloadableA300DCrack, TestDownloadableB200DCrack))
// Последние версии модулей A и B (сломанные)
object TestRepositoryDBroken : TestRepository(listOf(TestDownloadableA300DBroken, TestDownloadableB200DBroken))

// [=====] НОРМАЛЬНЫЕ ВЕРСИИ [=====]

object TestDownloadableA100A : TestDownloadable("test/a", "1.0.0".toVersion())
object TestDownloadableA100B : TestDownloadable("test/a", "1.0.0".toVersion())
object TestDownloadableA100C : TestDownloadable("test/a", "1.0.0".toVersion())
object TestDownloadableA110A : TestDownloadable("test/a", "1.1.0".toVersion())
object TestDownloadableA110B : TestDownloadable("test/a", "1.1.0".toVersion())
object TestDownloadableA110C : TestDownloadable("test/a", "1.1.0".toVersion())
object TestDownloadableA200A : TestDownloadable("test/a", "2.0.0".toVersion())
object TestDownloadableA200B : TestDownloadable("test/a", "2.0.0".toVersion())
object TestDownloadableA200C : TestDownloadable("test/a", "2.0.0".toVersion())
object TestDownloadableA300A : TestDownloadable("test/a", "3.0.0".toVersion())
object TestDownloadableA300B : TestDownloadable("test/a", "3.0.0".toVersion())
object TestDownloadableA300D : TestDownloadable("test/a", "3.0.0".toVersion())
object TestDownloadableB100A : TestDownloadable("test/b", "1.0.0".toVersion())
object TestDownloadableB100B : TestDownloadable("test/b", "1.0.0".toVersion())
object TestDownloadableB100C : TestDownloadable("test/b", "1.0.0".toVersion())
object TestDownloadableB200A : TestDownloadable("test/b", "2.0.0".toVersion())
object TestDownloadableB200B : TestDownloadable("test/b", "2.0.0".toVersion())
object TestDownloadableB200D : TestDownloadable("test/b", "2.0.0".toVersion())
object TestDownloadableC100A : TestDownloadable("test/c", "1.0.0".toVersion())
object TestDownloadableD100A : TestDownloadable("test/d", "1.0.0".toVersion())

// [=====] СКОМПРОМЕТИРОВАННЫЕ ВЕРСИИ [=====]

// Компрометация файла
object TestDownloadableA300DCrack : TestDownloadable("test/a", "3.0.0".toVersion(), TestDownloadable::initCrack) { init { this.hash0 = TestDownloadableA300D.hash } }
// Компрометация репозитория
object TestDownloadableB200DCrack : TestDownloadable("test/b", "2.0.0".toVersion(), TestDownloadable::initCrack)

// [=====] СЛОМАННЫЕ ВЕРСИИ [=====]

// Поломка заголовка
object TestDownloadableA300DBroken : TestDownloadable("test/a", "3.0.0".toVersion()) {
    override fun header(): ModuleHeader = throw IOException("Broken")
}

// Поломка скачивания
object TestDownloadableB200DBroken : TestDownloadable("test/a", "3.0.0".toVersion()) {
    override fun download(dir: Path) = throw IOException("Broken")
    override fun downloadZip(file: Path) = throw IOException("Broken")
}