package ru.pht.sprout.cli.build

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import ru.DmN.translate.TranslationKey
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.module.repo.cache.ICachingRepository
import ru.pht.sprout.module.repo.cache.impl.LocalCacheRepository
import ru.pht.sprout.module.repo.cache.impl.NoCacheRepository
import ru.pht.sprout.module.repo.impl.GitRepository
import ru.pht.sprout.utils.JsonUtils
import ru.pht.sprout.utils.exception.SproutIllegalArgumentException
import java.io.File
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

@Serializable(BuildSystemInfo.Serializer::class)
class BuildSystemInfo private constructor(
    val workdir: Path,
    val repositories: List<IRepository> = mutableListOf(GitRepository.github(), GitRepository.gitflic()),
    val cachingRepository: ICachingRepository = LocalCacheRepository(workdir, repositories, 1.hours)
) {
    class Serializer : KSerializer<BuildSystemInfo> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(BuildSystemInfo::class.qualifiedName!!) {
            element<String>("workdir")
            element<List<IRepository>>("repositories")
            element<ICachingRepository>("caching-repository")
        }

        override fun serialize(encoder: Encoder, value: BuildSystemInfo) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, value.workdir.toString())
                encodeSerializableElement(descriptor, 1, ListSerializer(RepositorySerializer), value.repositories)
                encodeSerializableElement(descriptor, 2, CachingRepositorySerializer(value.workdir, value.repositories), value.cachingRepository)
            }
        }

        override fun deserialize(decoder: Decoder): BuildSystemInfo {
            return decoder.decodeStructure(descriptor) {
                val workdir = File(decodeStringElement(descriptor, decodeElementIndex(descriptor))).toPath()
                val repositories = decodeSerializableElement(descriptor, decodeElementIndex(descriptor), ListSerializer(RepositorySerializer))
                val cachingRepository = decodeSerializableElement(descriptor, decodeElementIndex(descriptor), CachingRepositorySerializer(workdir, repositories))
                BuildSystemInfo(
                    workdir,
                    repositories,
                    cachingRepository,
                )
            }
        }
    }

    object RepositorySerializer : KSerializer<IRepository> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(IRepository::class.qualifiedName!!, PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: IRepository) {
            when (value) {
                is GitRepository -> encoder.encodeString("git://${value.repository}")
                else -> throw SproutIllegalArgumentException(TranslationKey.of<BuildSystemInfo>("serialize.exception"), "type" to value.javaClass.name)
            }
        }

        override fun deserialize(decoder: Decoder): IRepository {
            val url = decoder.decodeString()
            return when {
                url.startsWith("git://") -> GitRepository(repository = url.substring("git://".length))
                else -> throw SproutIllegalArgumentException(TranslationKey.of<BuildSystemInfo>("serialize.exception"), "url" to url)
            }
        }
    }

    class CachingRepositorySerializer(val workdir: Path, val repositories: List<IRepository>) : KSerializer<ICachingRepository> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(ICachingRepository::class.qualifiedName!!) {
            element<String>("type")
            element<String>("period")
        }

        override fun serialize(encoder: Encoder, value: ICachingRepository) {
            encoder.encodeStructure(descriptor) {
                when (value) {
                    is NoCacheRepository -> {
                        encodeStringElement(descriptor, 0, "no-cache")
                        encodeStringElement(descriptor, 1, "0")
                    }
                    is LocalCacheRepository -> {
                        encodeStringElement(descriptor, 0, "local-cache")
                        encodeStringElement(descriptor, 1, value.getInvalidationPeriod().toString())
                    }
                    else -> throw SproutIllegalArgumentException(TranslationKey.of<BuildSystemInfo>("serialize.exception"), "type" to value.javaClass.name)
                }
            }
        }

        override fun deserialize(decoder: Decoder): ICachingRepository {
            return decoder.decodeStructure(descriptor) {
                val url = decodeStringElement(descriptor, decodeElementIndex(descriptor))
                val invalidationPeriod = Duration.parse(decodeStringElement(descriptor, decodeElementIndex(descriptor)))
                when(url) {
                    "no-cache" -> NoCacheRepository(repositories)
                    "local-cache" -> LocalCacheRepository(workdir, repositories, invalidationPeriod)
                    else -> throw SproutIllegalArgumentException(TranslationKey.of<BuildSystemInfo>("serialize.exception"), "url" to url)
                }
            }
        }
    }

    companion object {
        val DEFAULT_WORKDIR: Path = Path("${System.getProperty("user.home")}/.sprout/").absolute().normalize()

        fun BuildSystemInfo(): BuildSystemInfo {
            val config = DEFAULT_WORKDIR.resolve("config.json")
            if (config.exists())
                return JsonUtils.fromJson(config.readText(Charsets.UTF_8))
            return BuildSystemInfo(DEFAULT_WORKDIR).also {
                config.writeText(JsonUtils.toJson(it), Charsets.UTF_8)
            }
        }
    }
}