package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "trip_sightings",
    primaryKeys = ["tripId", "speciesId"],
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["tripId"])]
)
data class TripSighting(
    val tripId: Long,
    val speciesId: String,
    val seenAt: Long = System.currentTimeMillis()
)
