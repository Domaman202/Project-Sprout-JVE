package ru.pht.sprout

object TestConfig {
    // ========== [(ОБЩЕЕ)] ========== //
    object Common {
        // ===== (ТЕКСТ) ===== //
        // Тесты на форматирование
        const val FMT_TEST = true
        // Тесты на перевод
        const val TRANSLATE_TEST = true
        // ===== (ОСТАЛЬНЫЕ УТИЛИТЫ) ===== //
        // Тесты остальных утилит
        const val OTHER_UTILS_TEST = true
    }

    // ========== [(МОДУЛЬ)] ========== //
    object Module {
        // ===== (АРХИВАЦИЯ) ===== //
        // Тесты с архивацией данных
        const val ZIP_TEST = true
        // ===== (ГРАФЫ) ===== //
        // Тесты с графами
        const val GRAPH_TEST = true
        // ===== (ЗАГОЛОВОК) ===== //
        // Тесты парсинга заголовка
        const val HEADER_PARSE_TEST = true
        // ===== (РЕПОЗИТОРИИ) ===== //
        // Тесты репозиториев
        const val REPO_TEST = true
        // ===== (СЕТЬ) ===== //
        // Тесты с реальными сетевыми запросами.
        const val REAL_NET_TEST = false
    }
}