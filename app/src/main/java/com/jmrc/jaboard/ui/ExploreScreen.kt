package com.jmrc.jaboard.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class UtilityItem(val id: String, val title: String, val category: String, val description: String, val icon: ImageVector)
private val utilities = listOf(
    UtilityItem("battery", "Battery Monitor", "Device", "Charge, temperature and system health", Icons.Filled.BatteryFull),
    UtilityItem("ram", "RAM Monitor", "Device", "Live memory availability", Icons.Filled.Memory),
    UtilityItem("storage", "Storage Monitor", "Device", "Used and available device storage", Icons.Filled.Storage),
    UtilityItem("device", "Device Information", "Device", "Android version, model and hardware", Icons.Filled.Devices),
    UtilityItem("network", "Network Information", "Device", "Connection type and status", Icons.Filled.NetworkCheck),
    UtilityItem("weather", "Weather", "Lifestyle", "Current conditions and seven-day outlook", Icons.Filled.Cloud),
    UtilityItem("clock", "Clock", "Lifestyle", "Time, date and world-ready clock", Icons.Filled.Schedule),
    UtilityItem("calendar", "Calendar", "Lifestyle", "Upcoming events from your calendars", Icons.Filled.CalendarMonth),
    UtilityItem("notes", "Notes", "Productivity", "Private notes stored on this device", Icons.Filled.Notes),
    UtilityItem("todo", "To-do Lists", "Productivity", "A simple, persistent task list", Icons.Filled.TaskAlt),
    UtilityItem("calculator", "Calculator", "Productivity", "Everyday and scientific calculations", Icons.Filled.Calculate),
    UtilityItem("timer", "Timer", "Productivity", "Countdown with pause and reset", Icons.Filled.MoreTime),
    UtilityItem("stopwatch", "Stopwatch", "Productivity", "Precise elapsed time and laps", Icons.Filled.Timelapse),
    UtilityItem("music", "Music Player", "Media", "Controls for active Android media sessions", Icons.Filled.GraphicEq)
)

@Composable
fun ExploreScreen(onOpenUtility: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    val categories = listOf("All", "Device", "Lifestyle", "Productivity", "Media")
    val matching = utilities.filter { (category == "All" || it.category == category) && (query.isBlank() || it.title.contains(query, true) || it.description.contains(query, true)) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text("Explore", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 20.dp))
        Text("Useful tools, all in one place.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), label = { Text("Find a utility") }, singleLine = true)
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            categories.forEach { item -> FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) }) }
        }
        if (matching.isEmpty()) Text("No utilities match that search.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 22.dp))
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 18.dp)) {
            items(matching, key = { it.id }) { item ->
                Card(onClick = { onOpenUtility(item.id) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(item.icon, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(start = 14.dp)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.Filled.ArrowForward, contentDescription = "Open ${item.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
