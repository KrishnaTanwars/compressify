package com.compressify.app.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.compressify.app.data.model.CompressionConfig
import com.compressify.app.data.model.CompressionPreset
import com.compressify.app.data.model.CompressionResult
import com.compressify.app.data.model.ImageMetadata
import com.compressify.app.data.model.ThemeMode
import com.compressify.app.data.repository.PreferencesRepository
import com.compressify.app.data.storage.FileStorageManager
import com.compressify.app.domain.compressor.ImageCompressor
import com.compressify.app.domain.compressor.ImageCompressorImpl
import com.compressify.app.ui.navigation.Screen
import com.compressify.app.utils.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CompressorViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application)
    private val compressor: ImageCompressor = ImageCompressorImpl(application)
    private val storageManager = FileStorageManager(application)

    val themeMode: StateFlow<ThemeMode> = preferencesRepository.themeMode
    val defaultPreset: StateFlow<CompressionPreset> = preferencesRepository.defaultPreset

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedImage = MutableStateFlow<ImageMetadata?>(null)
    val selectedImage: StateFlow<ImageMetadata?> = _selectedImage.asStateFlow()

    private val _compressionConfig = MutableStateFlow(
        CompressionConfig(
            preset = preferencesRepository.defaultPreset.value,
            quality = preferencesRepository.defaultPreset.value.defaultQuality
        )
    )
    val compressionConfig: StateFlow<CompressionConfig> = _compressionConfig.asStateFlow()

    private val _compressionResult = MutableStateFlow<CompressionResult?>(null)
    val compressionResult: StateFlow<CompressionResult?> = _compressionResult.asStateFlow()

    private val _isCompressing = MutableStateFlow(false)
    val isCompressing: StateFlow<Boolean> = _isCompressing.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isSaved = MutableStateFlow(false)
    val isSaved: StateFlow<Boolean> = _isSaved.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        // Periodic cleanup of cached files older than 24 hours
        viewModelScope.launch {
            FileUtils.cleanOldCache(application)
        }
    }

    fun onImageSelected(uri: Uri) {
        viewModelScope.launch {
            _errorMessage.value = null
            _isCompressing.value = true
            compressor.extractMetadata(uri).fold(
                onSuccess = { metadata ->
                    _selectedImage.value = metadata
                    _compressionConfig.value = CompressionConfig(
                        preset = preferencesRepository.defaultPreset.value,
                        quality = preferencesRepository.defaultPreset.value.defaultQuality
                    )
                    _isCompressing.value = false
                    _currentScreen.value = Screen.Preview
                },
                onFailure = { _ ->
                    _isCompressing.value = false
                    _errorMessage.value = "Unable to read this image. Please select a valid photo."
                }
            )
        }
    }

    fun selectPreset(preset: CompressionPreset) {
        if (preset == CompressionPreset.CUSTOM) {
            _compressionConfig.value = _compressionConfig.value.copy(
                preset = preset,
                quality = _compressionConfig.value.quality
            )
        } else {
            _compressionConfig.value = _compressionConfig.value.copy(
                preset = preset,
                quality = preset.defaultQuality
            )
        }
    }

    fun setCustomQuality(quality: Int) {
        val clamped = quality.coerceIn(10, 100)
        _compressionConfig.value = _compressionConfig.value.copy(
            preset = CompressionPreset.CUSTOM,
            quality = clamped
        )
    }

    fun setTargetSizeBytes(targetBytes: Long?) {
        _compressionConfig.value = _compressionConfig.value.copy(
            preset = CompressionPreset.TARGET_SIZE,
            targetSizeBytes = targetBytes
        )
    }

    fun compressImage() {
        val metadata = _selectedImage.value ?: return
        if (_isCompressing.value) return // Prevent duplicate taps

        viewModelScope.launch {
            _isCompressing.value = true
            _errorMessage.value = null

            compressor.compress(metadata, _compressionConfig.value).fold(
                onSuccess = { result ->
                    _compressionResult.value = result
                    _isSaved.value = false
                    _isCompressing.value = false
                    _currentScreen.value = Screen.Result
                },
                onFailure = { error ->
                    _isCompressing.value = false
                    val humanMessage = when {
                        error.message?.contains("large", ignoreCase = true) == true ->
                            "This image is too large to process on this device. Try another image."
                        else -> "Compression failed. Please try a different preset or image."
                    }
                    _errorMessage.value = humanMessage
                }
            )
        }
    }

    fun saveResult() {
        val result = _compressionResult.value ?: return
        if (_isSaving.value || _isSaved.value) return

        viewModelScope.launch {
            _isSaving.value = true
            storageManager.saveToGallery(result).fold(
                onSuccess = {
                    _isSaving.value = false
                    _isSaved.value = true
                    _toastMessage.value = "Saved to Pictures/Compressify"
                },
                onFailure = {
                    _isSaving.value = false
                    _errorMessage.value = "Failed to save image to gallery. Please check storage space."
                }
            )
        }
    }

    fun getShareIntent(): Intent? {
        val result = _compressionResult.value ?: return null
        return storageManager.createShareIntent(result)
    }

    fun resetToHome() {
        _selectedImage.value = null
        _compressionResult.value = null
        _errorMessage.value = null
        _isSaved.value = false
        _currentScreen.value = Screen.Home
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun updateThemeMode(mode: ThemeMode) {
        preferencesRepository.setThemeMode(mode)
    }

    fun updateDefaultPreset(preset: CompressionPreset) {
        preferencesRepository.setDefaultPreset(preset)
    }
}
