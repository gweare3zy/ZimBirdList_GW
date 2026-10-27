package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.example.data.model.BirdSpecies
import com.example.data.model.Trip
import com.example.pdf.TripPdfExporter
import com.example.ui.theme.TagConfirmColor
import com.example.ui.theme.TagRareColor
import com.example.ui.theme.TagVagrantColor
import com.example.ui.viewmodel.BirdFilterMode
import com.example.ui.viewmodel.BirdViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripChecklistScreen(
    trip: Trip,
    birds: List<BirdSpecies>,
    seenSpeciesIds: Set<String>,
    totalCatalogCount: Int,
    searchQuery: String,
    filterMode: BirdFilterMode,
    onSearchChange: (String) -> Unit,
    onFilterChange: (BirdFilterMode) -> Unit,
    onToggleBirdSeen: (String) -> Unit,
    onTripUpdate: (Trip) -> Unit,
    onBack: () -> Unit,
    viewModel: BirdViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()) }
    val formattedDate = remember(trip.dateMillis) { dateFormatter.format(Date(trip.dateMillis)) }

    val seenCount = seenSpeciesIds.size
    val progress = if (totalCatalogCount > 0) seenCount.toFloat() / totalCatalogCount.toFloat() else 0f

    BackHandler {
        onBack()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = trip.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (trip.location.isNotBlank()) "$formattedDate • ${trip.location}" else formattedDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_to_trips_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Trips"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.testTag("edit_trip_details_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Trip Details"
                        )
                    }

                    IconButton(
                        onClick = {
                            isExporting = true
                            viewModel.exportTripPdf(
                                context = context,
                                tripId = trip.id,
                                onSuccess = { file ->
                                    isExporting = false
                                    TripPdfExporter.sharePdf(context, file, trip.name)
                                },
                                onError = { errorMsg ->
                                    isExporting = false
                                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        modifier = Modifier.testTag("export_trip_pdf_top_button"),
                        enabled = !isExporting
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF Report",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Stats & Progress Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$seenCount of $totalCatalogCount species seen",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val pct = (progress * 100).toInt()
                        Text(
                            text = "$pct%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_species_input"),
                    placeholder = { Text("Search 400+ Zimbabwe birds...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Search"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterMode == BirdFilterMode.ALL,
                    onClick = { onFilterChange(BirdFilterMode.ALL) },
                    label = { Text("All ($totalCatalogCount)") },
                    modifier = Modifier.testTag("filter_chip_all"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                FilterChip(
                    selected = filterMode == BirdFilterMode.SEEN,
                    onClick = { onFilterChange(BirdFilterMode.SEEN) },
                    label = { Text("Seen ($seenCount)") },
                    modifier = Modifier.testTag("filter_chip_seen"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                FilterChip(
                    selected = filterMode == BirdFilterMode.UNSEEN,
                    onClick = { onFilterChange(BirdFilterMode.UNSEEN) },
                    label = { Text("Unseen (${totalCatalogCount - seenCount})") },
                    modifier = Modifier.testTag("filter_chip_unseen"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            // Species List
            if (birds.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No birds found matching \"$searchQuery\"" else "No birds in this filter",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isNotEmpty() || filterMode != BirdFilterMode.ALL) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(
                                onClick = {
                                    onSearchChange("")
                                    onFilterChange(BirdFilterMode.ALL)
                                }
                            ) {
                                Text("Reset search and filters")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("bird_checklist_column"),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(birds, key = { it.id }) { bird ->
                        val isSeen = seenSpeciesIds.contains(bird.id)
                        BirdChecklistItem(
                            bird = bird,
                            isSeen = isSeen,
                            onToggleSeen = { onToggleBirdSeen(bird.id) }
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        TripEditorDialog(
            initialTrip = trip,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, dateMillis, location, notes ->
                showEditDialog = false
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
}

@Composable
fun BirdChecklistItem(
    bird: BirdSpecies,
    isSeen: Boolean,
    onToggleSeen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(onClick = onToggleSeen)
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("bird_row_${bird.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSeen,
            onCheckedChange = null,
            modifier = Modifier.testTag("bird_checkbox_${bird.id}"),
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = bird.commonName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (isSeen) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 15.sp
                    ),
                    color = if (isSeen) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                // Subtle text tag for rarity
                bird.rarityLabel?.let { tagText ->
                    val tagColor = when (bird.rarityTag) {
                        "R" -> TagRareColor
                        "V" -> TagVagrantColor
                        "?" -> TagConfirmColor
                        else -> MaterialTheme.colorScheme.outline
                    }
                    Text(
                        text = "• $tagText",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        color = tagColor
                    )
                }
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    )
}
