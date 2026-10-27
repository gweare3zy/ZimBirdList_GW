package com.example.data.repository

import com.example.data.catalog.ZimbabweBirdCatalog
import com.example.data.db.TripDao
import com.example.data.db.TripSightingDao
import com.example.data.model.BirdSpecies
import com.example.data.model.Trip
import com.example.data.model.TripSighting
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class BirdRepository(
    private val tripDao: TripDao,
    private val sightingDao: TripSightingDao
) {

    val allTrips: Flow<List<Trip>> = tripDao.getAllTrips()

    val allSpecies: List<BirdSpecies> = ZimbabweBirdCatalog.speciesList

    fun getTripById(id: Long): Flow<Trip?> = tripDao.getTripById(id)

    suspend fun getTripByIdOnce(id: Long): Trip? = tripDao.getTripByIdOnce(id)

    fun getSeenSpeciesIds(tripId: Long): Flow<Set<String>> {
        return sightingDao.getSeenSpeciesIdsForTrip(tripId).map { it.toSet() }
    }

    suspend fun getSeenSpeciesForTripOnce(tripId: Long): List<BirdSpecies> {
        val sightings = sightingDao.getSightingsForTripOnce(tripId)
        val seenIds = sightings.map { it.speciesId }.toSet()
        return allSpecies.filter { seenIds.contains(it.id) }
    }

    suspend fun createTrip(name: String, dateMillis: Long, location: String = "", notes: String = ""): Long {
        val trip = Trip(
            name = name.trim(),
            dateMillis = dateMillis,
            location = location.trim(),
            notes = notes.trim()
        )
        return tripDao.insertTrip(trip)
    }

    suspend fun updateTrip(trip: Trip) {
        tripDao.updateTrip(trip)
    }

    suspend fun deleteTrip(tripId: Long) {
        sightingDao.deleteAllSightingsForTrip(tripId)
        tripDao.deleteTripById(tripId)
    }

    suspend fun toggleSeen(tripId: Long, speciesId: String, isSeen: Boolean) {
        if (isSeen) {
            sightingDao.insertSighting(TripSighting(tripId = tripId, speciesId = speciesId))
        } else {
            sightingDao.deleteSighting(tripId, speciesId)
        }
    }

    // Counts mapping: tripId -> count of seen birds
    val tripSightingCounts: Flow<Map<Long, Int>> = sightingDao.getAllSightings().map { sightings ->
        sightings.groupingBy { it.tripId }.eachCount()
    }

    suspend fun getTripCount(): Int = tripDao.getTripCount()

    suspend fun deduplicateStarterTrips() {
        val manaTrips = tripDao.getTripsByName("Mana Pools Zambezi Safari")
        if (manaTrips.size > 1) {
            for (dup in manaTrips.drop(1)) {
                deleteTrip(dup.id)
            }
        }
        val nyangaTrips = tripDao.getTripsByName("Nyanga Highlands Hike")
        if (nyangaTrips.size > 1) {
            for (dup in nyangaTrips.drop(1)) {
                deleteTrip(dup.id)
            }
        }
    }
}
