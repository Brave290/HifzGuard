package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.theme.StatusNotDone

@Composable
fun DashboardScreen(
    viewModel: HifzViewModel,
    onStartSessionClick: () -> Unit,
    onNavigateToJuzGrid: () -> Unit
) {
    val streak by viewModel.streakCount.collectAsState()
    val accumulatedSec by viewModel.accumulatedSeconds.collectAsState()
    val goalMinutes by viewModel.dailyGoalMinutes.collectAsState()
    val juzList by viewModel.juzProgressList.collectAsState()

    val currentMinutes = accumulatedSec / 60
    val progressFraction = if (goalMinutes > 0) currentMinutes.toFloat() / goalMinutes.toFloat() else 0f
    val isGoalCompleted = currentMinutes >= goalMinutes

    // Animated glow effect for button
    val infiniteTransition = rememberInfiniteTransition(label = "StartGlow")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "StartScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 40.dp), // Leaves room for watermark
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Card with Streak
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .border(1.dp, Color(0xFF1D3227), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Assalamu Alaikum",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Consistent Devotion",
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldAccent.copy(alpha = 0.9f)
                    )
                }

                // Interactive fire streak widget
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x22D4AF37))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = "Streak",
                        tint = if (streak > 0) Color(0xFFE67E22) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "$streak Days",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (streak > 0) Color.White else Color.Gray
                        )
                    )
                }
            }

            // Circular goal tracker card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(200.dp)
                ) {
                    // Outer background circle
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = Color.White.copy(alpha = 0.08f),
                        strokeWidth = 14.dp
                    )

                    // Daily progress progress bar
                    CircularProgressIndicator(
                        progress = { progressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxSize(),
                        color = if (isGoalCompleted) StatusCompleted else GoldAccent,
                        strokeWidth = 14.dp
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (isGoalCompleted) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = if (isGoalCompleted) StatusCompleted else GoldAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$currentMinutes / $goalMinutes",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Minutes Read Today",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Start Session Button
            Button(
                onClick = onStartSessionClick,
                modifier = Modifier
                    .scale(scaleAnim)
                    .width(260.dp)
                    .height(56.dp)
                    .testTag("start_session_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "START SESSION",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    )
                }
            }

            // Juz Progress mini-preview (First 5 Juz)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkCard)
                    .clickable { onNavigateToJuzGrid() }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Juz' Progress Grid Overview",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "View All →",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldAccent
                    )
                }

                // Show first 5 Juz cubes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 1..5) {
                        val juzItem = juzList.find { it.juzNumber == i }
                        val color = when (juzItem?.status) {
                            1 -> StatusInProgress
                            2 -> StatusCompleted
                            else -> StatusNotDone
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(color.copy(alpha = 0.2f))
                                .border(1.dp, color, RoundedCornerShape(8.dp))
                        ) {
                            Text(
                                text = "J$i",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = color
                            )
                        }
                    }
                }
            }
        }

        // NON-NEGOTIABLE WATERMARK (Bottom Center, 50% opacity, gold color)
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "Made by Akanji Mus'ab",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = GoldAccent.copy(alpha = 0.50f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp
                ),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}
