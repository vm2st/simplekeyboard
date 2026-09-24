package com.vm2.simplekeyboard

import android.content.ClipboardManager
import android.content.ClipDescription
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import kotlin.math.roundToInt

/**
 * A small, privacy-conscious keyboard.
 *
 * Clipboard contents are read only after the user taps Paste. The old
 * implementation continuously observed the clipboard and kept its contents
 * in memory, which was unnecessary for the keyboard to work.
 */
class CustomKeyboard : InputMethodService() {

    private lateinit var rootLayout: LinearLayout
    private lateinit var keyboardLayout: LinearLayout
    private lateinit var languageButton: Button
    private lateinit var clipboardStatus: TextView

    private var language = KeyboardLanguage.RUSSIAN

    private val horizontalPaddingDp = 6
    private val keyGapDp = 3
    private val maxClipboardCharacters = 100_000
    private val preferencesName = "keyboard_preferences"
    private val languagePreferenceKey = "language"

    override fun onCreate() {
        super.onCreate()
        language = getSharedPreferences(preferencesName, MODE_PRIVATE)
            .getString(languagePreferenceKey, KeyboardLanguage.RUSSIAN.code)
            ?.let { code -> KeyboardLanguage.values().firstOrNull { it.code == code } }
            ?: KeyboardLanguage.RUSSIAN
    }

