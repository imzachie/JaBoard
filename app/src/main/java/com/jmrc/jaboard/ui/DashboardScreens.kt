package com.jmrc.jaboard.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.app.ActivityManager
import android.os.StatFs
import android.os.Environment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.jmrc.jaboard.data.DashboardDefaults
import com.jmrc.jaboard.data.JaBoardViewModel
import com.jmrc.jaboard.data.SettingsState
import com.jmrc.jaboard.feature.weatherDescription
import com.jmrc.jaboard.media.JaBoardMediaNotificationListener
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun HomeScreen(vm: JaBoardViewModel, onOpenUtility: (String) -> Unit, onOpenStudio: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val weather by vm.weather.collectAsState()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(start = 22.dp, top = 18.dp, end = 18.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("JaBoard", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                Text(DateFormat.getDateInstance(DateFormat.FULL).format(Date(now)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AssistChip(onClick = onOpenStudio, label = { Text("Edit") }, leadingIcon = { Icon(Icons.Filled.Tune, null) })
        }
        DashboardGrid(settings, weather.report, weather.loading, onOpenUtility, vm)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DashboardGrid(settings: SettingsState, report: com.jmrc.jaboard.feature.WeatherReport?, weatherLoading: Boolean, navigate: (String) -> Unit, vm: JaBoardViewModel) {
    val gridState = rememberLazyGridState()
    var order by remember(settings.order) { mutableStateOf(settings.order.ifEmpty { DashboardDefaults }) }
    var dragged by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    val visible = order.filterNot { it in settings.hidden }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 156.dp), state = gridState,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(visible, key = { it }, span = { name ->
            val size = settings.sizes[name] ?: defaultSize(name)
            if (size == "Wide" || size == "Expanded") GridItemSpan(maxLineSpan) else GridItemSpan(1)
        }) { name ->
            val size = settings.sizes[name] ?: defaultSize(name)
            val height = when (size) { "Expanded" -> 220.dp; "Wide" -> 164.dp; else -> 164.dp }
            DashboardCard(
                name = name, report = report, loading = weatherLoading, sizeLabel = size,
                modifier = Modifier.fillMaxWidth().height(height)
                    .graphicsLayer { if (dragged == name) { translationX = dragOffset.x; translationY = dragOffset.y; scaleX = 1.025f; scaleY = 1.025f } }
                    .pointerInput(name, settings.sizes) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { dragged = name; dragOffset = Offset.Zero },
                            onDragEnd = { dragged = null; dragOffset = Offset.Zero; vm.updateDashboard(order = order) },
                            onDragCancel = { dragged = null; dragOffset = Offset.Zero },
                            onDrag = { change, amount ->
                                change.consume()
                                if (dragged == name) {
                                    dragOffset += amount
                                    val layoutItems = gridState.layoutInfo.visibleItemsInfo
                                    val moving = layoutItems.firstOrNull { it.key == name }
                                    if (moving != null) {
                                        val centerX = moving.offset.x + moving.size.width / 2 + dragOffset.x
                                        val centerY = moving.offset.y + moving.size.height / 2 + dragOffset.y
                                        val target = layoutItems.filter { it.key != name }.minByOrNull {
                                            val dx = it.offset.x + it.size.width / 2 - centerX
                                            val dy = it.offset.y + it.size.height / 2 - centerY
                                            dx * dx + dy * dy
                                        }
                                        if (target != null) {
                                            val targetName = target.key as? String
                                            if (targetName != null) {
                                                val from = order.indexOf(name)
                                                val to = order.indexOf(targetName)
                                                if (from >= 0 && to >= 0 && from != to) order = order.toMutableList().apply { add(to, removeAt(from)) }
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    },
                onClick = { navigate(cardRoute(name)) }, onUtility = navigate
            )
        }
    }
}

private fun defaultSize(name: String) = when (name) { "Weather", "Quick Actions", "Calendar" -> "Wide"; "Now Playing" -> "Wide"; else -> "Compact" }
private fun cardRoute(name: String) = when (name) {
    "Clock" -> "clock"; "Weather" -> "weather"; "Battery" -> "battery"; "Now Playing" -> "music"
    "Memory" -> "ram"; "Storage" -> "storage"; "Calendar" -> "calendar"; else -> "device"
}

@Composable
private fun DashboardCard(name: String, report: com.jmrc.jaboard.feature.WeatherReport?, loading: Boolean, sizeLabel: String, modifier: Modifier, onClick: () -> Unit, onUtility: (String) -> Unit) {
    val context = LocalContext.current
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxSize().padding(17.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.OpenInNew, contentDescription = "Open $name", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when (name) {
                "Clock" -> {
                    var time by remember { mutableStateOf(java.text.SimpleDateFormat("h:mm", java.util.Locale.getDefault()).format(Date())) }
                    LaunchedEffect(Unit) { while (true) { time = java.text.SimpleDateFormat("h:mm", java.util.Locale.getDefault()).format(Date()); delay(1000) } }
                    Text(time, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    Text(java.text.SimpleDateFormat("EEEE · MMM d", java.util.Locale.getDefault()).format(Date()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                "Weather" -> if (report == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Cloud, null, tint = MaterialTheme.colorScheme.primary)
                        Text(if (loading) "Checking forecast…" else "Set up your location", style = MaterialTheme.typography.titleMedium)
                        Text("Choose a city or opt into location access", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${report.temperature.toInt()}°", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(weatherDescription(report.code), style = MaterialTheme.typography.titleSmall)
                            Text("Feels like ${report.feelsLike.toInt()}°", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text("Humidity ${report.humidity}%  ·  Wind ${report.wind.toInt()} km/h", style = MaterialTheme.typography.bodySmall)
                }
                "Battery" -> {
                    val battery = readBattery(context)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.BatteryFull, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                        Text("${battery.percent}%", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 10.dp))
                    }
                    Text(battery.status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(progress = { battery.percent / 100f }, modifier = Modifier.fillMaxWidth())
                }
                "Memory" -> {
                    val info = memoryInfo(context)
                    Text(formatBytes(info.first), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text("of ${formatBytes(info.second)} in use", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(progress = { if (info.second > 0) info.first.toFloat() / info.second else 0f }, modifier = Modifier.fillMaxWidth())
                }
                "Storage" -> {
                    val info = storageInfo()
                    Text(formatBytes(info.first), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text("of ${formatBytes(info.second)} used", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LinearProgressIndicator(progress = { if (info.second > 0) info.first.toFloat() / info.second else 0f }, modifier = Modifier.fillMaxWidth())
                }
                "Now Playing" -> {
                    val playback by JaBoardMediaNotificationListener.playback.collectAsState()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.GraphicEq, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Column(Modifier.padding(start = 10.dp)) {
                            Text(if (playback.connected) playback.title.ifBlank { "Media session active" } else "No media session", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (playback.connected) playback.artist else "Tap to connect a player", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                    Text(if (playback.playing) "Playing" else "Ready when you are", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                "Quick Actions" -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = { onUtility("notes") }, modifier = Modifier.weight(1f)) { Text("Notes") }
                    FilledTonalButton(onClick = { onUtility("todo") }, modifier = Modifier.weight(1f)) { Text("To-dos") }
                    FilledTonalButton(onClick = { onUtility("calculator") }, modifier = Modifier.weight(1f)) { Text("Calc") }
                }
                "Calendar" -> CalendarDashboardContent()
                else -> Text(sizeLabel, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}


@Composable
private fun CalendarDashboardContent() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var permitted by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) }
    var revision by remember { mutableStateOf(0) }
    var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permitted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
                revision++
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(permitted, revision) {
        events = if (permitted) runCatching { withContext(Dispatchers.IO) { loadCalendarEvents(context).take(2) } }.getOrDefault(emptyList()) else emptyList()
    }
    Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
    if (!permitted) {
        Text("Your day, at a glance", style = MaterialTheme.typography.titleMedium)
        Text("Allow calendar access to see upcoming events", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else if (events.isEmpty()) {
        Text("No upcoming events", style = MaterialTheme.typography.titleMedium)
        Text("Your calendar is clear for now", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        events.forEach { event ->
            Text(event.title.ifBlank { "Untitled event" }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(java.text.SimpleDateFormat("EEE · h:mm a", java.util.Locale.getDefault()).format(Date(event.start)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
