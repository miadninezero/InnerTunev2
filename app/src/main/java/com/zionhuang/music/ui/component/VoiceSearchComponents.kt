package com.zionhuang.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/**
 * Voice search state models
 */
enum class VoiceSearchState {
    IDLE,
    LISTENING,
    PROCESSING,
    COMPLETED,
    ERROR,
}

data class VoiceSearchResult(
    val transcript: String,
    val confidence: Float, // 0.0-1.0
    val alternatives: List<String> = emptyList(),
)

/**
 * Voice search input component with waveform animation
 */
@Composable
fun VoiceSearchInput(
    onSearchSubmit: (String) -> Unit = {},
    onVoiceSearch: () -> Unit = {},
    voiceState: VoiceSearchState = VoiceSearchState.IDLE,
    voiceTranscript: String = "",
    modifier: Modifier = Modifier,
) {
    var searchText by remember { mutableStateOf(TextFieldValue(voiceTranscript)) }
    var isSearchActive by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Search bar with voice button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    color = if (voiceState == VoiceSearchState.LISTENING)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
                .border(
                    width = if (voiceState == VoiceSearchState.LISTENING) 2.dp else 1.dp,
                    color = if (voiceState == VoiceSearchState.LISTENING)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(20.dp)
                    .padding(start = 8.dp)
            )

            TextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier
                    .weight(1f)
                    .background(Color.Transparent),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp
                ),
                placeholder = {
                    Text(
                        text = if (voiceState == VoiceSearchState.LISTENING)
                            "Listening..." else "Search songs, artists...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true,
                maxLines = 1,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { onSearchSubmit(searchText.text) })
            )

            // Clear button
            if (searchText.text.isNotEmpty()) {
                IconButton(
                    onClick = { searchText = TextFieldValue() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Voice search button
            VoiceSearchButton(
                state = voiceState,
                onClick = onVoiceSearch
            )
        }

        // Waveform animation (shows when listening)
        if (voiceState == VoiceSearchState.LISTENING) {
            VoiceWaveform(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            )
        }

        // Confidence indicator
        if (voiceState == VoiceSearchState.PROCESSING || voiceState == VoiceSearchState.COMPLETED) {
            Text(
                text = when (voiceState) {
                    VoiceSearchState.PROCESSING -> "Processing speech..."
                    VoiceSearchState.COMPLETED -> "Heard: \"$voiceTranscript\""
                    else -> ""
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        // Error message
        if (voiceState == VoiceSearchState.ERROR) {
            Text(
                text = "Could not process voice input",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Voice search button with microphone icon and pulsing effect
 */
@Composable
private fun VoiceSearchButton(
    state: VoiceSearchState,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isActive = state == VoiceSearchState.LISTENING

    val backgroundColor by animateColorAsState(
        targetValue = if (isActive)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
        label = "voice_bg_color"
    )

    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1f,
        label = "voice_scale"
    )

    val pulseScale by animateFloatAsState(
        targetValue = if (isActive) 1.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                1f at 0
                1.3f at 500
                1f at 1000
            }
        ),
        label = "voice_pulse"
    )

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Pulse effect (behind icon)
        if (isActive) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                    .align(Alignment.Center),
            )
        }

        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = "Voice search",
            tint = if (isActive)
                Color.White
            else
                MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Animated waveform showing audio levels during voice recording
 */
@Composable
private fun VoiceWaveform(
    modifier: Modifier = Modifier,
    barCount: Int = 12,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(barCount) { index ->
                WaveformBar(
                    index = index,
                    totalBars = barCount,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Single waveform bar with animated height
 */
@Composable
private fun WaveformBar(
    index: Int,
    totalBars: Int,
    modifier: Modifier = Modifier,
) {
    val animationDuration = 300 + index * 50
    val height by animateFloatAsState(
        targetValue = sin((index.toFloat() / totalBars) * Math.PI.toFloat()) * 20f + 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = animationDuration)
        ),
        label = "waveform_bar_$index"
    )

    Box(
        modifier = modifier
            .height(height.dp)
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f + (index * 0.02f))
            )
    )
}

/**
 * Voice search result preview with transcript and confidence
 */
@Composable
fun VoiceSearchResultCard(
    result: VoiceSearchResult,
    onResultSelect: (String) -> Unit = {},
    onAlternativeSelect: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Primary result
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .clickable { onResultSelect(result.transcript) }
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "\"${result.transcript}\"",
                        fontSize = 14.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    // Confidence badge
                    Box(
                        modifier = Modifier
                            .background(
                                color = when {
                                    result.confidence >= 0.8f -> Color(0xFF4CAF50)
                                    result.confidence >= 0.6f -> Color(0xFFFFC107)
                                    else -> Color(0xFFF44336)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${(result.confidence * 100).toInt()}%",
                            fontSize = 10.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Alternative results
            if (result.alternatives.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Did you mean?",
                        fontSize = 12.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.W600,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    result.alternatives.take(3).forEach { alternative ->
                        Text(
                            text = alternative,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAlternativeSelect(alternative) }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Voice search service (for integration)
 * This handles the actual ML Kit integration logic
 */
interface IVoiceSearchService {
    suspend fun startListening(): Result<Unit>
    suspend fun stopListening(): Result<VoiceSearchResult>
    fun hasPermissions(): Boolean
    fun requestPermissions()
}

/**
 * Mock voice search service for demonstration
 */
class MockVoiceSearchService : IVoiceSearchService {
    override suspend fun startListening(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun stopListening(): Result<VoiceSearchResult> {
        return Result.success(
            VoiceSearchResult(
                transcript = "Play Imagine by John Lennon",
                confidence = 0.95f,
                alternatives = listOf(
                    "Play Imagine by John Lenin",
                    "Play I Imagine by John Lennon"
                )
            )
        )
    }

    override fun hasPermissions(): Boolean = true

    override fun requestPermissions() {
        // Permission request logic
    }
}
