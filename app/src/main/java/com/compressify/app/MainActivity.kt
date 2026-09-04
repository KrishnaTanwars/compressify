package com.compressify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.compressify.app.ui.home.HomeScreen
import com.compressify.app.ui.navigation.Screen
import com.compressify.app.ui.preview.PreviewScreen
import com.compressify.app.ui.result.ResultScreen
import com.compressify.app.ui.settings.SettingsScreen
import com.compressify.app.ui.theme.CompressifyTheme
import com.compressify.app.ui.viewmodel.CompressorViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CompressorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()

            CompressifyTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        viewModel = viewModel,
                        onShare = {
                            viewModel.getShareIntent()?.let { intent ->
                                startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: CompressorViewModel,
    onShare: () -> Unit
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedImage by viewModel.selectedImage.collectAsState()
    val compressionConfig by viewModel.compressionConfig.collectAsState()
    val compressionResult by viewModel.compressionResult.collectAsState()
    val isCompressing by viewModel.isCompressing.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val defaultPreset by viewModel.defaultPreset.collectAsState()

    // Handle Android system back gesture
    BackHandler(enabled = currentScreen != Screen.Home) {
        when (currentScreen) {
            Screen.Preview -> viewModel.resetToHome()
            Screen.Result -> viewModel.resetToHome()
            Screen.Settings -> {
                if (selectedImage != null) {
                    viewModel.navigateTo(Screen.Preview)
                } else {
                    viewModel.resetToHome()
                }
            }
            Screen.Home -> {}
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screenTransition"
    ) { screen ->
        when (screen) {
            Screen.Home -> {
                HomeScreen(
                    isLoading = isCompressing,
                    errorMessage = errorMessage,
                    onImageSelected = { uri -> viewModel.onImageSelected(uri) },
                    onSettingsClick = { viewModel.navigateTo(Screen.Settings) },
                    onClearError = { viewModel.clearError() }
                )
            }

            Screen.Preview -> {
                selectedImage?.let { metadata ->
                    PreviewScreen(
                        metadata = metadata,
                        config = compressionConfig,
                        isCompressing = isCompressing,
                        errorMessage = errorMessage,
                        onPresetSelected = { preset -> viewModel.selectPreset(preset) },
                        onCustomQualityChanged = { quality -> viewModel.setCustomQuality(quality) },
                        onTargetSizeChanged = { bytes -> viewModel.setTargetSizeBytes(bytes) },
                        onCompressClick = { viewModel.compressImage() },
                        onBackClick = { viewModel.resetToHome() },
                        onClearError = { viewModel.clearError() }
                    )
                } ?: viewModel.resetToHome()
            }

            Screen.Result -> {
                compressionResult?.let { result ->
                    ResultScreen(
                        result = result,
                        isSaving = isSaving,
                        isSaved = isSaved,
                        toastMessage = toastMessage,
                        errorMessage = errorMessage,
                        onSaveClick = { viewModel.saveResult() },
                        onShareClick = onShare,
                        onCompressAnotherClick = { viewModel.resetToHome() },
                        onBackClick = { viewModel.resetToHome() },
                        onClearToast = { viewModel.clearToast() },
                        onClearError = { viewModel.clearError() }
                    )
                } ?: viewModel.resetToHome()
            }

            Screen.Settings -> {
                SettingsScreen(
                    themeMode = themeMode,
                    defaultPreset = defaultPreset,
                    onThemeModeChange = { mode -> viewModel.updateThemeMode(mode) },
                    onDefaultPresetChange = { preset -> viewModel.updateDefaultPreset(preset) },
                    onBackClick = {
                        if (selectedImage != null) {
                            viewModel.navigateTo(Screen.Preview)
                        } else {
                            viewModel.resetToHome()
                        }
                    }
                )
            }
        }
    }
}
