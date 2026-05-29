package com.example

import android.app.Application
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.JuzProgressRepository
import com.example.data.preferences.PreferencesHelper
import com.example.data.preferences.SessionDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HifzGuardApplication : Application() {

    lateinit var preferencesHelper: PreferencesHelper
        private set
    lateinit var sessionDataStore: SessionDataStore
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var juzRepository: JuzProgressRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        
        preferencesHelper = PreferencesHelper(this)
        sessionDataStore = SessionDataStore(this)
        database = AppDatabase.getDatabase(this)
        juzRepository = JuzProgressRepository(database.juzProgressDao())

        // Ensure database preloads all 30 Juz slots & performs daily resets
        applicationScope.launch {
            try {
                juzRepository.initializeIfEmpty()
                sessionDataStore.checkDailyReset()
            } catch (e: Exception) {
                Log.e("HifzGuardApplication", "Error preloading or running daily reset", e)
            }
        }
    }
}
