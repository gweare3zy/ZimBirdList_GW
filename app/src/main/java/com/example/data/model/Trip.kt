package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dateMillis: Long,
    val location: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
