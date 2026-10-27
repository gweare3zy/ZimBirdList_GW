package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Trip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripEditorDialog(
    initialTrip: Trip? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, dateMillis: Long, location: String, notes: String) -> Unit
) {
    var tripName by remember(initialTrip) { mutableStateOf(initialTrip?.name ?: "") }
    var location by remember(initialTrip) { mutableStateOf(initialTrip?.location ?: "") }
    var notes by remember(initialTrip) { mutableStateOf(initialTrip?.notes ?: "") }
    var selectedDateMillis by remember(initialTrip) {
        mutableStateOf(initialTrip?.dateMillis ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("d MMMM yyyy", Locale.getDefault()) }
    val isEditing = initialTrip != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEditing) "Edit Trip List Details" else "Create New Bird Trip List",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = tripName,
                    onValueChange = { tripName = it },
                    label = { Text("List / Trip Name *") },
                    placeholder = { Text("e.g. Hwange National Park Safari") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trip_name_input"),
                    singleLine = true
                )

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("select_date_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Date: ${dateFormatter.format(Date(selectedDateMillis))}")
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location / Trail (Optional)") },
                    placeholder = { Text("e.g. Zambezi River, Victoria Falls") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trip_location_input"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Trip Notes (Optional)") },
                    placeholder = { Text("Weather conditions, habitat details...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("trip_notes_input"),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tripName.isNotBlank()) {
                        onConfirm(tripName.trim(), selectedDateMillis, location.trim(), notes.trim())
                    }
                },
                enabled = tripName.isNotBlank(),
                modifier = Modifier.testTag("save_trip_button")
            ) {
                Text(if (isEditing) "Save Changes" else "Create List")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDateMillis = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
