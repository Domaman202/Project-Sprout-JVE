package ru.pht.sprout

import ru.pht.sprout.TestConfig.Common.FMT_TEST
import ru.pht.sprout.TestConfig.Common.OTHER_UTILS_TEST
import ru.pht.sprout.TestConfig.Common.TRANSLATE_TEST
import ru.pht.sprout.TestConfig.Module.HEADER_PARSE_TEST
import ru.pht.sprout.TestConfig.Module.REAL_NET_TEST
import ru.pht.sprout.TestConfig.Module.REPO_TEST
import ru.pht.sprout.TestConfig.Module.ZIP_TEST

object TestConfigInternal {
    @JvmStatic fun fmtTest(): Boolean = FMT_TEST
    @JvmStatic fun translateTest(): Boolean = TRANSLATE_TEST
    @JvmStatic fun otherUtilsTest(): Boolean = OTHER_UTILS_TEST
    @JvmStatic fun zipTest(): Boolean = ZIP_TEST
    @JvmStatic fun realNetTest(): Boolean = REAL_NET_TEST
    @JvmStatic fun headerParseTest(): Boolean = HEADER_PARSE_TEST
    @JvmStatic fun repoTest(): Boolean = REPO_TEST
    @JvmStatic fun realNetRepoTest(): Boolean = repoTest() and realNetTest()
}