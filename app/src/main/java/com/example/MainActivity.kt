package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.TripChecklistScreen
import com.example.ui.screens.TripListScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BirdViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BirdApp()
                }
            }
        }
    }
}

@Composable
fun BirdApp(
    viewModel: BirdViewModel = viewModel()
) {
    val trips by viewModel.allTrips.collectAsStateWithLifecycle()
    val sightingCounts by viewModel.tripSightingCounts.collectAsStateWithLifecycle()
    val selectedTrip by viewModel.selectedTrip.collectAsStateWithLifecycle()
    val filteredBirds by viewModel.filteredBirds.collectAsStateWithLifecycle()
    val seenSpeciesIds by viewModel.seenSpeciesIds.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterMode by viewModel.filterMode.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = selectedTrip,
        transitionSpec = {
            if (targetState != null) {
                (slideInHorizontally { width -> width } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
            } else {
                (slideInHorizontally { width -> -width } + fadeIn())
                    .togetherWith(slideOutHorizontally { width -> width } + fadeOut())
            }
        },
        label = "ScreenTransition"
    ) { currentTrip ->
        if (currentTrip != null) {
            TripChecklistScreen(
                trip = currentTrip,
                birds = filteredBirds,
                seenSpeciesIds = seenSpeciesIds,
                totalCatalogCount = viewModel.totalChecklistCount,
                searchQuery = searchQuery,
                filterMode = filterMode,
                onSearchChange = { viewModel.setSearchQuery(it) },
                onFilterChange = { viewModel.setFilterMode(it) },
                onToggleBirdSeen = { viewModel.toggleBirdSeen(it) },
                onTripUpdate = { viewModel.updateTrip(it) },
                onBack = { viewModel.clearSelectedTrip() },
                viewModel = viewModel
            )
        } else {
            TripListScreen(
                trips = trips,
                sightingCounts = sightingCounts,
                totalSpeciesCount = viewModel.totalChecklistCount,
                onTripClick = { tripId -> viewModel.selectTrip(tripId) },
                onTripDelete = { tripId -> viewModel.deleteTrip(tripId) },
                onTripUpdate = { viewModel.updateTrip(it) },
                onCreateTrip = { name, dateMillis, location, notes ->
                    viewModel.createTrip(name, dateMillis, location, notes) { newId ->
                        viewModel.selectTrip(newId)
                    }
                },
                viewModel = viewModel
            )
        }
    }
}
