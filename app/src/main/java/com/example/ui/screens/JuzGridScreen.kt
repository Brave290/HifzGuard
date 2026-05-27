package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JuzGridScreen(
    viewModel: HifzViewModel
) {
    val progressList by viewModel.juzProgressList.collectAsState()
    val memorizedCount by viewModel.memorizedJuzCount.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }
    val percentage = (memorizedCount.toFloat() / 30f) * 100f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "JUZ' PROGRESS GRID",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = GoldAccent
                )
                Text(
                    text = "Interactive memorization status matrix",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }

            IconButton(
                onClick = { showResetDialog = true },
                modifier = Modifier.testTag("reset_all_juz_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset All Juz Progress",
                    tint = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        // Percentage Card
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1D3227), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Completed",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$memorizedCount of 30 Juz'",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(60.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { percentage / 100f },
                        color = StatusCompleted,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        strokeWidth = 6.dp,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = String.format(Locale.getDefault(), "%.0f%%", percentage),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                }
            }
        }

        // Legends Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = StatusNotDone, text = "Not Memorized")
            LegendItem(color = StatusInProgress, text = "In Progress")
            LegendItem(color = StatusCompleted, text = "Memorized")
        }

        // 6x5 Grid Space
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkCard)
                .border(1.dp, Color(0xFF1D3227), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(30) { index ->
                    val juzNum = index + 1
                    val item = progressList.find { it.juzNumber == juzNum }
                    val currentStatus = item?.status ?: 0

                    val color = when (currentStatus) {
                        1 -> StatusInProgress
                        2 -> StatusCompleted
                        else -> StatusNotDone
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color.copy(alpha = 0.15f))
                            .border(1.5.dp, color, RoundedCornerShape(10.dp))
                            .combinedClickable(
                                onClick = { viewModel.cycleJuzStatus(juzNum) },
                                onLongClick = {
                                    // Long press resets to unmemorized (Status 0)
                                    viewModel.cycleJuzStatus(juzNum) // Cycle to next or let's reset status to 0
                                    // Let's cycle directly back to 0
                                    if (currentStatus != 0) {
                                        viewModel.cycleJuzStatus(juzNum)
                                        if (currentStatus == 1) {
                                            viewModel.cycleJuzStatus(juzNum) // cycle twice to loop through status transitions
                                        }
                                    }
                                }
                            )
                    ) {
                        Text(
                            text = juzNum.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        )
                    }
                }
            }
        }

        Text(
            text = "Tip: Tap a Juz box to cycle through statuses. Long-press to reset.",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Reset All Confirmation Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset All Progress?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to clear your entire memorization progress matrix? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetAllJuz()
                        showResetDialog = false
                    }
                ) {
                    Text("RESET ALL", color = Color(0xFFC0392B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("CANCEL")
                }
            },
            containerColor = DarkCard
        )
    }
}

@Composable
fun LegendItem(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
