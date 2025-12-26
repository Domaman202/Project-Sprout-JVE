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
import ru.pht.sprout.utils.exception.SproutIllegalArgumentException
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

@Serializable(BuildSystemInfo.Serializer::class)
class BuildSystemInfo(
    val workdir: String = Path("${System.getProperty("user.home")}/.sprout/").absolutePathString(),
    val repositories: List<IRepository> = mutableListOf(GitRepository.github(), GitRepository.gitflic()),
    val cachingRepository: ICachingRepository = LocalCacheRepository(workdir, repositories)
) {
    class Serializer : KSerializer<BuildSystemInfo> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor(BuildSystemInfo::class.qualifiedName!!) {
            element<String>("workdir")
            element<List<IRepository>>("repositories")
            element<ICachingRepository>("cachingRepository")
        }

        override fun serialize(encoder: Encoder, value: BuildSystemInfo) {
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, value.workdir)
                encodeSerializableElement(descriptor, 1, ListSerializer(RepositorySerializer), value.repositories)
                encodeSerializableElement(descriptor, 2, CachingRepositorySerializer(value.workdir, value.repositories), value.cachingRepository)
            }
        }

        override fun deserialize(decoder: Decoder): BuildSystemInfo {
            return decoder.decodeStructure(descriptor) {
                val workdir = decodeStringElement(descriptor, decodeElementIndex(descriptor))
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

    class CachingRepositorySerializer(val workdir: String, val repositories: List<IRepository>) : KSerializer<ICachingRepository> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(ICachingRepository::class.qualifiedName!!, PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: ICachingRepository) {
            when (value) {
                is NoCacheRepository -> encoder.encodeString("no-cache")
                is LocalCacheRepository -> encoder.encodeString("local-cache")
                else -> throw SproutIllegalArgumentException(TranslationKey.of<BuildSystemInfo>("serialize.exception"), "type" to value.javaClass.name)
            }
        }

        override fun deserialize(decoder: Decoder): ICachingRepository {
            return when(val url = decoder.decodeString()) {
                "no-cache" -> NoCacheRepository(this.repositories)
                "local-cache" -> LocalCacheRepository(this.workdir, this.repositories)
                else -> throw SproutIllegalArgumentException(TranslationKey.of<BuildSystemInfo>("serialize.exception"), "url" to url)
            }
        }
    }
}