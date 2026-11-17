package com.jmrc.jaboard.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import android.provider.CalendarContract
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.jmrc.jaboard.data.JaBoardViewModel
import com.jmrc.jaboard.data.Note
import com.jmrc.jaboard.data.TodoTask
import com.jmrc.jaboard.feature.CalculatorEngine
import com.jmrc.jaboard.feature.CityResult
import com.jmrc.jaboard.feature.WeatherReport
import com.jmrc.jaboard.feature.weatherDescription
import com.jmrc.jaboard.media.JaBoardMediaNotificationListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@Composable
fun UtilityScreen(id: String, vm: JaBoardViewModel, onBack: () -> Unit) {
    when (id) {
        "battery" -> BatteryScreen(onBack)
        "ram" -> MonitorScreen("Memory", onBack)
        "storage" -> MonitorScreen("Storage", onBack)
        "device" -> DeviceScreen(onBack)
        "network" -> NetworkScreen(onBack)
        "weather" -> WeatherScreen(vm, onBack)
        "music" -> MusicScreen(onBack)
        "calendar" -> CalendarScreen(onBack)
        "notes" -> NotesScreen(vm, onBack)
        "todo" -> TodoScreen(vm, onBack)
        "calculator" -> CalculatorScreen(onBack)
        "timer" -> TimerScreen(onBack)
        "stopwatch" -> StopwatchScreen(onBack)
        "clock" -> ClockScreen(onBack)
        "about" -> AboutScreen(onBack)
        else -> DeviceScreen(onBack)
    }
}

@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun UtilityColumn(title: String, onBack: () -> Unit, scroll: Boolean = true, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title, onBack)
        val body = if (scroll) Modifier.fillMaxSize().verticalScroll(rememberScrollState()) else Modifier.fillMaxSize()
        Column(body.padding(horizontal = 20.dp), content = content)
    }
}

@Composable
private fun InfoCard(title: String, value: String, detail: String? = null) {
    Card(Modifier.fillMaxWidth().padding(bottom = 10.dp).animateContentSize(), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomEnd = 24.dp, bottomStart = 10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 15.dp)) {
            Text(title.uppercase(Locale.getDefault()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.7.sp)
            AnimatedContent(targetState = value, label = "metric-$title") { displayedValue ->
                Text(displayedValue, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp))
            }
            if (!detail.isNullOrBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun BatteryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var snapshot by remember { mutableStateOf(readBattery(context)) }
    LaunchedEffect(Unit) { while (true) { snapshot = readBattery(context); delay(4_000) } }
    UtilityColumn("Battery Monitor", onBack) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Card(Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.BatteryFull, null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(snapshot.percent?.let { "$it%" } ?: "Unavailable", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(snapshot.status, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                snapshot.percent?.let { percentage ->
                    LinearProgressIndicator(progress = { percentage / 100f }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp))
                }
            }
            InfoCard("Power source", snapshot.source, "Reported by Android BatteryManager")
            InfoCard("Instant current", snapshot.currentNowMa?.let(::formatCurrent) ?: "Not reported", "BatteryManager reading; sign follows the device's charge convention")
            InfoCard("Average current", snapshot.currentAverageMa?.let(::formatCurrent) ?: "Not reported")
            InfoCard("Charge counter", snapshot.chargeCounterMah?.let { String.format(Locale.getDefault(), "%.0f mAh", it) } ?: "Not reported", "Hardware support varies by device")
            InfoCard("Battery temperature", snapshot.temperature?.let { String.format(Locale.getDefault(), "%.1f °C", it) } ?: "Unavailable")
            InfoCard("Voltage", snapshot.voltage?.let { "$it mV" } ?: "Unavailable")
            InfoCard("Battery technology", snapshot.technology ?: "Not reported")
            InfoCard("System health", snapshot.health, "Health is shown only when Android reports it")
        }
    }
}

