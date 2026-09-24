package com.vm2.simplekeyboard

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

private enum class AppTheme(
    val preferenceValue: String,
    val labelRes: Int,
    val styleRes: Int
) {
    ACCENT("accent", R.string.theme_accent, R.style.Theme_SimpleKeyboard_Accent),
    LIGHT("light", R.string.theme_light, R.style.Theme_SimpleKeyboard_Light),
    DARK("dark", R.string.theme_dark, R.style.Theme_SimpleKeyboard_Dark);

    companion object {
        fun from(value: String?): AppTheme =
            entries.firstOrNull { it.preferenceValue == value } ?: ACCENT
    }
}

class MainActivity : AppCompatActivity() {

    private val preferencesName = "keyboard_preferences"
    private val themePreferenceKey = "app_theme"
    private lateinit var statusText: TextView
    private lateinit var themeStatus: TextView
    private lateinit var selectedTheme: AppTheme

    override fun onCreate(savedInstanceState: Bundle?) {
        selectedTheme = AppTheme.from(
            getSharedPreferences(preferencesName, MODE_PRIVATE)
                .getString(themePreferenceKey, AppTheme.ACCENT.preferenceValue)
        )
        AppCompatDelegate.setDefaultNightMode(
            if (selectedTheme == AppTheme.DARK) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
        )
        setTheme(selectedTheme.styleRes)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        applySystemBarInsets()
        bindActions()
        updateKeyboardStatus()
        updateThemeStatus()
    }

    override fun onResume() {
        super.onResume()
        if (::statusText.isInitialized) {
            updateKeyboardStatus()
        }
    }

    private fun bindActions() {
        statusText = findViewById(R.id.keyboard_status)
        themeStatus = findViewById(R.id.theme_status)

        findViewById<Button>(R.id.activate_button).setOnClickListener {
            openInputMethodSettings()
        }
        findViewById<Button>(R.id.open_settings_button).setOnClickListener {
            openInputMethodSettings()
        }
        findViewById<Button>(R.id.show_keyboard_button).setOnClickListener {
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showInputMethodPicker()
        }
        findViewById<Button>(R.id.theme_accent_button).setOnClickListener {
            selectTheme(AppTheme.ACCENT)
        }
        findViewById<Button>(R.id.theme_light_button).setOnClickListener {
            selectTheme(AppTheme.LIGHT)
        }
        findViewById<Button>(R.id.theme_dark_button).setOnClickListener {
            selectTheme(AppTheme.DARK)
        }
    }

    /**
     * Android 15/16 draws edge-to-edge for apps targeting recent SDKs.
     * Apply system-bar insets to the scrolling container so the title and
     * subtitle never render underneath the status bar or navigation area.
     */
    private fun applySystemBarInsets() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val scrollView = findViewById<ScrollView>(R.id.root_scroll)
        val left = scrollView.paddingLeft
        val top = scrollView.paddingTop
        val right = scrollView.paddingRight
        val bottom = scrollView.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(scrollView) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                left + systemBars.left,
                top + systemBars.top,
                right + systemBars.right,
                bottom + systemBars.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(scrollView)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        val isDark = selectedTheme == AppTheme.DARK
        insetsController.isAppearanceLightStatusBars = !isDark
        insetsController.isAppearanceLightNavigationBars = !isDark
    }

    private fun selectTheme(theme: AppTheme) {
        if (theme == selectedTheme) return

        getSharedPreferences(preferencesName, MODE_PRIVATE)
            .edit()
            .putString(themePreferenceKey, theme.preferenceValue)
            .apply()
        recreate()
    }

    private fun updateThemeStatus() {
        themeStatus.text = getString(R.string.theme_selected, getString(selectedTheme.labelRes))
    }

    private fun openInputMethodSettings() {
        startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
    }

    private fun updateKeyboardStatus() {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val isEnabled = inputMethodManager.enabledInputMethodList.any { info: InputMethodInfo ->
            info.packageName == packageName
        }
        statusText.text = getString(
            if (isEnabled) R.string.keyboard_enabled else R.string.keyboard_not_enabled
        )
        statusText.setTextColor(
            getColor(if (isEnabled) R.color.status_success else R.color.status_warning)
        )
        statusText.visibility = View.VISIBLE
    }
}
