package com.example.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class JuzProgressRepository(private val dao: JuzProgressDao) {
    val allProgress: Flow<List<JuzProgress>> = dao.getAllProgress()
    val memorizedCountFlow: Flow<Int> = dao.getMemorizedCountFlow()

    suspend fun updateProgress(juzProgress: JuzProgress) {
        dao.insertOrUpdate(juzProgress)
    }

    suspend fun resetAll() {
        dao.resetAll()
    }

    suspend fun initializeIfEmpty() {
        val currentProgress = allProgress.first()
        if (currentProgress.size < 30) {
            val list = (1..30).map { juzNum ->
                // Keep the current state if it exists, otherwise assign 0
                val existing = currentProgress.find { it.juzNumber == juzNum }
                existing ?: JuzProgress(juzNumber = juzNum, status = 0)
            }
            dao.insertAll(list)
        }
    }
}