@Composable
private fun MonitorScreen(title: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val isMemory = title == "Memory"
    var sample by remember { mutableStateOf(if (isMemory) memoryInfo(context) else storageInfo()) }
    var memory by remember { mutableStateOf(if (isMemory) memorySnapshot(context) else null) }
    LaunchedEffect(title) {
        while (true) {
            if (isMemory) {
                memory = memorySnapshot(context)
                memory?.let { sample = it.used to it.total }
            } else sample = storageInfo()
            delay(3_000)
        }
    }
    val used = sample.first
    val total = sample.second
    UtilityColumn(if (isMemory) "RAM Monitor" else "Storage Monitor", onBack) {
        Text(if (isMemory) "System-wide memory snapshot" else "Internal shared storage", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))
        if (!isMemory && total <= 0L) {
            InfoCard("Storage statistics", "Unavailable", "Android couldn't read the shared storage capacity.")
        } else {
            InfoCard("Used", formatBytes(used), "of ${formatBytes(total)} total")
            InfoCard("Available", formatBytes((total - used).coerceAtLeast(0)))
            if (isMemory) {
                memory?.let { info ->
                    InfoCard("Low-memory threshold", formatBytes(info.threshold), if (info.lowMemory) "Android currently reports low memory" else "Android does not currently report low memory")
                }
            }
            LinearProgressIndicator(progress = { if (total > 0) used.toFloat() / total else 0f }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
        }
        Text(if (isMemory) "Android's system memory information; not per-app RAM. Available memory includes memory Android can reclaim." else "Values reflect the device data partition and are refreshed periodically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DeviceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val metrics = context.resources.displayMetrics
    val uptimeSeconds = SystemClock.elapsedRealtime() / 1_000
    val uptime = "${uptimeSeconds / 86_400}d ${(uptimeSeconds / 3_600) % 24}h ${(uptimeSeconds / 60) % 60}m"
    UtilityColumn("Device Information", onBack) {
        InfoCard("Manufacturer", Build.MANUFACTURER.replaceFirstChar { it.uppercase() })
        InfoCard("Device model", Build.MODEL)
        InfoCard("Android version", "Android ${Build.VERSION.RELEASE}", "API level ${Build.VERSION.SDK_INT}")
        InfoCard("Security patch", Build.VERSION.SECURITY_PATCH.takeIf { it.isNotBlank() } ?: "Not reported")
        InfoCard("Hardware platform", Build.HARDWARE.ifBlank { "Not reported" })
        if (Build.VERSION.SDK_INT >= 31) InfoCard("Processor model", Build.SOC_MODEL.takeIf { it.isNotBlank() } ?: "Not reported")
        InfoCard("Supported ABIs", Build.SUPPORTED_ABIS.take(3).joinToString().ifBlank { "Not reported" })
        InfoCard("Screen", "${metrics.widthPixels} × ${metrics.heightPixels} px", "${metrics.densityDpi} dpi")
        InfoCard("Device uptime", uptime, "Since the last device boot")
        InfoCard("Kernel", System.getProperty("os.version")?.takeIf { it.isNotBlank() } ?: "Not reported")
        Text("Android does not expose reliable live CPU utilization to ordinary apps; JaBoard does not invent one.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun NetworkScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var network by remember { mutableStateOf(networkInfo(context)) }
    LaunchedEffect(Unit) { while (true) { network = networkInfo(context); delay(3_000) } }
    UtilityColumn("Network Information", onBack) {
        InfoCard("Connection", network.type, if (network.connected) if (network.validated) "Internet access validated by Android" else "Connected, but internet access is not validated" else "No active network")
        InfoCard("Metered status", if (!network.connected) "Not connected" else if (network.metered) "Metered" else "Unmetered", "Reported by Android ConnectivityManager")
        InfoCard("Downstream link estimate", network.downstreamKbps?.let(::formatBandwidth) ?: "Not reported", "Android capability estimate, not a live speed test")
        InfoCard("Upstream link estimate", network.upstreamKbps?.let(::formatBandwidth) ?: "Not reported", "Android capability estimate, not a live speed test")
        Text("JaBoard does not inspect traffic, run speed tests, or collect network activity.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WeatherScreen(vm: JaBoardViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsState()
    val weather by vm.weather.collectAsState()
    val cities by vm.cityResults.collectAsState()
    val cityError by vm.cityError.collectAsState()
    var query by remember { mutableStateOf("") }
    var locationMessage by remember { mutableStateOf<String?>(null) }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) useLastLocation(context) { city, error -> if (city != null) vm.chooseCity(city) else locationMessage = error }
        else locationMessage = "Location permission was declined. Search for a city instead."
    }
    UtilityColumn("Weather", onBack) {
        Text("Forecasts by Open-Meteo · No account required", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(query, { query = it; vm.searchCities(it) }, modifier = Modifier.weight(1f), label = { Text("Search city") }, singleLine = true, shape = RoundedCornerShape(18.dp))
            IconButton(onClick = { if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) useLastLocation(context) { city, error -> if (city != null) vm.chooseCity(city) else locationMessage = error } else locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }) { Icon(Icons.Filled.LocationOn, "Use current location") }
        }
        Text("Location is optional. You can search for a city without granting access.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp, bottom = 8.dp))
        cityError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        locationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        cities.take(5).forEach { city ->
            TextButtonRow(city.label) { vm.chooseCity(city); query = "" }
        }
        if (settings.city.isNotBlank()) {
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Cloud, null, tint = MaterialTheme.colorScheme.primary)
                Text(settings.city, Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                IconButton(onClick = vm::refreshWeather) { Icon(Icons.Filled.Refresh, "Refresh forecast") }
            }
        }
        if (weather.loading && weather.report == null) BoxCenter { CircularProgressIndicator() }
        weather.report?.let { WeatherDetails(it) }
        if (weather.report == null && !weather.loading && settings.city.isBlank()) {
            Card(Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(18.dp)) { Text("Choose your weather location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text("Search for a city above, or use your current location if you prefer. No location is selected until you choose one.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp)) }
            }
        }
        weather.error?.let { Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Text(if (weather.report == null) "Forecast unavailable: $it" else "Couldn't refresh. Showing the last saved forecast. $it", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onErrorContainer) } }
    }
}

