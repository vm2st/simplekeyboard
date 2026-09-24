package com.vm2.simplekeyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardLayoutTest {
    @Test
    fun russianLayoutContainsEveryRussianLetterOnce() {
        val letters = (KeyboardLayout.firstRow(KeyboardLanguage.RUSSIAN) +
            KeyboardLayout.secondRow(KeyboardLanguage.RUSSIAN) +
            KeyboardLayout.thirdRow(KeyboardLanguage.RUSSIAN)).joinToString("")

        assertEquals(33, letters.length)
        assertEquals(33, letters.toSet().size)
        assertTrue(letters.contains('ё'))
        assertTrue(letters.contains('ъ'))
        assertTrue(letters.contains('ь'))
    }

    @Test
    fun languageToggleAlternatesBetweenEnglishAndRussian() {
        assertEquals(KeyboardLanguage.RUSSIAN, KeyboardLanguage.ENGLISH.next())
        assertEquals(KeyboardLanguage.ENGLISH, KeyboardLanguage.RUSSIAN.next())
    }

    @Test
    fun englishLayoutKeepsStandardRows() {
        assertEquals("qwertyuiop", KeyboardLayout.firstRow(KeyboardLanguage.ENGLISH).joinToString(""))
        assertEquals("asdfghjkl", KeyboardLayout.secondRow(KeyboardLanguage.ENGLISH).joinToString(""))
        assertEquals("zxcvbnm", KeyboardLayout.thirdRow(KeyboardLanguage.ENGLISH).joinToString(""))
    }
}
