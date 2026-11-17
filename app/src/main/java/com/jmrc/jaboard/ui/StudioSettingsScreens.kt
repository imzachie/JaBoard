package com.jmrc.jaboard.ui

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.provider.Settings.Secure
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.jmrc.jaboard.data.DashboardDefaults
import com.jmrc.jaboard.data.JaBoardViewModel
import com.jmrc.jaboard.data.SettingsState
import com.jmrc.jaboard.media.JaBoardMediaNotificationListener

private val dashboardPresets = listOf("Expressive Bento", "System Telemetry", "Daily Focus", "Minimalist")

@Composable
fun StudioScreen(vm: JaBoardViewModel) {
    val settings by vm.settings.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp, bottom = 24.dp)) {
        item {
            Text("Studio", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text("Make your dashboard feel like yours.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
        }
        item {
            Text("Layout presets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            dashboardPresets.forEach { preset ->
                Card(onClick = { applyPreset(vm, preset) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = if (settings.preset == preset) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(preset, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(presetDescription(preset), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (settings.preset == preset) Text("Selected", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        item {
            Text("Arrange widgets", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            Text("Drag cards on Home or use the controls below.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val dashboardOrder = settings.order.ifEmpty { DashboardDefaults }
        items(dashboardOrder.size) { index ->
            val name = dashboardOrder[index]
            DashboardEditorRow(name, index, dashboardOrder, settings, vm)
        }
        item {
            OutlinedButton(onClick = vm::resetDashboard, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Restore, null); Text("  Restore default layout")
            }
        }
        item {
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Text("Appearance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Theme", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                listOf("System", "Light", "Dark").forEach { mode -> FilterChip(selected = settings.theme == mode, onClick = { vm.chooseTheme(mode) }, label = { Text(mode) }) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Material You colors", style = MaterialTheme.typography.titleMedium); Text("Use wallpaper colors on Android 12+", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
Switch(checked = settings.dynamicColor, onCheckedChange = vm::setDynamic)
            }
            Text("When turned off, JaBoard uses its Pixel Cobalt palette.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DashboardEditorRow(name: String, index: Int, defaults: List<String>, settings: SettingsState, vm: JaBoardViewModel) {
    val isHidden = name in settings.hidden
    var menuOpen by remember { mutableStateOf(false) }
    val currentSize = settings.sizes[name] ?: when (name) { "Weather", "Quick Actions", "Calendar", "Now Playing" -> "Wide"; else -> "Compact" }
    Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = !isHidden, onCheckedChange = { checked -> vm.updateDashboard(hidden = if (checked) settings.hidden - name else settings.hidden + name) })
            Column(Modifier.weight(1f)) { Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold); Text(if (isHidden) "Hidden from Home" else currentSize, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            TextButton(onClick = { menuOpen = true }, enabled = !isHidden) { Text("Size") }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                listOf("Compact", "Wide", "Expanded").forEach { size -> DropdownMenuItem(text = { Text(size) }, onClick = { vm.updateDashboard(sizes = settings.sizes + (name to size)); menuOpen = false }) }
            }
            IconButton(onClick = { moveDashboard(vm, settings, index, -1) }, enabled = index > 0) { Icon(Icons.Filled.ArrowUpward, "Move up") }
            IconButton(onClick = { moveDashboard(vm, settings, index, 1) }, enabled = index < defaults.lastIndex) { Icon(Icons.Filled.ArrowDownward, "Move down") }
        }
    }
}

private fun moveDashboard(vm: JaBoardViewModel, settings: SettingsState, index: Int, delta: Int) {
    val order = settings.order.ifEmpty { DashboardDefaults }.toMutableList()
    val from = order.indexOf(DashboardDefaults[index])
    val to = (from + delta).coerceIn(0, order.lastIndex)
    if (from >= 0 && from != to) { val moved = order.removeAt(from); order.add(to, moved); vm.updateDashboard(order = order) }
}

private fun applyPreset(vm: JaBoardViewModel, preset: String) {
    val order = when (preset) {
        "System Telemetry" -> listOf("Battery", "Memory", "Storage", "Now Playing", "Clock", "Weather", "Calendar", "Quick Actions")
        "Daily Focus" -> listOf("Clock", "Weather", "Calendar", "Quick Actions", "Battery", "Memory", "Storage", "Now Playing")
        "Minimalist" -> listOf("Clock", "Weather", "Battery", "Quick Actions", "Memory", "Storage", "Now Playing", "Calendar")
        else -> DashboardDefaults
    }
    val hidden = when (preset) {
        "Minimalist" -> setOf("Memory", "Storage", "Now Playing", "Calendar")
        "Daily Focus" -> setOf("Memory", "Storage", "Now Playing")
        else -> emptySet()
    }
    val sizes = when (preset) {
        "System Telemetry" -> mapOf("Battery" to "Wide", "Memory" to "Wide", "Storage" to "Wide")
        "Daily Focus" -> mapOf("Clock" to "Expanded", "Weather" to "Wide", "Calendar" to "Wide", "Quick Actions" to "Wide")
        "Minimalist" -> mapOf("Clock" to "Expanded", "Weather" to "Wide")
        else -> emptyMap()
    }
    vm.updateDashboard(order, hidden, sizes, preset)
}

private fun presetDescription(name: String) = when (name) {
    "System Telemetry" -> "Battery, memory and storage up front"
    "Daily Focus" -> "Weather, calendar and quick actions"
    "Minimalist" -> "A calm glance with fewer cards"
    else -> "The full mixed-size JaBoard dashboard"
}

@Composable
fun SettingsScreen(vm: JaBoardViewModel, onOpenAbout: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var permissionRefresh by remember { mutableStateOf(0) }
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event -> if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) permissionRefresh++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionRefresh++ }
    val calendarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionRefresh++ }
    val permissionRevision = permissionRefresh
    val locationGranted = permissionRevision >= 0 && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val calendarGranted = permissionRevision >= 0 && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    val mediaGranted = isMediaAccessGranted(context)
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp, bottom = 24.dp)) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Text("A private dashboard, configured your way.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        item {
            SettingsSectionTitle("Appearance")
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 6.dp)) { listOf("System", "Light", "Dark").forEach { mode -> FilterChip(selected = settings.theme == mode, onClick = { vm.chooseTheme(mode) }, label = { Text(mode) }) } }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Dynamic color", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall); Switch(checked = settings.dynamicColor, onCheckedChange = vm::setDynamic) }
                }
            }
        }
        item {
            SettingsSectionTitle("Permissions")
            Text("Optional access is requested only when you use the related feature.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
            PermissionRow("Location", "For nearby weather. A city can be chosen without it.", locationGranted, "Allow") {
                locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
            PermissionRow("Calendar", "Read upcoming events from calendars on this device.", calendarGranted, "Allow") {
                calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
            }
            PermissionRow("Media controls", "Connect to active media sessions from other apps.", mediaGranted, "Open settings") {
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        item {
            SettingsSectionTitle("Data & privacy")
            Text("Notes and tasks stay on this device. Weather uses Open-Meteo without an account; your chosen city coordinates are stored locally.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = vm::clearWeatherCache, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Clear saved weather forecast") }
            Text("No account, analytics, or advertising.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SettingsSectionTitle("About JaBoard")
            Card(onClick = onOpenAbout, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(16.dp)) { Text("JaBoard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text("Version 1.0.0 · Open-source licenses", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) }

@Composable
private fun PermissionRow(title: String, description: String, granted: Boolean, action: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); Text(if (granted) "Allowed" else "Not allowed", color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium) }
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
            TextButton(onClick = onClick, modifier = Modifier.align(Alignment.End)) { Text(if (granted && title != "Media controls") "Granted" else action) }
        }
    }
}

private fun isMediaAccessGranted(context: Context): Boolean = runCatching {
    if (Build.VERSION.SDK_INT >= 27) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.isNotificationListenerAccessGranted(ComponentName(context, JaBoardMediaNotificationListener::class.java))
    } else {
        val enabled = Secure.getString(context.contentResolver, "enabled_notification_listeners").orEmpty()
        enabled.contains(context.packageName)
    }
}.getOrDefault(false)