    override fun onCreateInputView(): View {
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(horizontalPaddingDp), dp(4), dp(horizontalPaddingDp), dp(6))
            setBackgroundColor(ContextCompat.getColor(context, R.color.keyboard_surface))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }

        rootLayout.addView(createToolbar())

        keyboardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        rootLayout.addView(keyboardLayout)
        rebuildKeyboard()

        return rootLayout
    }

    private fun createToolbar(): View {
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), 0, dp(4), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        }

        clipboardStatus = TextView(this).apply {
            text = getString(R.string.clipboard_ready)
            textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.keyboard_text_secondary))
            gravity = Gravity.CENTER_VERTICAL
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            ).apply {
                marginEnd = dp(6)
            }
        }

        val pasteButton = createActionButton(
            text = getString(R.string.paste),
            contentDescription = getString(R.string.paste)
        ) {
            pasteFromClipboard()
        }
        pasteButton.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            dp(40)
        ).apply {
            marginEnd = dp(6)
        }

        languageButton = createActionButton(
            text = language.code,
            contentDescription = getString(R.string.switch_language)
        ) {
            language = language.next()
            getSharedPreferences(preferencesName, MODE_PRIVATE)
                .edit { putString(languagePreferenceKey, language.code) }
            updateLanguageButton()
            rebuildKeyboard()
        }
        languageButton.layoutParams = LinearLayout.LayoutParams(dp(52), dp(40))

        toolbar.addView(clipboardStatus)
        toolbar.addView(pasteButton)
        toolbar.addView(languageButton)
        return toolbar
    }

    private fun rebuildKeyboard() {
        keyboardLayout.removeAllViews()

        keyboardLayout.addView(createLetterRow(KeyboardLayout.firstRow(language)))

        val secondRow = createLetterRow(KeyboardLayout.secondRow(language))
        secondRow.addView(
            createKeyButton(
                label = "⌫",
                contentDescription = getString(R.string.delete),
                weight = 1f
            ) { deleteText() }
        )
        keyboardLayout.addView(secondRow)

        val thirdRow = createLetterRow(KeyboardLayout.thirdRow(language))
        // Enter intentionally occupies the former space position.
        thirdRow.addView(
            createKeyButton(
                label = getString(R.string.enter),
                contentDescription = getString(R.string.enter),
                weight = 2f,
                actionKey = true
            ) { sendEnter() }
        )
        keyboardLayout.addView(thirdRow)

        // Space intentionally occupies the former enter position.
        val spaceRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = rowLayoutParams()
        }
        spaceRow.addView(
            createKeyButton(
                label = getString(R.string.space),
                contentDescription = getString(R.string.space),
                weight = 1f,
                actionKey = true
            ) { insertText(" ") }
        )
        keyboardLayout.addView(spaceRow)
    }

    private fun createLetterRow(keys: List<String>): LinearLayout {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = rowLayoutParams()
        }

        keys.forEach { key ->
            row.addView(
                createKeyButton(
                    label = key,
                    contentDescription = key,
                    weight = 1f
                ) { insertText(key) }
            )
        }
        return row
    }

    private fun createKeyButton(
        label: String,
        contentDescription: String,
        weight: Float,
        actionKey: Boolean = false,
        onClick: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            this.contentDescription = contentDescription
            isAllCaps = false
            textSize = if (actionKey) 12f else 18f
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (actionKey) R.color.keyboard_action_text else R.color.keyboard_text
                )
            )
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            stateListAnimator = null
            background = createKeyBackground(actionKey)
            setPadding(dp(2), dp(2), dp(2), dp(2))
            layoutParams = LinearLayout.LayoutParams(0, dp(52), weight).apply {
                setMargins(dp(keyGapDp / 2), dp(2), dp(keyGapDp / 2), dp(2))
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createActionButton(
        text: String,
        contentDescription: String,
        onClick: () -> Unit
    ): Button {
        return Button(this).apply {
            this.text = text
            this.contentDescription = contentDescription
            isAllCaps = false
            textSize = 12f
            minWidth = 0
            minimumWidth = 0
            minHeight = 0
            minimumHeight = 0
            setTextColor(ContextCompat.getColor(context, R.color.keyboard_action_text))
            background = createKeyBackground(true)
            setPadding(dp(8), 0, dp(8), 0)
            setOnClickListener { onClick() }
        }
    }

    private fun createKeyBackground(actionKey: Boolean): Drawable {
        val keyBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(if (actionKey) 12 else 10).toFloat()
            setColor(
                ContextCompat.getColor(
                    this@CustomKeyboard,
                    if (actionKey) R.color.keyboard_action else R.color.keyboard_key
                )
            )
            setStroke(dp(1), ContextCompat.getColor(this@CustomKeyboard, R.color.keyboard_border))
        }
        return RippleDrawable(
            ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.keyboard_border)
            ),
            keyBackground,
            null
        )
    }

    private fun rowLayoutParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

    private fun updateLanguageButton() {
        languageButton.text = language.code
        languageButton.contentDescription = getString(R.string.switch_language)
    }

    private fun pasteFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return
        val description = clipboard.primaryClipDescription
        if (description == null ||
            (!description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
                !description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML))
        ) {
            clipboardStatus.text = getString(R.string.clipboard_not_text)
            return
        }
        val clip = clipboard.primaryClip
        val item = clip?.takeIf { it.itemCount > 0 }?.getItemAt(0)
        val text = item?.coerceToText(this)?.toString().orEmpty()

        if (text.isEmpty()) {
            clipboardStatus.text = getString(R.string.clipboard_empty)
            return
        }

        val safeText = text.take(maxClipboardCharacters)
        currentInputConnection?.commitText(safeText, 1)
        clipboardStatus.text = if (text.length > maxClipboardCharacters) {
            getString(R.string.pasted_truncated)
        } else {
            getString(R.string.pasted)
        }
    }

    private fun insertText(text: String) {
        currentInputConnection?.commitText(text, 1)
    }

    private fun deleteText() {
        val inputConnection = currentInputConnection ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            inputConnection.deleteSurroundingTextInCodePoints(1, 0)
        } else {
            inputConnection.deleteSurroundingText(1, 0)
        }
    }

    private fun sendEnter() {
        val inputConnection = currentInputConnection ?: return
        val editorInfo = currentInputEditorInfo
        val imeOptions = editorInfo?.imeOptions ?: EditorInfo.IME_ACTION_NONE
        val action = imeOptions and EditorInfo.IME_MASK_ACTION
        val noEnterAction = imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0

        if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE &&
            action != EditorInfo.IME_ACTION_UNSPECIFIED
        ) {
            inputConnection.performEditorAction(action)
        } else {
            inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()
}
