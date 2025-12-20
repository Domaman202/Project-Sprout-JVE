package ru.pht.sprout.module.repo.cache.impl

import io.github.z4kn4fein.semver.Version
import kotlinx.io.IOException
import ru.DmN.translate.TranslationKey
import ru.pht.sprout.module.header.ModuleHeader
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.utils.SproutIllegalArgumentException
import java.nio.file.Path

/**
 * Комбинированный источник загрузки.
 * Собирает в себе множество источников для одного конкретного модуля.
 *
 * @param originals Источники загрузки.
 * @param name Имя модуля.
 * @param version Версия модуля.
 * @param hash Хеш модуля.
 */
class CombinedDownloadable(
    val originals: List<IDownloadable>,
    override val name: String,
    override val version: Version,
    override val hash: String
) : IDownloadable {
    override fun header(): ModuleHeader = tryAllSources {
        header()
    }

    override suspend fun headerAsync(): ModuleHeader = tryAllSources {
        headerAsync()
    }

    override fun download(dir: Path) = tryAllSources {
        download(dir)
    }

    override suspend fun downloadAsync(dir: Path) = tryAllSources {
        downloadAsync(dir)
    }

    override fun downloadZip(file: Path) = tryAllSources {
        downloadZip(file)
    }

    override suspend fun downloadZipAsync(file: Path) = tryAllSources {
        downloadZipAsync(file)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CombinedDownloadable
        return this.originals == other.originals
    }

    override fun hashCode(): Int =
        this.originals.hashCode()

    private inline fun <T> tryAllSources(block: IDownloadable.() -> T): T {
        for (downloadable in this.originals) {
            try {
                return block(downloadable)
            } catch (_: IOException) {
            }
        }
        throw IOException("Все источники '${this.name}' недоступны")
    }

    companion object {
        /**
         * Создание объединённого источника из нескольких однотипных источников конкретного модуля.
         *
         * @param combine Список источников.
         * @return Объединённый источник.
         */
        @Suppress("NOTHING_TO_INLINE")
        inline fun of(combine: List<IDownloadable>): CombinedDownloadable {
            val first = combine.first()
            if (combine.all { it.name == first.name && it.version == first.version && it.hash == first.hash })
                return CombinedDownloadable(combine, first.name, first.version, first.hash)
            throw SproutIllegalArgumentException(TranslationKey.of<CombinedDownloadable>("exception"), "name" to "${first.name}@${first.version}")
        }

        /**
         * Объединяет несколько однотипных источников конкретного модуля [combine] и передаёт в [addTo].
         *
         * @param combine Список источников.
         */
        inline fun combineAndAdd(combine: List<IDownloadable>, addTo: (IDownloadable) -> Unit) {
            addTo(of(combine))
        }
    }
}