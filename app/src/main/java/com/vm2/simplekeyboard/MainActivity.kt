package com.vm2.simplekeyboard

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Для старых Samsung отключаем анимации
        if (isOldSamsungDevice()) {
            window.setBackgroundDrawableResource(android.R.color.white)
            window.decorView.setBackgroundColor(Color.WHITE)
        }

        setContentView(R.layout.activity_main)

        val instructionsText = findViewById<TextView>(R.id.instructions_text)
        val activateButton = findViewById<Button>(R.id.activate_button)
        val openSettingsButton = findViewById<Button>(R.id.open_settings_button)

        // Явно устанавливаем цвета для старых устройств
        if (isOldSamsungDevice()) {
            instructionsText.setTextColor(Color.BLACK)
            // Можно добавить для других TextView если нужно
        }

        // Устанавливаем текст
        instructionsText.text = """
            Клавиатура с постоянным доступом к буферу обмена.
            
            Функции:
            • Буфер обмена всегда на виду
            • Быстрая вставка
            • Работает во всех приложениях
            
            Просто активируйте и пользуйтесь!
        """.trimIndent()

        // Кнопка для активации клавиатуры
        activateButton.setOnClickListener {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            startActivity(intent)
        }

        // Кнопка для настроек ввода
        openSettingsButton.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_INPUT_METHOD_SUBTYPE_SETTINGS)
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                startActivity(intent)
            }
        }
    }

    private fun isOldSamsungDevice(): Boolean {
        return Build.MANUFACTURER.equals("samsung", ignoreCase = true) &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.O // Android 8.0 и ниже
    }
}