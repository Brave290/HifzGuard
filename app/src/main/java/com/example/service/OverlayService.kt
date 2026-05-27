package com.example.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.data.preferences.PreferencesHelper
import com.example.data.preferences.SessionDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.*

class OverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val TAG = "OverlayService"
    private val NOTIFICATION_ID = 2079
    private val CHANNEL_ID = "hifzguard_overlay_service_channel"

    // Lifecycle setups to make ComposeView happy in a Service
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private lateinit var prefsHelper: PreferencesHelper
    private lateinit var sessionDataStore: SessionDataStore
    private var overlayView: ComposeView? = null
    private var isOverlayAttached = false

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefsHelper = PreferencesHelper(this)
        sessionDataStore = SessionDataStore(this)

        createNotificationChannel()
        startForegroundServiceCompact()

        // Monitor state periodically to adjust the overlay
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        // Check triggers on start
        serviceScope.launch {
            evaluateOverlayState()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "HifzGuard Lock Active",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors current reading session and enforces lock screen"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceCompact() {
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, pendingIntentFlags)

        val notification = NotificationCompatBuilder(this, CHANNEL_ID)
            .setContentTitle("HifzGuard Is Active")
            .setContentText("Keeping your focus protected for Quran goals.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setPriority(Notification.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= 34) { // Build.VERSION_CODES.UPSIDE_DOWN_CAKE is 34
            try {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Failed startForeground with type SPECIAL_USE, falling back", e)
                try {
                    startForeground(NOTIFICATION_ID, notification)
                } catch (t: Throwable) {
                    Log.e(TAG, "Failed all startForeground attempts", t)
                }
            }
        } else {
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed startForeground on pre-34 platform", e)
            }
        }
    }

    private fun startMonitoring() {
        serviceScope.launch {
            // Hot combine of factors: accumulated seconds, thresholds, goal settings, debt
            combine(
                sessionDataStore.accumulatedSecondsFlow,
                sessionDataStore.yesterdayDebtMinutesFlow
            ) { sec, debt ->
                Pair(sec, debt)
            }.collect { (accumulatedSeconds, debtMinutes) ->
                evaluateOverlayState(accumulatedSeconds, debtMinutes)
            }
        }

        // Periodic clock evaluator (runs every 10 seconds to verify lock hours)
        serviceScope.launch {
            while (isActive) {
                evaluateOverlayState()
                delay(10000)
            }
        }
    }

    private suspend fun evaluateOverlayState(
        accumSec: Int? = null,
        debtMin: Int? = null
    ) {
        val seconds = accumSec ?: withContext(Dispatchers.IO) {
            sessionDataStore.accumulatedSecondsFlow.first()
        }
        val debt = debtMin ?: withContext(Dispatchers.IO) {
            sessionDataStore.yesterdayDebtMinutesFlow.first()
        }

        val goalMinutes = prefsHelper.dailyGoalMinutes
        val currentMinutes = seconds / 60
        val remainingGoalMinutes = (goalMinutes - currentMinutes).coerceAtLeast(0)

        val isGoalMet = currentMinutes >= goalMinutes

        // Check if current time is past lock threshold
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val thresholdHour = prefsHelper.lockThresholdHour
        val thresholdMin = prefsHelper.lockThresholdMinute

        val isPastThreshold = (hour > thresholdHour) || (hour == thresholdHour && minute >= thresholdMin)

        // Lock overrides (such as emergency 30 mins, or signature mismatch, etc.)
        val isEmergencyActive = prefsHelper.isEmergencyOverrideActive()

        // Core lock trigger condition:
        // 1. Goal not met today AND past 8pm (threshold)
        // OR 2. Goal not met today AND carrying debt from yesterday (debt > 0 makes it all-day lock until today's goal is met!)
        val shouldLock = !isGoalMet && !isEmergencyActive && (isPastThreshold || debt > 0)

        withContext(Dispatchers.Main) {
            if (shouldLock) {
                showLockOverlay(remainingGoalMinutes)
            } else {
                hideLockOverlay()
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showLockOverlay(remainingMinutes: Int) {
        if (isOverlayAttached && overlayView != null) {
            // Already attached, update content dynamically if needed.
            // Under compose, updating state triggers recomposition automatically.
            return
        }

        // Setup layouts
        val layoutParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.TRANSLUCENT
            flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_FULLSCREEN
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.CENTER
        }

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)

            setContent {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xE60A2E1C), // 90% opacity emerald dark
                                    Color(0xF0050E09)  // 94% opacity deep charcoal black
                                )
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Logo Shield + Book Indicator
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(110.dp)
                                .background(Color(0x33D4AF37), CircleShape)
                                .border(1.5.dp, Color(0xFFD4AF37), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "Quran",
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(56.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Text(
                            text = "SPIRITUAL LOCK IN EFFECT",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD4AF37),
                                letterSpacing = 2.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "HifzGuard has protected your phone from distractions until you meet your daily goal.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(36.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.width(280.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Goal Remaining Today",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$remainingMinutes Minutes",
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        // Only visual active controller
                        Button(
                            onClick = {
                                launchMainApp()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0D5E3A),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .width(240.dp)
                                .height(50.dp)
                                .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(24.dp))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Unlock",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "OPEN HIFZGUARD",
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    // Watermark
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = "Made by Akanji Mus'ab",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFD4AF37).copy(alpha = 0.5f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp
                            )
                        )
                    }
                }
            }
        }

        try {
            windowManager.addView(overlayView, layoutParams)
            isOverlayAttached = true
            Log.d(TAG, "Overlay screen attached successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Exception attaching overlay", e)
        }
    }

    private fun hideLockOverlay() {
        if (isOverlayAttached && overlayView != null) {
            try {
                windowManager.removeView(overlayView)
                Log.d(TAG, "Overlay screen removed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Exception removing overlay", e)
            } finally {
                overlayView = null
                isOverlayAttached = false
            }
        }
    }

    private fun launchMainApp() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(launchIntent)
    }

    override fun onDestroy() {
        hideLockOverlay()
        serviceScope.cancel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    // Standard low-dependency custom builder for notification to remain resilient
    private class NotificationCompatBuilder(private val context: Context, private val channelId: String) {
        private var title = ""
        private var text = ""
        private var iconId = 0
        private var intent: PendingIntent? = null
        private var priority = Notification.PRIORITY_LOW

        fun setContentTitle(t: String) = apply { title = t }
        fun setContentText(t: String) = apply { text = t }
        fun setSmallIcon(i: Int) = apply { iconId = i }
        fun setContentIntent(pi: PendingIntent) = apply { intent = pi }
        fun setPriority(p: Int) = apply { priority = p }

        @Suppress("DEPRECATION")
        fun build(): Notification {
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, channelId)
            } else {
                Notification.Builder(context)
            }
            builder.setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(iconId)
                .setAutoCancel(false)
                .setOngoing(true)

            intent?.let { builder.setContentIntent(it) }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                builder.setPriority(priority)
            }
            return builder.build()
        }
    }
}
