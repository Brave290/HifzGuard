package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HifzViewModel
import com.example.ui.theme.DarkCard
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusInProgress
import java.util.Locale

@Composable
fun SessionScreen(
    viewModel: HifzViewModel,
    onBackToDashboard: () -> Unit
) {
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val accumulatedSec by viewModel.accumulatedSeconds.collectAsState()
    val dailyGoalMin by viewModel.dailyGoalMinutes.collectAsState()
    val micAmp by viewModel.micAmplitude.collectAsState()
    val isIdleReminderVisible by viewModel.isIdleReminderVisible.collectAsState()
    val lastManualConfirmationSec by viewModel.lastManualConfirmationMinutes.collectAsState()

    val currentSessionMinutes = accumulatedSec / 60
    val currentSessionSeconds = accumulatedSec % 60
    val remainingSeconds = ((dailyGoalMin * 60) - accumulatedSec).coerceAtLeast(0)
    val remainingMin = remainingSeconds / 60
    val remainingSec = remainingSeconds % 60

    // Force periodic trigger to register screen gesture
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(onClick = { viewModel.notifyUserInteraction() })
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ACTIVE RECITATION",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    ),
                    color = GoldAccent
                )

                // Small live status indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isSessionActive) StatusCompleted else Color.Gray)
                    )
                    Text(
                        text = if (isSessionActive) "RECORDING" else "PAUSED",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSessionActive) StatusCompleted else Color.Gray
                    )
                }
            }

            // Big Clock Timer Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Text(
                    text = String.format(Locale.getDefault(), "%02d:%02d", currentSessionMinutes, currentSessionSeconds),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 72.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Accumulated Today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Time remaining representation
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier.border(1.dp, Color(0xFF1D3227), RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "Remaining: ${remainingMin}m ${remainingSec}s",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        ),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }

            // GPU sound amplitude visualizer (pulsing spectrum)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Voice Input Indicator",
                        tint = GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Recitation Voice Waveform",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // Waveform rendering
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    val barsCount = 21
                    val spacing = 8.dp.toPx()
                    val barWidth = (size.width - (barsCount - 1) * spacing) / barsCount
                    val midY = size.height / 2

                    for (i in 0 until barsCount) {
                        // Offset sinus factor representing wave propagation
                        val waveOffset = (Math.sin((i.toDouble() / barsCount.toDouble()) * Math.PI) * 0.5 + 0.5).toFloat()
                        val noise = if (isSessionActive) (micAmp * (0.3f + 0.7f * waveOffset)) else 0.05f

                        val barHeight = (size.height * noise * waveOffset).coerceAtLeast(4.dp.toPx())
                        val x = i * (barWidth + spacing)
                        val y = midY - (barHeight / 2)

                        drawRoundRect(
                            color = if (isSessionActive && micAmp > 0.08f) StatusCompleted else GoldAccent,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                        )
                    }
                }
            }

            // Interactive "I've Read" confirm button (toggles state, resets session tracking)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.tapManualConfirmation() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (lastManualConfirmationSec >= 600) StatusInProgress else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("ive_read_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = "Confirm",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "I'M READING (CONFIRM SESSION)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Text(
                    text = "Required manual confirmation check-in timer.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }

            // Controls buttons (Resume/Pause & Return)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        if (isSessionActive) {
                            viewModel.pauseSession()
                        } else {
                            viewModel.startSession()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSessionActive) Color(0xFFC0392B) else Color(0xFF27AE60)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("pause_resume_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSessionActive) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = "Playback Control"
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSessionActive) "PAUSE" else "RESUME",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        viewModel.pauseSession()
                        onBackToDashboard()
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(
                        text = "FINISH SESSION",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Animated idle reminder overlay
        AnimatedVisibility(
            visible = isIdleReminderVisible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { viewModel.notifyUserInteraction() }, // Any tap resumes!
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, GoldAccent),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .padding(32.dp)
                        .width(320.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Alert",
                            tint = GoldAccent,
                            modifier = Modifier.size(48.dp)
                        )

                        Text(
                            text = "Your recitation is waiting.",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )

                        Text(
                            text = "No interaction or voice amplitude has been detected for 2 minutes. Tap anywhere to resume your goals.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = { viewModel.notifyUserInteraction() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("RESUME SESSION", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
