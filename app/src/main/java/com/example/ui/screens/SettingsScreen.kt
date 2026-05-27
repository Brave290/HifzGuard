package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HifzViewModel
import com.example.ui.theme.DarkCard
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.StatusCompleted
import com.example.util.Constants
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: HifzViewModel
) {
    val context = LocalContext.current

    // Observe ViewModel configurations
    val currentGoalMinutes by viewModel.dailyGoalMinutes.collectAsState()
    val thresholdHour by viewModel.lockThresholdHour.collectAsState()
    val thresholdMinute by viewModel.lockThresholdMinute.collectAsState()
    val phoneNum by viewModel.accountabilityPhone.collectAsState()
    val isConsequenceByMsg by viewModel.consequenceEnabled.collectAsState()
    val customTemplateMessage by viewModel.consequenceMessageTemplate.collectAsState()

    var goalInput by remember { mutableStateOf(currentGoalMinutes.toString()) }
    var hourInput by remember { mutableStateOf(thresholdHour.toString()) }
    var minInput by remember { mutableStateOf(thresholdMinute.toString()) }

    var phoneInput by remember { mutableStateOf(phoneNum) }
    var consequenceEnabledState by remember { mutableStateOf(isConsequenceByMsg) }
    var messageInput by remember { mutableStateOf(customTemplateMessage) }

    LaunchedEffect(currentGoalMinutes) {
        goalInput = currentGoalMinutes.toString()
    }
    LaunchedEffect(thresholdHour) {
        hourInput = thresholdHour.toString()
    }
    LaunchedEffect(thresholdMinute) {
        minInput = thresholdMinute.toString()
    }
    LaunchedEffect(phoneNum) {
        phoneInput = phoneNum
    }
    LaunchedEffect(isConsequenceByMsg) {
        consequenceEnabledState = isConsequenceByMsg
    }
    LaunchedEffect(customTemplateMessage) {
        messageInput = customTemplateMessage
    }

    // Version Easter Egg click counter
    var versionClicks by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Screen Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "SYSTEM SETTINGS",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                ),
                color = GoldAccent
            )
            Text(
                text = "Fine-tune HifzGuard protection options",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        // Section A: Daily Targets
        SectionCard(title = "Daily Target settings") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Timer",
                    tint = GoldAccent
                )
                // Number Input
                OutlinedTextField(
                    value = goalInput,
                    onValueChange = {
                        goalInput = it
                        val intVal = it.toIntOrNull()
                        if (intVal != null && intVal > 0) {
                            viewModel.setDailyGoal(intVal)
                        }
                    },
                    label = { Text("Daily Goal (Minutes)", color = Color.White.copy(alpha = 0.6f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f).testTag("daily_goal_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    )
                )
            }
        }

        // Section B: Lock Threshold Time Pickers (past 8 PM etc.)
        SectionCard(title = "Lock Threshold Time") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                
                // Visual feedback card with AM/PM formatted presentation & Select Time Picker Trigger
                val amPm = if (thresholdHour >= 12) "PM" else "AM"
                val displayHour = when {
                    thresholdHour == 0 -> 12
                    thresholdHour > 12 -> thresholdHour - 12
                    else -> thresholdHour
                }
                val displayMinute = String.format(Locale.getDefault(), "%02d", thresholdMinute)
                val formattedTime = "$displayHour:$displayMinute $amPm"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ACTIVE LOCK TIME",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White.copy(alpha = 0.4f)
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = GoldAccent
                        )
                    }

                    Button(
                        onClick = {
                            val picker = android.app.TimePickerDialog(
                                context,
                                { _, pickedHour, pickedMinute ->
                                    viewModel.setLockThreshold(pickedHour, pickedMinute)
                                    hourInput = pickedHour.toString()
                                    minInput = pickedMinute.toString()
                                },
                                thresholdHour,
                                thresholdMinute,
                                false // 12-hour format
                            )
                            picker.show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent.copy(alpha = 0.15f),
                            contentColor = GoldAccent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f)),
                        modifier = Modifier.testTag("time_picker_trigger")
                    ) {
                        Text("SELECT TIME", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }

                // Quick manual text fields below for precise entries
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Schedule",
                        tint = GoldAccent
                    )

                    // Hour
                    OutlinedTextField(
                        value = hourInput,
                        onValueChange = {
                            hourInput = it
                            val intVal = it.toIntOrNull()
                            if (intVal != null && intVal in 0..23) {
                                viewModel.setLockThreshold(intVal, minInput.toIntOrNull() ?: 0)
                            }
                        },
                        label = { Text("Hour (0-23)", color = Color.White.copy(alpha = 0.6f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("lock_hour_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )

                    // Minute
                    OutlinedTextField(
                        value = minInput,
                        onValueChange = {
                            minInput = it
                            val intVal = it.toIntOrNull()
                            if (intVal != null && intVal in 0..59) {
                                viewModel.setLockThreshold(hourInput.toIntOrNull() ?: 20, intVal)
                            }
                        },
                        label = { Text("Minute (0-59)", color = Color.White.copy(alpha = 0.6f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("lock_minute_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        // Section C: Accountability Setup
        SectionCard(title = "Accountability Partner") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Partner phone number
                OutlinedTextField(
                    value = phoneInput,
                    onValueChange = {
                        phoneInput = it
                        viewModel.setAccountabilityContact(it, consequenceEnabledState, messageInput)
                    },
                    label = { Text("Partner's WhatsApp Num (Include country code)", color = Color.White.copy(alpha = 0.6f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    ),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Phone, contentDescription = "Phone", tint = GoldAccent)
                    }
                )

                // Consequence Mode Enable Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Trigger Auto Consequence",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Switch(
                        checked = consequenceEnabledState,
                        onCheckedChange = {
                            consequenceEnabledState = it
                            viewModel.setAccountabilityContact(phoneInput, it, messageInput)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StatusCompleted,
                            checkedTrackColor = StatusCompleted.copy(alpha = 0.4f)
                        )
                    )
                }

                // Custom Consequence message
                OutlinedTextField(
                    value = messageInput,
                    onValueChange = {
                        messageInput = it
                        viewModel.setAccountabilityContact(phoneInput, consequenceEnabledState, it)
                    },
                    label = { Text("Custom Consequence message template", color = Color.White.copy(alpha = 0.6f)) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldAccent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                    )
                )

                // Helper trigger button to simulate the consequence send right now
                OutlinedButton(
                    onClick = {
                        simulateSendWhatsApp(context, phoneInput, messageInput)
                    },
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "Test WhatsApp Send", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TEST WHATSAPP TRIGGER NOW", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section D: Share HifzGuard Referral
        Button(
            onClick = {
                val shareMsg = Constants.SHARE_MESSAGE_TEMPLATE.replace("{DOWNLOAD_URL}", Constants.APP_DOWNLOAD_URL)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareMsg)
                }
                context.startActivity(Intent.createChooser(intent, "Share HifzGuard using"))
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("share_app_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Share, contentDescription = "Refer App")
                Spacer(modifier = Modifier.width(8.dp))
                Text("SHARE HIFZGUARD (REFERRAL LINK)", fontWeight = FontWeight.Bold)
            }
        }

        // Section E: About Developer
        SectionCard(title = "About Information") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "HifzGuard",
                    tint = GoldAccent,
                    modifier = Modifier.size(36.dp)
                )

                // SIGNATURE / DEVELOPER WATERMARK (Slightly larger, prominent text)
                Text(
                    text = "Developed by ${Constants.DEVELOPER_NAME}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent,
                        fontSize = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "A digital sanctuary that shields your focus from distraction, prioritizing your daily spiritual connection to the Quran.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // App version click Easter Egg (Emergency override)
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier
                        .clickable {
                            versionClicks++
                            if (versionClicks >= 7) {
                                viewModel.triggerEmergencyOverride()
                                Toast.makeText(context, "EMERGENCY OVERRIDE GRANTED! Unified lock bypass active for 30 minutes.", Toast.LENGTH_LONG).show()
                                versionClicks = 0
                            } else {
                                val remaining = 7 - versionClicks
                                Toast.makeText(context, "Tap $remaining more times to unlock Emergency Override", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White.copy(alpha = 0.5f)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(DarkCard)
                .border(1.dp, Color(0xFF1D3227), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            content()
        }
    }
}

private fun simulateSendWhatsApp(context: Context, phone: String, message: String) {
    if (phone.isBlank()) {
        Toast.makeText(context, "Please set a valid WhatsApp phone number first!", Toast.LENGTH_SHORT).show()
        return
    }
    // Deep-link to WhatsApp API
    try {
        val formattedMsg = Uri.encode(message)
        val url = "https://api.whatsapp.com/send?phone=$phone&text=$formattedMsg"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open WhatsApp. Ensure it is installed.", Toast.LENGTH_SHORT).show()
    }
}
