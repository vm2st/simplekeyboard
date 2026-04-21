package com.vm2.simplekeyboard

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.content.ClipboardManager
import android.content.Context
import android.view.KeyEvent

class CustomKeyboard : InputMethodService() {

    private lateinit var clipboardLayout: LinearLayout
    private lateinit var keyboardLayout: LinearLayout
    private lateinit var clipboardManager: ClipboardManager
    private var currentClip: String = ""

    override fun onCreateInputView(): View {
        // Создаем основной контейнер
        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Создаем область для буфера обмена
        clipboardLayout = LinearLayout(this)
        clipboardLayout.orientation = LinearLayout.HORIZONTAL
        clipboardLayout.setBackgroundColor(0xFFCCCCCC.toInt())
        clipboardLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val clipboardLabel = TextView(this)
        clipboardLabel.text = "Буфер: "
        clipboardLabel.setPadding(10, 10, 5, 10)
        clipboardLabel.textSize = 12f  // Уменьшаем текст

        val clipboardText = TextView(this)
        clipboardText.id = android.R.id.text1
        clipboardText.setPadding(5, 10, 5, 10)
        clipboardText.textSize = 12f  // Уменьшаем текст
        clipboardText.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val pasteButton = Button(this)
        pasteButton.text = "Вст."
        pasteButton.textSize = 11f  // Уменьшаем текст
        pasteButton.setPadding(5, 5, 5, 5)
        pasteButton.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        pasteButton.setOnClickListener {
            pasteFromClipboard()
        }

        clipboardLayout.addView(clipboardLabel)
        clipboardLayout.addView(clipboardText)
        clipboardLayout.addView(pasteButton)

        // Создаем клавиатуру с кнопками
        keyboardLayout = LinearLayout(this)
        keyboardLayout.orientation = LinearLayout.VERTICAL
        keyboardLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        // Создаем ряды клавиш с использованием веса (weight)
        // Первый ряд: q w e r t y u i o p
        val row1 = createCompactRow(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"))

        // Второй ряд: a s d f g h j k l DEL
        val row2 = LinearLayout(this)
        row2.orientation = LinearLayout.HORIZONTAL
        row2.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val row2Keys = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
        for (key in row2Keys) {
            val button = createCompactButton(key, 0, 1f)
            button.setOnClickListener { insertText(key) }
            row2.addView(button)
        }

        val deleteButton = createCompactButton("⌫", LinearLayout.LayoutParams.WRAP_CONTENT)
        deleteButton.setOnClickListener { deleteText() }
        row2.addView(deleteButton)

        // Третий ряд: z x c v b n m SPACE
        val row3 = LinearLayout(this)
        row3.orientation = LinearLayout.HORIZONTAL
        row3.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val row3Keys = listOf("z", "x", "c", "v", "b", "n", "m")
        for (key in row3Keys) {
            val button = createCompactButton(key, 0, 1f)
            button.setOnClickListener { insertText(key) }
            row3.addView(button)
        }

        val spaceButton = createCompactButton("_____", 0, 2f)  // Пробел с большим весом
        spaceButton.setOnClickListener { insertText(" ") }
        row3.addView(spaceButton)

        // Четвертый ряд: только ENTER (широкая кнопка)
        val row4 = LinearLayout(this)
        row4.orientation = LinearLayout.HORIZONTAL
        row4.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        val enterButton = createCompactButton("ВВОД", 0, 10f)  // Очень большой вес
        enterButton.setOnClickListener { sendEnter() }
        row4.addView(enterButton)

        // Добавляем все ряды
        keyboardLayout.addView(row1)
        keyboardLayout.addView(row2)
        keyboardLayout.addView(row3)
        keyboardLayout.addView(row4)

        // Добавляем все в основной контейнер
        mainLayout.addView(clipboardLayout)
        mainLayout.addView(keyboardLayout)

        // Инициализируем менеджер буфера обмена
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Начинаем отслеживать буфер обмена
        startClipboardMonitoring()

        return mainLayout
    }

    private fun createCompactRow(keys: List<String>): LinearLayout {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        for (key in keys) {
            val button = createCompactButton(key, 0, 1f)
            button.setOnClickListener { insertText(key) }
            row.addView(button)
        }

        return row
    }

    private fun createCompactButton(text: String, width: Int = 0, weight: Float = 1f): Button {
        val button = Button(this)
        button.text = text
        button.setPadding(4, 12, 4, 12)  // Увеличили вертикальные отступы
        button.textSize = 14f  // Увеличили размер текста
        button.minWidth = 0
        button.minimumWidth = 0

        // Для маленьких экранов делаем текст еще меньше
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels / displayMetrics.density

        if (screenWidth < 360) { // Маленький экран
            button.textSize = 12f
            button.setPadding(2, 8, 2, 8)
        }

        val params = LinearLayout.LayoutParams(
            width,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            weight
        )
        params.setMargins(1, 1, 1, 1)
        button.layoutParams = params

        return button
    }

    // Остальные методы остаются без изменений...
    private fun startClipboardMonitoring() {
        updateClipboardDisplay()

        clipboardManager.addPrimaryClipChangedListener {
            updateClipboardDisplay()
        }
    }

    private fun updateClipboardDisplay() {
        try {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text.toString()
                if (text.isNotEmpty()) {
                    currentClip = text
                    val clipboardText = clipboardLayout.findViewById<TextView>(android.R.id.text1)
                    // Показываем только первые 10 символов в портретном режиме
                    val displayText = if (text.length > 15) {
                        text.substring(0, 15) + "..."
                    } else {
                        text
                    }
                    clipboardText.text = displayText
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun pasteFromClipboard() {
        val ic: InputConnection? = currentInputConnection
        ic?.commitText(currentClip, 1)
    }

    private fun insertText(text: String) {
        val ic: InputConnection? = currentInputConnection
        ic?.commitText(text, 1)
    }

    private fun deleteText() {
        val ic: InputConnection? = currentInputConnection
        ic?.deleteSurroundingText(1, 0)
    }

    private fun sendEnter() {
        val ic: InputConnection? = currentInputConnection
        ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        ic?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }
}