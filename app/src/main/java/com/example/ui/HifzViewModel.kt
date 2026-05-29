package com.example.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.JuzProgress
import com.example.data.local.JuzProgressRepository
import com.example.data.preferences.PreferencesHelper
import com.example.data.preferences.SessionDataStore
import com.example.integrity.RootDetector
import com.example.integrity.SignatureVerifier
import com.example.service.OverlayService
import com.example.util.Constants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class HifzViewModel(
    private val context: Context,
    private val prefsHelper: PreferencesHelper,
    private val sessionDataStore: SessionDataStore,
    private val juzProgressRepository: JuzProgressRepository
) : ViewModel() {

    private val TAG = "HifzViewModel"

    // Integration & Integrity Checks
    private val _isRooted = MutableStateFlow(false)
    val isRooted: StateFlow<Boolean> = _isRooted.asStateFlow()

    private val _isTampered = MutableStateFlow(false)
    val isTampered: StateFlow<Boolean> = _isTampered.asStateFlow()

    // Preferences & settings fields exposing to UI
    val dailyGoalMinutes = MutableStateFlow(prefsHelper.dailyGoalMinutes)
    val committedTargetMinutes = MutableStateFlow(prefsHelper.committedTargetMinutes)
    private val _sessionElapsedSeconds = MutableStateFlow(0)
    val sessionElapsedSeconds: StateFlow<Int> = _sessionElapsedSeconds.asStateFlow()

    val lockThresholdHour = MutableStateFlow(prefsHelper.lockThresholdHour)
    val lockThresholdMinute = MutableStateFlow(prefsHelper.lockThresholdMinute)
    val accountabilityPhone = MutableStateFlow(prefsHelper.accountabilityPhone)
    val consequenceEnabled = MutableStateFlow(prefsHelper.consequenceEnabled)
    val consequenceMessageTemplate = MutableStateFlow(prefsHelper.consequenceMessageTemplate)

    val streakCount = MutableStateFlow(prefsHelper.streakCount)
    val bestStreak = MutableStateFlow(prefsHelper.bestStreak)
    val totalLifetimeMinutes = MutableStateFlow(prefsHelper.totalLifetimeMinutes)

    // DataStore Flow for accumulated seconds today
    val accumulatedSeconds = sessionDataStore.accumulatedSecondsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val yesterdayDebtMinutes = sessionDataStore.yesterdayDebtMinutesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    // Current Active Reading Session variables
    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _micAmplitude = MutableStateFlow(0f)
    val micAmplitude: StateFlow<Float> = _micAmplitude.asStateFlow()

    private val _isIdleReminderVisible = MutableStateFlow(false)
    val isIdleReminderVisible: StateFlow<Boolean> = _isIdleReminderVisible.asStateFlow()

    private var sessionTimerJob: Job? = null
    private var voicePollingJob: Job? = null
    private var lastActivityTimeMillis = System.currentTimeMillis()
    private var isGoalCompletedTriggered = false
    private val _lastManualConfirmationMinutes = MutableStateFlow(0)
    val lastManualConfirmationMinutes: StateFlow<Int> = _lastManualConfirmationMinutes.asStateFlow()

    // Database Juz query
    val juzProgressList = juzProgressRepository.allProgress.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val memorizedJuzCount = juzProgressRepository.memorizedCountFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private val prefChangeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "daily_goal_minutes" || key == "committed_target_minutes" || key == "lock_threshold_hour" ||
            key == "lock_threshold_minute" || key == "accountability_phone_number" || key == "consequence_enabled" ||
            key == "consequence_message_template" || key == "streak_count" || key == "best_streak" ||
            key == "total_lifetime_minutes" || key == "commitment_end_time_millis") {
            
            dailyGoalMinutes.value = prefsHelper.dailyGoalMinutes
            committedTargetMinutes.value = prefsHelper.committedTargetMinutes
            lockThresholdHour.value = prefsHelper.lockThresholdHour
            lockThresholdMinute.value = prefsHelper.lockThresholdMinute
            accountabilityPhone.value = prefsHelper.accountabilityPhone
            consequenceEnabled.value = prefsHelper.consequenceEnabled
            consequenceMessageTemplate.value = prefsHelper.consequenceMessageTemplate
            streakCount.value = prefsHelper.streakCount
            bestStreak.value = prefsHelper.bestStreak
            totalLifetimeMinutes.value = prefsHelper.totalLifetimeMinutes
        }
    }

    init {
        performIntegrityChecks()
        monitorGoalCompletionToUpdateStreak()
        prefsHelper.registerListener(prefChangeListener)
    }

    private fun performIntegrityChecks() {
        _isRooted.value = RootDetector.isDeviceRooted()
        // verify package signature
        _isTampered.value = !SignatureVerifier.verifySignature(context)
    }

    private fun monitorGoalCompletionToUpdateStreak() {
        viewModelScope.launch {
            accumulatedSeconds.collect { sec ->
                val currentMinutes = sec / 60
                val goalMin = dailyGoalMinutes.value
                if (currentMinutes >= goalMin) {
                    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val lastSaved = prefsHelper.lastStreakDate
                    
                    if (lastSaved != today) {
                        // Increments streak
                        val newStreak = prefsHelper.streakCount + 1
                        prefsHelper.streakCount = newStreak
                        prefsHelper.lastStreakDate = today
                        streakCount.value = newStreak

                        if (newStreak > prefsHelper.bestStreak) {
                            prefsHelper.bestStreak = newStreak
                            bestStreak.value = newStreak
                        }

                        // Also update lifetime minutes
                        prefsHelper.totalLifetimeMinutes = prefsHelper.totalLifetimeMinutes + currentMinutes
                        totalLifetimeMinutes.value = prefsHelper.totalLifetimeMinutes

                        Log.d(TAG, "Goal Complete today! Streak incremented to: $newStreak")
                    }
                }
            }
        }
    }

    // 1. Daily Goal Settings Mutators
    fun setDailyGoal(minutes: Int) {
        prefsHelper.dailyGoalMinutes = minutes
        dailyGoalMinutes.value = minutes
    }

    fun setLockThreshold(hour: Int, minute: Int) {
        prefsHelper.lockThresholdHour = hour
        prefsHelper.lockThresholdMinute = minute
        lockThresholdHour.value = hour
        lockThresholdMinute.value = minute
    }

    fun setAccountabilityContact(phone: String, enabled: Boolean, message: String) {
        prefsHelper.accountabilityPhone = phone
        prefsHelper.consequenceEnabled = enabled
        prefsHelper.consequenceMessageTemplate = message
        accountabilityPhone.value = phone
        consequenceEnabled.value = enabled
        consequenceMessageTemplate.value = message
    }

    // 2. Juz Grid Mutators
    fun cycleJuzStatus(juzNumber: Int) {
        viewModelScope.launch {
            val currentList = juzProgressList.value
            val existing = currentList.find { it.juzNumber == juzNumber }
            val nextStatus = when (existing?.status) {
                0 -> 1 // Gray -> Orange
                1 -> 2 // Orange -> Green
                else -> 0 // Green -> Gray
            }
            juzProgressRepository.updateProgress(JuzProgress(juzNumber, nextStatus))
        }
    }

    fun setJuzStatus(juzNumber: Int, status: Int) {
        viewModelScope.launch {
            juzProgressRepository.updateProgress(JuzProgress(juzNumber, status))
        }
    }

    fun resetAllJuz() {
        viewModelScope.launch {
            juzProgressRepository.resetAll()
        }
    }

    fun resetAllStatistics() {
        viewModelScope.launch {
            // 1. Reset memorization database progress
            juzProgressRepository.resetAll()
            // 2. Clear Datastore values (accumulated seconds to 0, yesterday's debt to 0)
            sessionDataStore.setAccumulatedSeconds(0)
            sessionDataStore.setYesterdayDebtMinutes(0)
            // 3. Clear SharedPreferences lifetime statistics
            prefsHelper.totalLifetimeMinutes = 0
            prefsHelper.streakCount = 0
            prefsHelper.bestStreak = 0
            // 4. Update the live flows so the UI reflects the reset immediately
            totalLifetimeMinutes.value = 0
            streakCount.value = 0
            bestStreak.value = 0
            _sessionElapsedSeconds.value = 0
        }
    }

    // 3. Active Session Controls
    fun setCommittedTargetMinutes(minutes: Int) {
        prefsHelper.committedTargetMinutes = minutes
        committedTargetMinutes.value = minutes
        if (minutes > 0) {
            prefsHelper.dailyGoalMinutes = minutes
            dailyGoalMinutes.value = minutes
            prefsHelper.commitmentEndTimeMillis = System.currentTimeMillis() + (minutes * 60 * 1000)
        } else {
            prefsHelper.commitmentEndTimeMillis = 0
        }
    }

    private fun playAlarmSound() {
        try {
            // Updated to ensure reliable notification as requested
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = "hifzguard_alarm_channel"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Session Alarm",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }
            
            val builder = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("Session Complete!")
                .setContentText("Your recitation goal is met. Taking you out of focus mode.")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
            
            notificationManager.notify(3000, builder.build())
            
            val notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE)
            val ringtone = android.media.RingtoneManager.getRingtone(context, notificationUri)
            ringtone?.play()
            viewModelScope.launch {
                delay(5000)
                if (ringtone?.isPlaying == true) {
                    ringtone.stop()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm sound", e)
        }
    }

    fun startSession() {
        if (_isSessionActive.value) return
        _isSessionActive.value = true
        _isIdleReminderVisible.value = false
        _sessionElapsedSeconds.value = 0 // Reset elapsed count for the current session
        lastActivityTimeMillis = System.currentTimeMillis()
        _lastManualConfirmationMinutes.value = 0
        isGoalCompletedTriggered = false

        // Start counting minutes/seconds
        startSessionTimer()

        // Poll voice volume levels
        startVoicePolling()
    }

    fun pauseSession() {
        _isSessionActive.value = false
        sessionTimerJob?.cancel()
        voicePollingJob?.cancel()
    }

    fun notifyUserInteraction() {
        // Reset the idle timeout whenever any gesture touches the screen
        lastActivityTimeMillis = System.currentTimeMillis()
        if (_isIdleReminderVisible.value) {
            _isIdleReminderVisible.value = false
            // auto-resume if it was paused due to idle
            startSession()
        }
    }

    fun tapManualConfirmation() {
        // user confirms active reading manually
        _lastManualConfirmationMinutes.value = 0
        notifyUserInteraction()
    }

    private fun startSessionTimer() {
        sessionTimerJob?.cancel()
        sessionTimerJob = viewModelScope.launch {
            var lastTick = System.currentTimeMillis()
            while (_isSessionActive.value) {
                delay(1000)
                val now = System.currentTimeMillis()
                val deltaSeconds = ((now - lastTick) / 1000).toInt()
                if (deltaSeconds > 0) {
                    sessionDataStore.incrementAccumulatedSeconds(deltaSeconds)
                    _sessionElapsedSeconds.value += deltaSeconds
                    lastTick = now
                }

                val elapsedSec = _sessionElapsedSeconds.value
                val minElapsed = elapsedSec / 60
                val totalAccumulatedMin = (accumulatedSeconds.value + deltaSeconds) / 60

                // Check committed target
                val targetMinutes = committedTargetMinutes.value
                val dailyGoalMin = dailyGoalMinutes.value
                val isCommitmentTimeUp = prefsHelper.commitmentEndTimeMillis > 0 && now >= prefsHelper.commitmentEndTimeMillis
                
                val isGoalCompleted = (targetMinutes > 0 && (elapsedSec >= targetMinutes * 60 || isCommitmentTimeUp)) ||
                                     (targetMinutes <= 0 && dailyGoalMin > 0 && totalAccumulatedMin >= dailyGoalMin)

                if (isGoalCompleted && !isGoalCompletedTriggered) {
                    isGoalCompletedTriggered = true
                    
                    // 1. Play the alarm sound!
                    playAlarmSound()
                    
                    // 2. No automatic bypass
                    prefsHelper.temporaryUnlockUntil = 0L
                    
                    // 3. Reset target if active
                    if (targetMinutes > 0) {
                        prefsHelper.committedTargetMinutes = 0
                        committedTargetMinutes.value = 0
                        prefsHelper.commitmentEndTimeMillis = 0
                    }
                    
                    // 4. Send broadcast to close app
                    context.sendBroadcast(Intent(OverlayService.ACTION_CLOSE_APP))
                    
                    Log.i(TAG, "Goal completion reached! Playing alarm.")
                }

                // Track the manual reminder limit (user must tap interactive confirm button every 10 mins)
                _lastManualConfirmationMinutes.value = (_lastManualConfirmationMinutes.value + 1).coerceAtMost(60)

                // Evaluate Idle checks (no activity of either mic volume or touch for 2 minutes)
                val totalIdle = System.currentTimeMillis() - lastActivityTimeMillis
                if (totalIdle >= 120000L) { // 2 minutes (120,000ms)
                    pauseSession()
                    _isIdleReminderVisible.value = true
                    Log.d(TAG, "Session suspended due to inactivity timeout (2 minutes)")
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startVoicePolling() {
        voicePollingJob?.cancel()
        voicePollingJob = viewModelScope.launch(Dispatchers.IO) {
            var audioRecord: AudioRecord? = null
            try {
                val bufferSize = AudioRecord.getMinBufferSize(
                    8000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (bufferSize > 0) {
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        8000,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize
                    )
                    
                    if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
                        val buffer = ShortArray(bufferSize)
                        audioRecord.startRecording()
                        
                        while (_isSessionActive.value) {
                            val readSize = audioRecord.read(buffer, 0, buffer.size)
                            if (readSize > 0) {
                                var maxAmplitude = 0
                                for (i in 0 until readSize) {
                                    val absVal = Math.abs(buffer[i].toInt())
                                    if (absVal > maxAmplitude) {
                                        maxAmplitude = absVal
                                    }
                                }
                                
                                val amplitudeVal = (maxAmplitude.toFloat() / 32767f).coerceIn(0f, 1f)
                                _micAmplitude.value = amplitudeVal

                                // Soft voice amplitude (above 8%) qualifies as activity event!
                                if (amplitudeVal > 0.08f) {
                                    lastActivityTimeMillis = System.currentTimeMillis()
                                }
                            }
                            delay(100)
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "AudioRecord polling exception, falls back purely to gestures", e)
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (t: Throwable) { /* ignore */ }
            }
        }
    }

    fun hideLockOverlay() {
        val intent = Intent(context, OverlayService::class.java).apply {
            action = com.example.service.OverlayService.ACTION_FORCE_UNLOCK
        }
        context.startService(intent)
    }

    fun triggerEmergencyOverride() {
        prefsHelper.triggerEmergencyOverride()
    }

    override fun onCleared() {
        super.onCleared()
        try {
            prefsHelper.unregisterListener(prefChangeListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister preference listener", e)
        }
    }
}

class HifzViewModelFactory(
    private val context: Context,
    private val prefsHelper: PreferencesHelper,
    private val sessionDataStore: SessionDataStore,
    private val juzProgressRepository: JuzProgressRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HifzViewModel::class.java)) {
            return HifzViewModel(context, prefsHelper, sessionDataStore, juzProgressRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
