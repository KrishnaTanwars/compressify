package com.compressify.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.compressify.app.data.model.CompressionPreset
import com.compressify.app.data.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getSavedThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _defaultPreset = MutableStateFlow(getSavedDefaultPreset())
    val defaultPreset: StateFlow<CompressionPreset> = _defaultPreset.asStateFlow()

    private val _defaultQuality = MutableStateFlow(getSavedDefaultQuality())
    val defaultQuality: StateFlow<Int> = _defaultQuality.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setDefaultPreset(preset: CompressionPreset) {
        prefs.edit().putString(KEY_DEFAULT_PRESET, preset.name).apply()
        _defaultPreset.value = preset
    }

    fun setDefaultQuality(quality: Int) {
        prefs.edit().putInt(KEY_DEFAULT_QUALITY, quality).apply()
        _defaultQuality.value = quality
    }

    private fun getSavedThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return runCatching { ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    private fun getSavedDefaultPreset(): CompressionPreset {
        val name = prefs.getString(KEY_DEFAULT_PRESET, CompressionPreset.BALANCED.name)
        return runCatching { CompressionPreset.valueOf(name ?: CompressionPreset.BALANCED.name) }
            .getOrDefault(CompressionPreset.BALANCED)
    }

    private fun getSavedDefaultQuality(): Int {
        return prefs.getInt(KEY_DEFAULT_QUALITY, 78)
    }

    companion object {
        private const val PREFS_NAME = "compressify_prefs"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_DEFAULT_PRESET = "key_default_preset"
        private const val KEY_DEFAULT_QUALITY = "key_default_quality"
    }
}
