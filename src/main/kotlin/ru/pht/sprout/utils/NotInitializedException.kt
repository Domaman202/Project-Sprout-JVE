package ru.pht.sprout.utils

class NotInitializedException(val field: String) : RuntimeException("Неинициализированное обязательное поле '$field'")