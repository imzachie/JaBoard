package com.jmrc.jaboard.ui

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.jmrc.jaboard.R
import java.util.Locale

 data class BatterySnapshot(
    val percent: Int = 0, val status: String = "Unknown", val source: String = "Unknown",
    val temperature: Float? = null, val voltage: Int? = null, val technology: String? = null,
    val health: String = "Unavailable"
)

fun readBattery(context: Context): BatterySnapshot {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return BatterySnapshot()
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
    val status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
        else -> "Status unavailable"
    }
    val source = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC power"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
        else -> "Battery"
    }
    val temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1).takeIf { it >= 0 }?.div(10f)
    val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1).takeIf { it >= 0 }
    val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf(String::isNotBlank)
    val health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheated"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "System reported a failure"
        else -> "Unavailable"
    }
    return BatterySnapshot(if (level >= 0) (level * 100 / scale).coerceIn(0, 100) else 0, status, source, temperature, voltage, technology, health)
}

fun memoryInfo(context: Context): Pair<Long, Long> {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val info = ActivityManager.MemoryInfo()
    manager.getMemoryInfo(info)
    return (info.totalMem - info.availMem).coerceAtLeast(0L) to info.totalMem
}

fun storageInfo(): Pair<Long, Long> = runCatching {
    val stat = StatFs(Environment.getDataDirectory().path)
    (stat.totalBytes - stat.availableBytes).coerceAtLeast(0L) to stat.totalBytes
}.getOrDefault(0L to 0L)

fun formatBytes(bytes: Long): String {
    if (bytes < 1_000_000_000L) return String.format(Locale.getDefault(), "%.0f MB", bytes / 1_000_000.0)
    return String.format(Locale.getDefault(), "%.1f GB", bytes / 1_000_000_000.0)
}

data class NetworkSnapshot(val connected: Boolean, val type: String, val metered: Boolean)
fun networkInfo(context: Context): NetworkSnapshot {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = manager.activeNetwork ?: return NetworkSnapshot(false, "Offline", false)
    val caps = manager.getNetworkCapabilities(network) ?: return NetworkSnapshot(false, "Offline", false)
    val type = when {
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile data"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        else -> "Connected"
    }
    return NetworkSnapshot(true, type, manager.isActiveNetworkMetered)
}
