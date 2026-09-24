package com.vm2.simplekeyboard

/**
 * Keyboard layouts are kept as data instead of being duplicated in the input service.
 * This makes language switching deterministic and easy to test.
 */
enum class KeyboardLanguage(
    val code: String,
    val label: String
) {
    ENGLISH("EN", "English"),
    RUSSIAN("RU", "Русский");

    fun next(): KeyboardLanguage =
        if (this == ENGLISH) RUSSIAN else ENGLISH
}

object KeyboardLayout {
    fun firstRow(language: KeyboardLanguage): List<String> =
        when (language) {
            KeyboardLanguage.ENGLISH -> "qwertyuiop".map(Char::toString)
            KeyboardLanguage.RUSSIAN -> "йцукенгшщзхъ".map(Char::toString)
        }

    fun secondRow(language: KeyboardLanguage): List<String> =
        when (language) {
            KeyboardLanguage.ENGLISH -> "asdfghjkl".map(Char::toString)
            KeyboardLanguage.RUSSIAN -> "фывапролджэ".map(Char::toString)
        }

    fun thirdRow(language: KeyboardLanguage): List<String> =
        when (language) {
            KeyboardLanguage.ENGLISH -> "zxcvbnm".map(Char::toString)
            KeyboardLanguage.RUSSIAN -> "ёячсмитьбю".map(Char::toString)
        }
}
