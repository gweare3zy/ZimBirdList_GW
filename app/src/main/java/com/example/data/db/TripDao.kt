package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY dateMillis DESC, id DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    fun getTripById(id: Long): Flow<Trip?>

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    suspend fun getTripByIdOnce(id: Long): Trip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip): Long

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteTripById(id: Long)

    @Query("SELECT COUNT(*) FROM trips")
    suspend fun getTripCount(): Int

    @Query("SELECT * FROM trips WHERE name = :name")
    suspend fun getTripsByName(name: String): List<Trip>

    @Query("SELECT * FROM trips")
    suspend fun getAllTripsOnce(): List<Trip>
}
