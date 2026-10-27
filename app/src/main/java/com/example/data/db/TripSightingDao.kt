package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.TripSighting
import kotlinx.coroutines.flow.Flow

@Dao
interface TripSightingDao {
    @Query("SELECT * FROM trip_sightings WHERE tripId = :tripId")
    fun getSightingsForTrip(tripId: Long): Flow<List<TripSighting>>

    @Query("SELECT * FROM trip_sightings WHERE tripId = :tripId")
    suspend fun getSightingsForTripOnce(tripId: Long): List<TripSighting>

    @Query("SELECT speciesId FROM trip_sightings WHERE tripId = :tripId")
    fun getSeenSpeciesIdsForTrip(tripId: Long): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM trip_sightings WHERE tripId = :tripId")
    fun getSeenCountForTrip(tripId: Long): Flow<Int>

    @Query("SELECT * FROM trip_sightings")
    fun getAllSightings(): Flow<List<TripSighting>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSighting(sighting: TripSighting)

    @Query("DELETE FROM trip_sightings WHERE tripId = :tripId AND speciesId = :speciesId")
    suspend fun deleteSighting(tripId: Long, speciesId: String)

    @Query("DELETE FROM trip_sightings WHERE tripId = :tripId")
    suspend fun deleteAllSightingsForTrip(tripId: Long)
}
