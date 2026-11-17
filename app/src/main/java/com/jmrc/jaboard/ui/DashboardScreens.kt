package com.jmrc.jaboard.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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

@Composable
fun HomeScreen(vm: JaBoardViewModel, onOpenUtility: (String) -> Unit, onOpenStudio: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val weather by vm.weather.collectAsState()
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }
    val greeting = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(Modifier.fillMaxWidth().padding(start = 22.dp, top = 20.dp, end = 18.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("JABOARD  ·  YOUR SPACE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.4.sp)
                Text(greeting, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
            }
            AssistChip(onClick = onOpenStudio, label = { Text("Studio") }, leadingIcon = { Icon(Icons.Filled.Tune, null) })
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarMonth, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
            Text(DateFormat.getDateInstance(DateFormat.FULL).format(Date(now)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f).padding(start = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("HOLD TO REARRANGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
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
                modifier = Modifier.animateItem().fillMaxWidth().height(height)
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
private fun AnimatedMetricBar(progress: Float, modifier: Modifier = Modifier) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "metricProgress"
    )
    LinearProgressIndicator(progress = { animatedProgress }, modifier = modifier)
}

@Composable
private fun DashboardCard(name: String, report: com.jmrc.jaboard.feature.WeatherReport?, loading: Boolean, sizeLabel: String, modifier: Modifier, onClick: () -> Unit, onUtility: (String) -> Unit) {
    val context = LocalContext.current
    val containerColor = when (name) {
        "Weather", "Calendar" -> MaterialTheme.colorScheme.tertiaryContainer
        "Clock", "Battery", "Now Playing" -> MaterialTheme.colorScheme.primaryContainer
        "Memory", "Storage" -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val leadingIcon = when (name) {
        "Clock" -> Icons.Filled.AccessTime
        "Weather" -> Icons.Filled.Cloud
        "Battery" -> Icons.Filled.BatteryFull
        "Now Playing" -> Icons.Filled.GraphicEq
        "Memory" -> Icons.Filled.Memory
        "Storage" -> Icons.Filled.Storage
        "Quick Actions" -> Icons.Filled.Bolt
        else -> Icons.Filled.CalendarMonth
    }
    val cardShape = when (name) {
        "Clock", "Weather" -> RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp, bottomEnd = 32.dp, bottomStart = 14.dp)
        "Quick Actions" -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomEnd = 14.dp, bottomStart = 28.dp)
        else -> RoundedCornerShape(28.dp)
    }
    Card(modifier = modifier.animateContentSize().clickable(onClick = onClick), shape = cardShape, colors = CardDefaults.cardColors(containerColor = containerColor), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.fillMaxSize().padding(17.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)), contentAlignment = Alignment.Center) {
                    Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 10.dp))
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
                        Text(battery.percent?.let { "$it%" } ?: "Unavailable", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 10.dp))
                    }
                    Text(listOfNotNull(battery.status, battery.currentNowMa?.let { "${formatCurrent(it)} now" }).joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    battery.percent?.let { AnimatedMetricBar(progress = it / 100f, modifier = Modifier.fillMaxWidth()) }
                }
                "Memory" -> {
                    val info = memorySnapshot(context)
                    Text(formatBytes(info.used), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text("${formatBytes(info.available)} available · ${formatBytes(info.total)} RAM", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AnimatedMetricBar(progress = if (info.total > 0) info.used.toFloat() / info.total else 0f, modifier = Modifier.fillMaxWidth())
                    if (info.lowMemory) Text("Android reports low memory", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
                "Storage" -> {
                    val info = storageInfo()
                    Text(if (info.second > 0) formatBytes(info.first) else "Unavailable", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text(if (info.second > 0) "of ${formatBytes(info.second)} used" else "Storage stats not reported", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AnimatedMetricBar(progress = if (info.second > 0) info.first.toFloat() / info.second else 0f, modifier = Modifier.fillMaxWidth())
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
                "Quick Actions" -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(onClick = { onUtility("notes") }, modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Notes, null, modifier = Modifier.size(18.dp)); Text("Notes", style = MaterialTheme.typography.labelSmall, maxLines = 1) } }
                    FilledTonalButton(onClick = { onUtility("todo") }, modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.TaskAlt, null, modifier = Modifier.size(18.dp)); Text("To-dos", style = MaterialTheme.typography.labelSmall, maxLines = 1) } }
                    FilledTonalButton(onClick = { onUtility("calculator") }, modifier = Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 10.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Calculate, null, modifier = Modifier.size(18.dp)); Text("Calc", style = MaterialTheme.typography.labelSmall, maxLines = 1) } }
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
