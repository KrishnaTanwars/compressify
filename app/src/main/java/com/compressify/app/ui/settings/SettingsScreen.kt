package com.compressify.app.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.compressify.app.R
import com.compressify.app.data.model.CompressionPreset
import com.compressify.app.data.model.ThemeMode
import com.compressify.app.ui.components.AppTopBar
import com.compressify.app.ui.components.PrivacyDialog

@Composable
fun SettingsScreen(
    themeMode: ThemeMode,
    defaultPreset: CompressionPreset,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDefaultPresetChange: (CompressionPreset) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showPresetMenu by remember { mutableStateOf(false) }

    if (showPrivacyDialog) {
        PrivacyDialog(onDismissRequest = { showPrivacyDialog = false })
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Settings",
                onBackClick = onBackClick
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Preferences Section
            SettingsSectionHeader(title = "Compression & Appearance")

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Theme Setting
                    SettingsRow(
                        icon = Icons.Outlined.DarkMode,
                        title = "App Theme",
                        subtitle = themeMode.title,
                        onClick = { showThemeMenu = true }
                    ) {
                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false }
                        ) {
                            ThemeMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.title) },
                                    onClick = {
                                        onThemeModeChange(mode)
                                        showThemeMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Default Preset Setting
                    SettingsRow(
                        icon = Icons.Outlined.Tune,
                        title = "Default Compression Level",
                        subtitle = defaultPreset.title,
                        onClick = { showPresetMenu = true }
                    ) {
                        DropdownMenu(
                            expanded = showPresetMenu,
                            onDismissRequest = { showPresetMenu = false }
                        ) {
                            listOf(
                                CompressionPreset.BALANCED,
                                CompressionPreset.HIGH_QUALITY,
                                CompressionPreset.SMALL_SIZE,
                                CompressionPreset.TARGET_SIZE
                            ).forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.title) },
                                    onClick = {
                                        onDefaultPresetChange(preset)
                                        showPresetMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Save Location info
                    SettingsRow(
                        icon = Icons.Outlined.Folder,
                        title = "Save Location",
                        subtitle = "Pictures/Compressify (Scoped Storage)",
                        onClick = {}
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // About & Privacy Section
            SettingsSectionHeader(title = "About & Privacy")

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    // Privacy Policy
                    SettingsRow(
                        icon = Icons.Outlined.Lock,
                        title = "Privacy & Security Guarantee",
                        subtitle = "100% on-device local compression, zero server uploads",
                        onClick = { showPrivacyDialog = true }
                    )

                    // Rate App
                    SettingsRow(
                        icon = Icons.Outlined.Star,
                        title = "Rate Compressify",
                        subtitle = "Support independent development on Google Play",
                        onClick = {
                            val uri = Uri.parse("market://details?id=${context.packageName}")
                            val playIntent = Intent(Intent.ACTION_VIEW, uri)
                            if (playIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(playIntent)
                            }
                        }
                    )

                    // Share App
                    SettingsRow(
                        icon = Icons.Outlined.Share,
                        title = "Share Compressify",
                        subtitle = "Recommend Compressify to friends and colleagues",
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Check out Compressify: Fast, private, offline image compressor for Android! https://play.google.com/store/apps/details?id=${context.packageName}"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Compressify"))
                        }
                    )

                    // App Version
                    SettingsRow(
                        icon = Icons.Outlined.Info,
                        title = "Version",
                        subtitle = "1.0.0 (Play Store Production Release)",
                        onClick = {}
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Privacy Commitment Callout
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.privacy_statement),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (trailingContent != null) {
            trailingContent()
        } else {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
