package ru.pht.sprout.module.utils

import io.github.z4kn4fein.semver.constraints.Constraint
import io.github.z4kn4fein.semver.constraints.toConstraint
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.EnabledIf
import ru.pht.sprout.module.repo.IDownloadable
import ru.pht.sprout.module.repo.IRepository
import ru.pht.sprout.module.repo.cache.impl.NoCacheRepository.CombinedDownloadable
import ru.pht.sprout.module.repo.test.*
import kotlin.test.Test
import kotlin.test.assertEquals

@EnabledIf("ru.pht.sprout.TestConfigInternal#repoTest", disabledReason = "Тест выключен конфигурацией")
class RepoUtilsTest {
    @Test
    @DisplayName("Тестирование поиска / фильтрации / объединения")
    fun findFilterCombineTest() {
        findFilterCombineTest0 { repositories, name, version, combineAndAdd ->
            RepoUtils.findAndVerifySorted(repositories, name, version, combineAndAdd)
        }
    }


    @Test
    @DisplayName("Тестирование асинхронного поиска / фильтрации / объединения")
    fun findFilterCombineAsyncTest() = runTest {
        findFilterCombineTest0 { repositories, name, version, combineAndAdd ->
            RepoUtils.findAndVerifySortedAsync(repositories, name, version, combineAndAdd)
        }
    }

    @Test
    @DisplayName("Тестирование поиска / фильтрации / верификация / объединения")
    fun findFilterCombineVerifyTest() {
        findFilterCombineVerifyTest0 { repositories, name, version, combineAndAdd ->
            RepoUtils.findAndVerifySorted(repositories, name, version, combineAndAdd)
        }
    }

    @Test
    @DisplayName("Тестирование асинхронного поиска / фильтрации / верификация / объединения")
    fun findFilterCombineVerifyAsyncTest() = runTest {
        findFilterCombineVerifyTest0 { repositories, name, version, combineAndAdd ->
            RepoUtils.findAndVerifySortedAsync(repositories, name, version, combineAndAdd)
        }
    }

    @Test
    @DisplayName("Тестирование поиска / объединения")
    fun findAllTest() {
        findCombineTest0 { repositories, combineAndAddToVerified ->
            RepoUtils.findAllAndVerify(repositories, combineAndAddToVerified)
        }
    }


    @Test
    @DisplayName("Тестирование асинхронного поиска / объединения")
    fun findAllAsyncTest() = runTest {
        findCombineTest0 { repositories, combineAndAddToVerified ->
            RepoUtils.findAllAndVerifyAsync(repositories, combineAndAddToVerified)
        }
    }

    @Test
    @DisplayName("Тестирование поиска / верификация / объединения")
    fun findAllVerifyTest() {
        findCombineVerifyTest0 { repositories, combineAndAddToVerified ->
            RepoUtils.findAllAndVerify(repositories, combineAndAddToVerified)
        }
    }


    @Test
    @DisplayName("Тестирование асинхронного поиска / верификация / объединения")
    fun findAllVerifyAsyncTest() = runTest {
        findCombineVerifyTest0 { repositories, combineAndAddToVerified ->
            RepoUtils.findAllAndVerifyAsync(repositories, combineAndAddToVerified)
        }
    }

    private inline fun findFilterCombineTest0(
        method: (
            repositories: List<IRepository>,
            name: String,
            version: Constraint,
            combineAndAdd: (combine: List<IDownloadable>, addTo: (IDownloadable) -> Unit) -> Unit
        ) -> List<IDownloadable>
    ) {
        val list = method.invoke(
            listOf(TestRepositoryA, TestRepositoryC, TestRepositoryD, AssertNoCacheRepository),
            "test/a",
            ">=2.0.0".toConstraint(),
            CombinedDownloadable::combineAndAdd
        )
        assertEquals(list.size, 2)
        assertEquals(
            list.map { (it as CombinedDownloadable).originals },
            listOf(
                listOf(TestDownloadableA200A, TestDownloadableA200C),
                listOf(TestDownloadableA300A, TestDownloadableA300D)
            )
        )
    }

    private inline fun findFilterCombineVerifyTest0(
        method: (
            repositories: List<IRepository>,
            name: String,
            version: Constraint,
            combineAndAdd: (combine: List<IDownloadable>, addTo: (IDownloadable) -> Unit) -> Unit
        ) -> List<IDownloadable>
    ) {
        val list = method.invoke(
            listOf(TestRepositoryB, TestRepositoryD, TestRepositoryDCrack),
            "test/b",
            "2.0.0".toConstraint(),
            CombinedDownloadable::combineAndAdd
        )
        assertEquals(list.size, 1)
        assertEquals(
            list.map { (it as CombinedDownloadable).originals },
            listOf(listOf(
                TestDownloadableB200B,
                TestDownloadableB200D,
                // Компрометация идёт со стороны репозитория - хеш не совпадёт с остальными репозиториями.
                // Это выявляется в момент получения ссылки, поэтому сам ресурс не проходит.
//                TestDownloadableB200DCrack
            ))
        )
    }

    private inline fun findCombineTest0(
        method: (
            repositories: List<IRepository>,
            combineAndAddToVerified: (List<IDownloadable>) -> Unit
        ) -> Unit
    ) {
        val list: MutableList<IDownloadable> = ArrayList()
        method.invoke(listOf(TestRepositoryA, TestRepositoryC, TestRepositoryD, AssertNoCacheRepository)) { list += it }
        assertEquals(list.size, 14)
        assertEquals(
            list.sortedBy { it.hashCode() },
            listOf<IDownloadable>(
                TestDownloadableA100A, TestDownloadableA110A, TestDownloadableA200A, TestDownloadableA300A, TestDownloadableB100A, TestDownloadableB200A, TestDownloadableC100A, TestDownloadableD100A,
                TestDownloadableA100C, TestDownloadableA110C, TestDownloadableA200C, TestDownloadableB100C,
                TestDownloadableA300D, TestDownloadableB200D
            ).sortedBy { it.hashCode() }
        )
    }


    private inline fun findCombineVerifyTest0(
        method: (
            repositories: List<IRepository>,
            combineAndAddToVerified: (List<IDownloadable>) -> Unit
        ) -> Unit
    ) {
        val list: MutableList<IDownloadable> = ArrayList()
        method.invoke(listOf(TestRepositoryB, TestRepositoryD, TestRepositoryDCrack)) { list += it }
        assertEquals(list.size, 9)
        assertEquals(
            list.sortedBy { it.hashCode() },
            listOf<IDownloadable>(
                TestDownloadableA100B, TestDownloadableA110B, TestDownloadableA200B, TestDownloadableA300B, TestDownloadableB100B, TestDownloadableB200B,
                TestDownloadableA300D, TestDownloadableB200D,
                // Компрометация идёт со стороны пользователя - хеш архива не совпадёт при проверке после его загрузки.
                // Это выявляется в момент загрузки архива, поэтому сам ресурс проходит.
                TestDownloadableA300DCrack,
                // Компрометация идёт со стороны репозитория - хеш не совпадёт с остальными репозиториями.
                // Это выявляется в момент получения ссылки, поэтому сам ресурс не проходит.
//                TestDownloadableB200DCrack
            ).sortedBy { it.hashCode() }
        )
    }
}