private fun useLastLocation(context: Context, result: (CityResult?, String?) -> Unit) {
    try {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val best = manager.getProviders(true).mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }.maxByOrNull { it.time }
        if (best == null) result(null, "Android has no recent location. Search for a city instead.")
        else result(CityResult("Current location", "", "", best.latitude, best.longitude), null)
    } catch (_: SecurityException) { result(null, "Location access is unavailable. Search for a city instead.") }
}

@Composable
private fun TextButtonRow(text: String, onClick: () -> Unit) { TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) } }

@Composable
private fun BoxCenter(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun WeatherDetails(report: WeatherReport) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(20.dp)) {
                Text("${report.temperature.toInt()}°", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(weatherDescription(report.code), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Feels like ${report.feelsLike.toInt()}°", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        InfoCard("Humidity", "${report.humidity}%")
        InfoCard("Wind", "${report.wind.toInt()} km/h")
        InfoCard("Rain chance", report.rainChance?.let { "$it%" } ?: "Not reported")
        Text("Hourly", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
        report.hourly.take(12).forEach { (time, temp) -> Text("$time     ${temp.toInt()}°", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp)) }
        Text("7-day forecast", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
        report.daily.forEach { day ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(day.date, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                Text(weatherDescription(day.code), Modifier.weight(1.3f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${day.high.toInt()}° / ${day.low.toInt()}°", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable
private fun MusicScreen(onBack: () -> Unit) {
    val playback by JaBoardMediaNotificationListener.playback.collectAsState()
    val context = LocalContext.current
    var position by remember(playback.positionMs) { mutableStateOf(playback.positionMs.toFloat()) }
    LaunchedEffect(playback.positionMs, playback.playing, playback.connected) {
        val started = SystemClock.elapsedRealtime()
        while (playback.connected && playback.playing) {
            position = (playback.positionMs + SystemClock.elapsedRealtime() - started).coerceAtMost(playback.durationMs).toFloat()
            delay(500)
        }
    }
    UtilityColumn("Music & media", onBack) {
        Icon(Icons.Filled.GraphicEq, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp).padding(bottom = 12.dp))
        if (!playback.connected) {
            Text("Connect your media sessions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Android protects playback metadata and controls. Grant notification access to JaBoard so it can read active media sessions from your music or podcast apps. JaBoard does not play or simulate audio.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 10.dp))
            Button(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) { Text("Open notification access") }
        } else {
            playback.artwork?.let { artwork -> Image(artwork.asImageBitmap(), contentDescription = "Album artwork", modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(26.dp))) }
            Text(playback.title.ifBlank { "Media session active" }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 14.dp))
            Text(playback.artist, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            if (playback.durationMs > 0) {
                Slider(value = position.coerceIn(0f, playback.durationMs.toFloat()), onValueChange = { position = it }, onValueChangeFinished = { JaBoardMediaNotificationListener.seekTo(position.toLong()) }, valueRange = 0f..playback.durationMs.toFloat(), modifier = Modifier.padding(top = 14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(formatTimer(position.toLong())); Text(formatTimer(playback.durationMs)) }
            }
            Text(if (playback.playing) "Playing" else "Paused", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = JaBoardMediaNotificationListener::previous) { Icon(Icons.Filled.SkipPrevious, "Previous") }
                IconButton(onClick = JaBoardMediaNotificationListener::playPause) { Icon(if (playback.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, if (playback.playing) "Pause" else "Play") }
                IconButton(onClick = JaBoardMediaNotificationListener::next) { Icon(Icons.Filled.SkipNext, "Next") }
            }
            Text("Playback is controlled through the active Android MediaSession.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

internal data class CalendarEvent(val title: String, val start: Long, val end: Long, val location: String?)

@Composable
private fun CalendarScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var permission by remember { mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) }
    var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> permission = granted }
    val lifecycleOwner = LocalLifecycleOwner.current
    var refresh by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) { permission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED; refresh++ } }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(permission, refresh) {
        if (permission) {
            try { events = withContext(Dispatchers.IO) { loadCalendarEvents(context) }; error = null }
            catch (e: Exception) { events = emptyList(); error = e.message ?: "Calendar could not be read" }
        }
    }
    UtilityColumn("Calendar", onBack) {
        if (!permission) {
            Text("Calendar access is optional", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("JaBoard reads event titles and times from calendars on this device to show your upcoming schedule. Nothing is uploaded.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
            Button(onClick = { launcher.launch(Manifest.permission.READ_CALENDAR) }) { Text("Allow calendar access") }
            Text("You can continue using JaBoard without granting access.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp))
        } else if (events.isEmpty()) {
            Text(error ?: "No upcoming events", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(if (error == null) "Events in the next 30 days will appear here." else "Check calendar access and try again.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("Coming up", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 12.dp))
            events.forEach { event ->
                val start = SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault()).format(Date(event.start))
                InfoCard(event.title.ifBlank { "Untitled event" }, start, event.location)
            }
        }
    }
}

internal fun loadCalendarEvents(context: Context): List<CalendarEvent> {
    val now = System.currentTimeMillis()
    val end = now + 30L * 24 * 60 * 60 * 1000
    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().appendPath(now.toString()).appendPath(end.toString()).build()
    val projection = arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.EVENT_LOCATION)
    val list = mutableListOf<CalendarEvent>()
    context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
        val title = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
        val begin = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
        val finish = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
        val place = cursor.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)
        while (cursor.moveToNext() && list.size < 50) list += CalendarEvent(cursor.getString(title) ?: "", cursor.getLong(begin), cursor.getLong(finish), if (place >= 0) cursor.getString(place) else null)
    }
    return list
}

@Composable
private fun NotesScreen(vm: JaBoardViewModel, onBack: () -> Unit) {
    val notes by vm.notes.collectAsState(initial = emptyList())
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Note?>(null) }
    val matches = notes.filter { search.isBlank() || it.title.contains(search, true) || it.body.contains(search, true) }
    Scaffold(floatingActionButton = { FloatingActionButton(onClick = { vm.addNote { id -> selected = Note(id = id, title = "", body = "") } }) { Icon(Icons.Filled.Edit, "New note") } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader("Notes", onBack)
            OutlinedTextField(search, { search = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), label = { Text("Search notes") }, singleLine = true, shape = RoundedCornerShape(18.dp))
            if (matches.isEmpty()) Text(if (search.isBlank()) "Your notes are saved only on this device." else "No matching notes.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp))
            else LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(matches, key = { it.id }) { note ->
                    Card(onClick = { selected = note }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.padding(15.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(note.title.ifBlank { "Untitled note" }, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                IconButton(onClick = { vm.togglePin(note) }) { Icon(Icons.Filled.PushPin, if (note.pinned) "Unpin" else "Pin", tint = if (note.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                                IconButton(onClick = { vm.deleteNote(note) }) { Icon(Icons.Filled.Delete, "Delete note") }
                            }
                            if (note.body.isNotBlank()) Text(note.body, maxLines = 3, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
    selected?.let { note -> NoteEditor(note, onDismiss = { selected = null }, onChange = { updated -> selected = updated; vm.saveNote(updated) }) }
}

@Composable
private fun NoteEditor(note: Note, onDismiss: () -> Unit, onChange: (Note) -> Unit) {
    var title by remember(note.id) { mutableStateOf(note.title) }
    var body by remember(note.id) { mutableStateOf(note.body) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit note") },
        text = { Column {
            OutlinedTextField(title, { title = it; onChange(note.copy(title = it, body = body)) }, modifier = Modifier.fillMaxWidth(), label = { Text("Title") }, singleLine = true)
            OutlinedTextField(body, { body = it; onChange(note.copy(title = title, body = it)) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Write something…") }, minLines = 5)
        } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun TodoScreen(vm: JaBoardViewModel, onBack: () -> Unit) {
    val tasks by vm.tasks.collectAsState(initial = emptyList())
    var title by remember { mutableStateOf("") }
    UtilityColumn("To-do list", onBack) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(title, { title = it }, modifier = Modifier.weight(1f), label = { Text("Add a task") }, singleLine = true, shape = RoundedCornerShape(18.dp))
            IconButton(onClick = { vm.addTask(title); title = "" }) { Icon(Icons.Filled.PlayArrow, "Add task") }
        }
        if (tasks.isEmpty()) Text("Your tasks are stored locally. Add one to get started.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 18.dp))
        tasks.forEach { task ->
            Card(Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(task.completed, { vm.toggleTask(task) })
                    Text(task.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None)
                    IconButton(onClick = { vm.deleteTask(task) }) { Icon(Icons.Filled.Delete, "Delete task") }
                }
            }
        }
    }
}

@Composable
private fun CalculatorScreen(onBack: () -> Unit) {
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val buttons = listOf("AC", "( )", "%", "÷", "7", "8", "9", "×", "4", "5", "6", "−", "1", "2", "3", "+", "±", "0", ".", "=")
    UtilityColumn("Calculator", onBack, scroll = false) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            Spacer(Modifier.weight(1f))
            Text(expression.ifBlank { "0" }, Modifier.fillMaxWidth().padding(vertical = 8.dp), textAlign = TextAlign.End, style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Monospace, maxLines = 2)
            result?.let { Text(it, Modifier.fillMaxWidth(), textAlign = TextAlign.End, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary) }
            error?.let { Text(it, Modifier.fillMaxWidth(), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.error) }
            val rows = buttons.chunked(4)
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        FilledTonalButton(onClick = {
                            error = null
                            when (key) {
                                "AC" -> { expression = ""; result = null }
                                "=" -> try { result = formatResult(CalculatorEngine.evaluate(expression)); error = null } catch (e: Exception) { error = e.message ?: "Invalid expression" }
                                "±" -> expression = if (expression.startsWith("-")) expression.drop(1) else "-$expression"
                                "( )" -> expression += if (expression.count { it == '(' } > expression.count { it == ')' }) ")" else "("
                                else -> { expression += key; result = null }
                            }
                        }, modifier = Modifier.weight(1f).padding(vertical = 4.dp), shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(vertical = 17.dp)) { Text(key, style = MaterialTheme.typography.titleLarge) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("sin(", "cos(", "tan(", "sqrt(", "ln(", "π", "^2").forEach { key ->
                    TextButton(onClick = { expression += key }) { Text(key) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private fun formatResult(value: Double): String = if (value == value.toLong().toDouble()) value.toLong().toString() else String.format(Locale.getDefault(), "%.8f", value).trimEnd('0').trimEnd('.')

@Composable
private fun StopwatchScreen(onBack: () -> Unit) {
    var running by remember { mutableStateOf(false) }
    var startAt by remember { mutableStateOf(0L) }
    var accumulated by remember { mutableStateOf(0L) }
    var elapsed by remember { mutableStateOf(0L) }
    val laps = remember { mutableStateListOf<Long>() }
    LaunchedEffect(running, startAt, accumulated) {
        while (running) { elapsed = accumulated + (SystemClock.elapsedRealtime() - startAt); delay(30) }
    }
    UtilityColumn("Stopwatch", onBack, scroll = false) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(formatElapsed(elapsed), style = MaterialTheme.typography.displayLarge, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 22.dp)) {
                Button(onClick = { if (running) { accumulated += SystemClock.elapsedRealtime() - startAt; running = false } else { startAt = SystemClock.elapsedRealtime(); running = true } }) { Text(if (running) "Pause" else "Start") }
                OutlinedButton(onClick = { if (running) laps.add(elapsed) else { accumulated = 0; elapsed = 0; laps.clear() } }) { Text(if (running) "Lap" else "Reset") }
            }
            if (laps.isNotEmpty()) Column(Modifier.fillMaxWidth().padding(top = 24.dp)) { laps.asReversed().forEachIndexed { index, lap -> Text("Lap ${laps.size - index}     ${formatElapsed(lap)}", Modifier.fillMaxWidth().padding(vertical = 5.dp), style = MaterialTheme.typography.titleMedium) } }
        }
    }
}

private fun formatElapsed(milliseconds: Long): String {
    val hours = milliseconds / 3_600_000
    val minutes = milliseconds / 60_000 % 60
    val seconds = milliseconds / 1_000 % 60
    val centiseconds = milliseconds / 10 % 100
    return if (hours > 0) String.format(Locale.getDefault(), "%d:%02d:%02d.%02d", hours, minutes, seconds, centiseconds) else String.format(Locale.getDefault(), "%02d:%02d.%02d", minutes, seconds, centiseconds)
}

@Composable
private fun TimerScreen(onBack: () -> Unit) {
    var minutesInput by remember { mutableStateOf("1") }
    var secondsInput by remember { mutableStateOf("00") }
    var remaining by remember { mutableStateOf(60_000L) }
    var running by remember { mutableStateOf(false) }
    var endAt by remember { mutableStateOf(0L) }
    LaunchedEffect(running, endAt) {
        while (running) {
            remaining = (endAt - SystemClock.elapsedRealtime()).coerceAtLeast(0)
            if (remaining == 0L) { running = false; break }
            delay(100)
        }
    }
    val context = LocalContext.current
    UtilityColumn("Countdown timer", onBack, scroll = false) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (!running && remaining == 60_000L) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(minutesInput, { minutesInput = it.filter(Char::isDigit).take(3) }, label = { Text("Minutes") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    Text(":", style = MaterialTheme.typography.headlineLarge)
                    OutlinedTextField(secondsInput, { secondsInput = it.filter(Char::isDigit).take(2) }, label = { Text("Seconds") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
            }
            Text(formatTimer(remaining), style = MaterialTheme.typography.displayLarge, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 22.dp))
            if (remaining == 0L && !running) Text("Time is up", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.tertiary)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    if (running) running = false else {
                        if (remaining == 60_000L || remaining == 0L) remaining = ((minutesInput.toLongOrNull() ?: 0) * 60 + (secondsInput.toLongOrNull() ?: 0)).coerceAtLeast(1) * 1000
                        endAt = SystemClock.elapsedRealtime() + remaining
                        running = true
                    }
                }) { Text(if (running) "Pause" else if (remaining == 0L) "Start again" else "Start") }
                OutlinedButton(onClick = { running = false; remaining = 60_000L; minutesInput = "1"; secondsInput = "00" }) { Text("Reset") }
            }
        }
    }
}

private fun formatTimer(ms: Long) = String.format(Locale.getDefault(), "%02d:%02d", ms / 60_000, ms / 1_000 % 60)

@Composable
private fun ClockScreen(onBack: () -> Unit) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1_000) } }
    UtilityColumn("Clock", onBack, scroll = false) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(SimpleDateFormat("h:mm:ss", Locale.getDefault()).format(Date(now)), style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            Text(SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(now)), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 12.dp))
            Text(SimpleDateFormat("zzzz", Locale.getDefault()).format(Date(now)), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    UtilityColumn("About JaBoard", onBack) {
        Text("JaBoard", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        Text("A calm, private dashboard for the things you check every day.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
        InfoCard("Version", "1.0.0", "Package: com.jmrc.jaboard")
        Text("Open-source components", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 8.dp))
        listOf("Jetpack Compose and Material 3 — Apache License 2.0", "AndroidX Core, Lifecycle, Navigation, Room and DataStore — Apache License 2.0", "Kotlin and kotlinx.coroutines — Apache License 2.0", "Open-Meteo weather and geocoding APIs — attribution to Open-Meteo").forEach { InfoCard(it.substringBefore(" —"), it.substringAfter("— ", "See component license")) }
        Text("No analytics or advertising SDKs are included.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
    }
}
