package com.compressify.app.ui.preview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.compressify.app.data.model.CompressionConfig
import com.compressify.app.data.model.CompressionPreset
import com.compressify.app.data.model.ImageMetadata
import com.compressify.app.ui.components.AppTopBar
import com.compressify.app.ui.components.PresetCard
import com.compressify.app.ui.components.QualitySlider
import com.compressify.app.utils.Formatters
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreviewScreen(
    metadata: ImageMetadata,
    config: CompressionConfig,
    isCompressing: Boolean,
    errorMessage: String?,
    onPresetSelected: (CompressionPreset) -> Unit,
    onCustomQualityChanged: (Int) -> Unit,
    onTargetSizeChanged: (Long?) -> Unit,
    onCompressClick: () -> Unit,
    onBackClick: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var showCustomSlider by remember { mutableStateOf(config.preset == CompressionPreset.CUSTOM) }
    var customTargetKbText by remember { mutableStateOf("500") }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            onClearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Compression Options",
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = onCompressClick,
                        enabled = !isCompressing,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                    ) {
                        if (isCompressing) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Compressing…",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Compress,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Compress Image",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawVerticalScrollbar(
                    scrollState = scrollState,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Original Image Preview Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 10f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(metadata.uri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Original photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Metadata Metrics Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricInfoChip(
                            label = "Original Size",
                            value = Formatters.formatFileSize(metadata.byteSize),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        MetricInfoChip(
                            label = "Resolution",
                            value = metadata.resolutionString,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Presets Header
            Text(
                text = "Choose Compression Level",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Presets Cards
            PresetCard(
                preset = CompressionPreset.BALANCED,
                isSelected = config.preset == CompressionPreset.BALANCED,
                onSelect = {
                    showCustomSlider = false
                    onPresetSelected(CompressionPreset.BALANCED)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            PresetCard(
                preset = CompressionPreset.HIGH_QUALITY,
                isSelected = config.preset == CompressionPreset.HIGH_QUALITY,
                onSelect = {
                    showCustomSlider = false
                    onPresetSelected(CompressionPreset.HIGH_QUALITY)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            PresetCard(
                preset = CompressionPreset.SMALL_SIZE,
                isSelected = config.preset == CompressionPreset.SMALL_SIZE,
                onSelect = {
                    showCustomSlider = false
                    onPresetSelected(CompressionPreset.SMALL_SIZE)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Target File Size Option Card
            PresetCard(
                preset = CompressionPreset.TARGET_SIZE,
                isSelected = config.preset == CompressionPreset.TARGET_SIZE,
                onSelect = {
                    showCustomSlider = false
                    val kb = customTargetKbText.toLongOrNull() ?: 500L
                    onTargetSizeChanged(kb * 1024L)
                    coroutineScope.launch {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                }
            )

            AnimatedVisibility(
                visible = config.preset == CompressionPreset.TARGET_SIZE,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Target Size compresses your image so it fits below your requested file size limit.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Quick Target Options:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val targetChips = listOf(
                            Pair("50 KB", 50L),
                            Pair("100 KB", 100L),
                            Pair("250 KB", 250L),
                            Pair("500 KB", 500L),
                            Pair("1 MB", 1024L)
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            targetChips.forEach { (label, kb) ->
                                val targetBytes = kb * 1024L
                                val isChipSelected = config.targetSizeBytes == targetBytes
                                FilterChip(
                                    selected = isChipSelected,
                                    onClick = {
                                        customTargetKbText = kb.toString()
                                        onTargetSizeChanged(targetBytes)
                                    },
                                    label = { Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = customTargetKbText,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }
                                customTargetKbText = filtered
                                val kb = filtered.toLongOrNull()
                                if (kb != null && kb > 0) {
                                    onTargetSizeChanged(kb * 1024L)
                                }
                            },
                            label = { Text("Or Enter Target Size (in KB)") },
                            suffix = { Text("KB", fontWeight = FontWeight.Bold) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Advanced custom slider toggle
            OutlinedButton(
                onClick = {
                    showCustomSlider = !showCustomSlider
                    if (showCustomSlider) {
                        onPresetSelected(CompressionPreset.CUSTOM)
                        coroutineScope.launch {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                    } else {
                        onPresetSelected(CompressionPreset.BALANCED)
                    }
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (showCustomSlider) "Hide Custom Quality" else "Advanced: Custom Quality Slider",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            AnimatedVisibility(
                visible = showCustomSlider,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    QualitySlider(
                        quality = config.quality,
                        onQualityChange = onCustomQualityChanged
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

fun Modifier.drawVerticalScrollbar(
    scrollState: ScrollState,
    color: Color,
    width: Dp = 5.dp
): Modifier = this.drawWithContent {
    drawContent()
    val elementHeight = size.height
    val scrollMax = scrollState.maxValue.toFloat()
    if (scrollMax > 0) {
        val scrollbarHeight = (elementHeight * (elementHeight / (scrollMax + elementHeight))).coerceAtLeast(36.dp.toPx())
        val scrollbarOffsetY = (scrollState.value.toFloat() / scrollMax) * (elementHeight - scrollbarHeight)
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width - width.toPx() - 2.dp.toPx(), scrollbarOffsetY),
            size = Size(width.toPx(), scrollbarHeight),
            cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
        )
    }
}

@Composable
private fun MetricInfoChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
