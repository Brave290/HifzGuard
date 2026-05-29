package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "juz_progress")
data class JuzProgress(
    @PrimaryKey val juzNumber: Int,
    val status: Int // 0 = Not Memorized (Gray), 1 = In Progress (Orange), 2 = Memorized (Green)
)
