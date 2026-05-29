package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HifzViewModel
import com.example.ui.theme.DarkCard
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.StatusCompleted
import com.example.data.local.QuranData

data class Surah(
    val number: Int,
    val nameEnglish: String,
    val nameArabic: String,
    val translation: String,
    val versesCount: Int,
    val type: String, // Meccan or Medinan
    val verses: List<Verse>
)

data class Verse(
    val number: Int,
    val arabic: String,
    val translation: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadQuranScreen(viewModel: HifzViewModel) {
    val isSessionActive by viewModel.isSessionActive.collectAsState()
    val accumulatedSec by viewModel.accumulatedSeconds.collectAsState()
    val committedTargetMinutes by viewModel.committedTargetMinutes.collectAsState()
    val sessionActiveMinutes = accumulatedSec / 60
    val sessionActiveSeconds = accumulatedSec % 60

    var selectedSurah by remember { mutableStateOf<Surah?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    var showGotoDialog by remember { mutableStateOf(false) }
    var gotoSurahNumberInput by remember { mutableStateOf("") }
    var gotoVerseNumberInput by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    var initialScrollIndex by remember { mutableStateOf(0) }

    val context = androidx.compose.ui.platform.LocalContext.current
    // Load full metadata + verses of all 114 Surahs dynamically from the safe central catalog module
    val surahsList = remember(context) { QuranData.getSurahsList(context) }

    LaunchedEffect(selectedSurah, initialScrollIndex) {
        if (selectedSurah != null && initialScrollIndex > 0) {
            // Verse indices are 1-based, index 0 is first Bismillah/spacer header
            listState.scrollToItem(initialScrollIndex)
            initialScrollIndex = 0
        }
    }

    val filteredSurahs = surahsList.filter {
        it.nameEnglish.contains(searchQuery, ignoreCase = true) ||
                it.translation.contains(searchQuery, ignoreCase = true)
    }

    if (showGotoDialog) {
        AlertDialog(
            onDismissRequest = { showGotoDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Explore, contentDescription = null, tint = GoldAccent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GOTO Surah / Verse", color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter a Surah and optional Verse number to navigate or scroll directly there.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value = gotoSurahNumberInput,
                        onValueChange = { gotoSurahNumberInput = it },
                        label = { Text("Surah Number (1-114)", color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = gotoVerseNumberInput,
                        onValueChange = { gotoVerseNumberInput = it },
                        label = { Text("Verse Number (Optional)", color = Color.White.copy(alpha = 0.6f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val surahNum = gotoSurahNumberInput.toIntOrNull()
                        if (surahNum != null && surahNum in 1..114) {
                            val foundSurah = surahsList.find { it.number == surahNum }
                            if (foundSurah != null) {
                                selectedSurah = foundSurah
                                val verseNum = gotoVerseNumberInput.toIntOrNull() ?: 1
                                val clampedVerse = verseNum.coerceIn(1, foundSurah.versesCount)
                                initialScrollIndex = clampedVerse
                                showGotoDialog = false
                                gotoSurahNumberInput = ""
                                gotoVerseNumberInput = ""
                            } else {
                                Toast.makeText(context, "Surah not found", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Please enter a valid Surah number (1-114)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.Black)
                ) {
                    Text("NAVIGATE", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGotoDialog = false }) {
                    Text("CANCEL", color = Color.White.copy(alpha = 0.6f))
                }
            },
            containerColor = DarkCard,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF060F0A), // Extremely deep charcoal-emerald
                        Color(0xFF0A1C12), // Dark emerald
                        Color(0xFF050B08)  // Solid elegant black
                    )
                )
            )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (selectedSurah == null) {
                // Surah Catalog Header
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(1.dp, Color(0xFF1D3227), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = "Quran Catalogs",
                                tint = GoldAccent,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "REAL MUSHAF",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                ),
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Read the Glorious Scripture and track recitation dynamically",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Search & GOTO controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Surah...", color = Color.White.copy(alpha = 0.4f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search icon",
                                tint = GoldAccent
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = Color(0xFF1D3227)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                    )

                    Button(
                        onClick = { showGotoDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x22D4AF37)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = "GOTO icon",
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "GOTO",
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // List of Surahs
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSurahs) { surah ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkCard)
                                .border(1.dp, Color(0xFF1D3227), RoundedCornerShape(12.dp))
                                .clickable { selectedSurah = surah }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0x1AD4AF37), RoundedCornerShape(8.dp))
                                        .border(1.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                ) {
                                    Text(
                                        text = "${surah.number}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = GoldAccent
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = surah.nameEnglish,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${surah.translation} • ${surah.versesCount} Verses",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                            Text(
                                text = surah.nameArabic,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Serif
                                ),
                                color = GoldLight,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x1AD4AF37)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Additional chapters can be cataloged. Recite Al-Fatiha, Al-Mulk, or Al-Kahf above to achieve quick spiritual lock-bypass quotas today.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldLight.copy(alpha = 0.7f),
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            } else {
                // Surah Verse-by-Verse Reader View
                Column(modifier = Modifier.weight(1f)) {
                    // Header navigation bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkCard)
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedSurah = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GoldAccent)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = selectedSurah!!.nameEnglish,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Surah ${selectedSurah!!.number} • ${selectedSurah!!.type}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))

                        // Quick Navigate inside reading screen
                        IconButton(onClick = { showGotoDialog = true }) {
                            Icon(Icons.Default.Explore, contentDescription = "Quick Jump", tint = GoldAccent)
                        }

                        Text(
                            text = selectedSurah!!.nameArabic,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = GoldLight,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }

                    // Verses list container with smooth scroll state
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            // Elegant Bismillah Header
                            if (selectedSurah!!.number != 1) {
                                Text(
                                    text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    textAlign = TextAlign.Center,
                                    color = GoldLight,
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                                )
                            }
                        }

                        items(selectedSurah!!.verses) { verse ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkCard)
                                    .border(1.dp, Color(0xFF1D3227), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(Color(0x1AD4AF37), RoundedCornerShape(14.dp))
                                            .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                    ) {
                                        Text(
                                            text = "${verse.number}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = GoldAccent
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val shareText = "📖 *Shared from HifzGuard* \n\n" +
                                                    "\"${verse.arabic}\"\n\n" +
                                                    "Translation:\n" +
                                                    "\"${verse.translation}\"\n\n" +
                                                    "— Surah ${selectedSurah!!.nameEnglish} (${selectedSurah!!.nameArabic}), Verse ${verse.number}"
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Quran Verse"))
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share Verse",
                                            tint = GoldAccent.copy(alpha = 0.7f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = verse.arabic,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        lineHeight = 38.sp,
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = verse.translation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Left,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }

            // Interactive Recitation Tracker Floating Overlay (Bottom persistent overlay panel)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF071F14)),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                border = BorderStroke(1.dp, Color(0xFF113D26)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSessionActive) StatusCompleted else Color.Gray)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Active Mic Monitor",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val targetLabel = if (committedTargetMinutes > 0) "Committed Target: $committedTargetMinutes mins" else "Daily Recitation Session"
                            Text(
                                text = targetLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Elapsed: " + String.format("%02d:%02d", sessionActiveMinutes, sessionActiveSeconds),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (isSessionActive) {
                                viewModel.pauseSession()
                            } else {
                                viewModel.startSession()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSessionActive) Color(0xFFC0392B) else StatusCompleted,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("mushaf_record_toggle_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isSessionActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSessionActive) "PAUSE" else "RECITE",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }
}
