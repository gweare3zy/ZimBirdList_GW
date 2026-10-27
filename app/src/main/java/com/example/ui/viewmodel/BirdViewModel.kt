package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.catalog.ZimbabweBirdCatalog
import com.example.data.db.AppDatabase
import com.example.data.model.BirdSpecies
import com.example.data.model.Trip
import com.example.data.repository.BirdRepository
import com.example.pdf.TripPdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

enum class BirdFilterMode {
    ALL,
    SEEN,
    UNSEEN
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BirdViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BirdRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = BirdRepository(db.tripDao(), db.tripSightingDao())
        seedInitialTripsIfEmpty()
    }

    val allTrips: StateFlow<List<Trip>> = repository.allTrips
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val tripSightingCounts: StateFlow<Map<Long, Int>> = repository.tripSightingCounts
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val totalChecklistCount: Int = ZimbabweBirdCatalog.totalCount

    // Active trip screen state
    private val _selectedTripId = MutableStateFlow<Long?>(null)
    val selectedTripId: StateFlow<Long?> = _selectedTripId.asStateFlow()

    val selectedTrip: StateFlow<Trip?> = _selectedTripId
        .flatMapLatest { id ->
            if (id != null) repository.getTripById(id) else flowOf(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val seenSpeciesIds: StateFlow<Set<String>> = _selectedTripId
        .flatMapLatest { id ->
            if (id != null) repository.getSeenSpeciesIds(id) else flowOf(emptySet())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filterMode = MutableStateFlow(BirdFilterMode.ALL)
    val filterMode: StateFlow<BirdFilterMode> = _filterMode.asStateFlow()

    // Filtered and searched bird list
    val filteredBirds: StateFlow<List<BirdSpecies>> = combine(
        _searchQuery,
        _filterMode,
        seenSpeciesIds
    ) { query, filter, seenIds ->
        val trimmedQuery = query.trim().lowercase()
        ZimbabweBirdCatalog.speciesList.filter { bird ->
            val matchesQuery = if (trimmedQuery.isEmpty()) {
                true
            } else {
                val nameLower = bird.commonName.lowercase()
                val nameNoHyphen = nameLower.replace("-", " ")
                val queryNoHyphen = trimmedQuery.replace("-", " ")
                nameLower.contains(trimmedQuery) ||
                        nameNoHyphen.contains(queryNoHyphen) ||
                        (bird.rarityLabel?.lowercase()?.contains(trimmedQuery) == true)
            }

            val matchesFilter = when (filter) {
                BirdFilterMode.ALL -> true
                BirdFilterMode.SEEN -> seenIds.contains(bird.id)
                BirdFilterMode.UNSEEN -> !seenIds.contains(bird.id)
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ZimbabweBirdCatalog.speciesList
    )

    fun selectTrip(tripId: Long) {
        _selectedTripId.value = tripId
        _searchQuery.value = ""
        _filterMode.value = BirdFilterMode.ALL
    }

    fun clearSelectedTrip() {
        _selectedTripId.value = null
        _searchQuery.value = ""
        _filterMode.value = BirdFilterMode.ALL
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterMode(mode: BirdFilterMode) {
        _filterMode.value = mode
    }

    fun toggleBirdSeen(speciesId: String) {
        val tripId = _selectedTripId.value ?: return
        val isCurrentlySeen = seenSpeciesIds.value.contains(speciesId)
        viewModelScope.launch {
            repository.toggleSeen(tripId, speciesId, !isCurrentlySeen)
        }
    }

    fun createTrip(name: String, dateMillis: Long, location: String, notes: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val newId = repository.createTrip(name, dateMillis, location, notes)
            onCreated(newId)
        }
    }

    fun deleteTrip(tripId: Long) {
        viewModelScope.launch {
            if (_selectedTripId.value == tripId) {
                _selectedTripId.value = null
            }
            repository.deleteTrip(tripId)
        }
    }

    fun updateTrip(trip: Trip) {
        viewModelScope.launch {
            repository.updateTrip(trip)
        }
    }

    fun exportTripPdf(
        context: Context,
        tripId: Long,
        onSuccess: (File) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val trip = repository.getTripByIdOnce(tripId)
                if (trip == null) {
                    withContext(Dispatchers.Main) { onError("Trip not found") }
                    return@launch
                }
                val seenBirds = repository.getSeenSpeciesForTripOnce(tripId)
                val file = TripPdfExporter.generateTripPdf(context, trip, seenBirds)
                withContext(Dispatchers.Main) {
                    onSuccess(file)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.localizedMessage ?: "Failed to export PDF")
                }
            }
        }
    }

    private fun seedInitialTripsIfEmpty() {
        viewModelScope.launch {
            val prefs = getApplication<Application>().getSharedPreferences("zim_birds_prefs", Context.MODE_PRIVATE)
            val hasSeeded = prefs.getBoolean("has_seeded_starter_trips_v2", false)

            // Clean up any duplicates created by previous app launches
            repository.deduplicateStarterTrips()

            if (hasSeeded) {
                return@launch
            }

            val currentTripCount = repository.getTripCount()
            if (currentTripCount > 0) {
                prefs.edit().putBoolean("has_seeded_starter_trips_v2", true).apply()
                return@launch
            }

            val cal = Calendar.getInstance()
            val trip1Id = repository.createTrip(
                name = "Mana Pools Zambezi Safari",
                dateMillis = cal.timeInMillis,
                location = "Mana Pools National Park",
                notes = "Early morning walking safari along the riverbank."
            )
            val starterSeen = listOf(
                "african_fish_eagle",
                "lilac_breasted_roller",
                "pels_fishing_owl",
                "southern_carmine_bee_eater",
                "bateleur",
                "saddle_billed_stork",
                "white_backed_vulture",
                "goliath_heron",
                "african_jacana",
                "pied_kingfisher",
                "narina_trogon",
                "african_paradise_flycatcher"
            )
            for (sId in starterSeen) {
                repository.toggleSeen(trip1Id, sId, true)
            }

            cal.add(Calendar.DAY_OF_YEAR, -5)
            val trip2Id = repository.createTrip(
                name = "Nyanga Highlands Hike",
                dateMillis = cal.timeInMillis,
                location = "Eastern Highlands, Nyanga",
                notes = "Montane grassland and forest trails birding."
            )
            val starterSeen2 = listOf(
                "swynnertons_robin",
                "robertss_warbler",
                "cape_robin_chat",
                "malachite_sunbird",
                "gurneys_sugarbird",
                "augur_buzzard",
                "verreauxs_eagle"
            )
            for (sId in starterSeen2) {
                repository.toggleSeen(trip2Id, sId, true)
            }

            prefs.edit().putBoolean("has_seeded_starter_trips_v2", true).apply()
        }
    }
}
