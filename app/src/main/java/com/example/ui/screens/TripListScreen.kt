package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Trip
import com.example.pdf.TripPdfExporter
import com.example.ui.viewmodel.BirdViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripListScreen(
    trips: List<Trip>,
    sightingCounts: Map<Long, Int>,
    totalSpeciesCount: Int,
    onTripClick: (Long) -> Unit,
    onTripDelete: (Long) -> Unit,
    onTripUpdate: (Trip) -> Unit,
    onCreateTrip: (name: String, dateMillis: Long, location: String, notes: String) -> Unit,
    viewModel: BirdViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var tripToEdit by remember { mutableStateOf<Trip?>(null) }
    var tripToDelete by remember { mutableStateOf<Trip?>(null) }
    var exportingTripId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Zimbabwe Bird Lists",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        )
                        Text(
                            text = "Official $totalSpeciesCount Species Checklist & Trip Log",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("create_trip_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Trip List"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "New List", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (trips.isEmpty()) {
                EmptyTripsView(
                    onCreateFirstTrip = { showCreateDialog = true },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("trip_list_column"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(trips, key = { it.id }) { trip ->
                        val seenCount = sightingCounts[trip.id] ?: 0
                        TripCard(
                            trip = trip,
                            seenCount = seenCount,
                            totalSpeciesCount = totalSpeciesCount,
                            onClick = { onTripClick(trip.id) },
                            onEdit = { tripToEdit = trip },
                            onDelete = { tripToDelete = trip },
                            onExportPdf = {
                                exportingTripId = trip.id
                                viewModel.exportTripPdf(
                                    context = context,
                                    tripId = trip.id,
                                    onSuccess = { file ->
                                        exportingTripId = null
                                        TripPdfExporter.sharePdf(context, file, trip.name)
                                    },
                                    onError = { err ->
                                        exportingTripId = null
                                        Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            isExporting = exportingTripId == trip.id
                        )
                    }
                    // Bottom spacer for FAB clearance
                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        TripEditorDialog(
            initialTrip = null,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, dateMillis, location, notes ->
                showCreateDialog = false
                onCreateTrip(name, dateMillis, location, notes)
            }
        )
    }

    tripToEdit?.let { trip ->
        TripEditorDialog(
            initialTrip = trip,
            onDismiss = { tripToEdit = null },
            onConfirm = { name, dateMillis, location, notes ->
                tripToEdit = null
                onTripUpdate(
                    trip.copy(
                        name = name,
                        dateMillis = dateMillis,
                        location = location,
                        notes = notes
                    )
                )
            }
        )
    }

    tripToDelete?.let { trip ->
        AlertDialog(
            onDismissRequest = { tripToDelete = null },
            title = { Text("Delete Trip List") },
            text = { Text("Are you sure you want to delete '${trip.name}' and all its recorded sightings? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onTripDelete(trip.id)
                        tripToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_trip_button")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { tripToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TripCard(
    trip: Trip,
    seenCount: Int,
    totalSpeciesCount: Int,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExportPdf: () -> Unit,
    isExporting: Boolean,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()) }
    val formattedDate = remember(trip.dateMillis) { dateFormatter.format(Date(trip.dateMillis)) }
    val progress = if (totalSpeciesCount > 0) seenCount.toFloat() / totalSpeciesCount.toFloat() else 0f

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trip_card_${trip.id}")
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trip.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (trip.location.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = trip.location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("edit_trip_button_${trip.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Trip",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onExportPdf,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("export_pdf_button_${trip.id}"),
                        enabled = !isExporting
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF Report",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("delete_trip_button_${trip.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete Trip",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sighting Progress Bar and Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$seenCount species observed",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val pct = (progress * 100).toInt()
                Text(
                    text = "$pct% of catalog",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
fun EmptyTripsView(
    onCreateFirstTrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Bird Lists Yet",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create a list to record the birds you see on your trip. Works 100% offline with the official Zimbabwe checklist.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateFirstTrip,
            modifier = Modifier.testTag("create_first_trip_button")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Trip List")
        }
    }
}
