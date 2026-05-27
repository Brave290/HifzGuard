package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JuzProgressDao {
    @Query("SELECT * FROM juz_progress ORDER BY juzNumber ASC")
    fun getAllProgress(): Flow<List<JuzProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: JuzProgress)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(progressList: List<JuzProgress>)

    @Query("SELECT COUNT(*) FROM juz_progress WHERE status = 2")
    fun getMemorizedCountFlow(): Flow<Int>

    @Query("UPDATE juz_progress SET status = 0")
    suspend fun resetAll()
}
