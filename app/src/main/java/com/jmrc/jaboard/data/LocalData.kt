package com.jmrc.jaboard.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val body: String = "",
    val pinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TodoTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val details: String = "",
    val completed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "weather_cache")
data class WeatherCache(
    @PrimaryKey val id: Int = 1,
    val city: String,
    val latitude: Double,
    val longitude: Double,
    val payload: String,
    val fetchedAt: Long = System.currentTimeMillis()
)

@Dao
interface WeatherCacheDao {
    @Query("SELECT * FROM weather_cache WHERE id = 1") suspend fun get(): WeatherCache?
    @Query("DELETE FROM weather_cache") suspend fun clear()
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE) suspend fun put(cache: WeatherCache)
}

@Dao
interface NotesDao {
    @Query("SELECT * FROM notes ORDER BY pinned DESC, updatedAt DESC") fun observeAll(): Flow<List<Note>>
    @Insert suspend fun insert(note: Note): Long
    @Update suspend fun update(note: Note)
    @Delete suspend fun delete(note: Note)
}

@Dao
interface TodoDao {
    @Query("SELECT * FROM tasks ORDER BY completed ASC, createdAt DESC") fun observeAll(): Flow<List<TodoTask>>
    @Insert suspend fun insert(task: TodoTask): Long
    @Update suspend fun update(task: TodoTask)
    @Delete suspend fun delete(task: TodoTask)
}

@Database(entities = [Note::class, TodoTask::class, WeatherCache::class], version = 1, exportSchema = false)
abstract class JaBoardDatabase : RoomDatabase() {
    abstract fun notes(): NotesDao
    abstract fun todos(): TodoDao
    abstract fun weatherCache(): WeatherCacheDao

    companion object {
        @Volatile private var INSTANCE: JaBoardDatabase? = null
        fun get(context: Context): JaBoardDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, JaBoardDatabase::class.java, "jaboard.db").build().also { INSTANCE = it }
        }
    }
}

private val Context.settingsStore by preferencesDataStore(name = "jaboard_settings")
object SettingsKeys {
    val THEME = stringPreferencesKey("theme")
    val DYNAMIC = booleanPreferencesKey("dynamic_color")
    val ORDER = stringPreferencesKey("dashboard_order")
    val HIDDEN = stringPreferencesKey("dashboard_hidden")
    val SIZES = stringPreferencesKey("dashboard_sizes")
    val PRESET = stringPreferencesKey("dashboard_preset")
    val CITY = stringPreferencesKey("weather_city")
    val LAT = stringPreferencesKey("weather_lat")
    val LON = stringPreferencesKey("weather_lon")
}

class SettingsRepository(private val context: Context) {
    val values = context.settingsStore.data.map { prefs ->
        SettingsState(
            theme = prefs[SettingsKeys.THEME] ?: "System",
            dynamicColor = prefs[SettingsKeys.DYNAMIC] ?: true,
            order = (prefs[SettingsKeys.ORDER] ?: DashboardDefaults.joinToString(",")).split(",").filter(String::isNotBlank),
            hidden = (prefs[SettingsKeys.HIDDEN] ?: "").split(",").filter(String::isNotBlank).toSet(),
            sizes = (prefs[SettingsKeys.SIZES] ?: "").split(",").mapNotNull { entry -> entry.split(":", limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap(),
            preset = prefs[SettingsKeys.PRESET] ?: "Expressive Bento",
            city = prefs[SettingsKeys.CITY] ?: "",
            latitude = prefs[SettingsKeys.LAT]?.toDoubleOrNull(),
            longitude = prefs[SettingsKeys.LON]?.toDoubleOrNull()
        )
    }
    suspend fun setTheme(theme: String) = context.settingsStore.edit { it[SettingsKeys.THEME] = theme }
    suspend fun setDynamic(enabled: Boolean) = context.settingsStore.edit { it[SettingsKeys.DYNAMIC] = enabled }
    suspend fun saveDashboard(order: List<String>, hidden: Set<String>, sizes: Map<String, String>, preset: String) = context.settingsStore.edit {
        it[SettingsKeys.ORDER] = order.joinToString(",")
        it[SettingsKeys.HIDDEN] = hidden.joinToString(",")
        it[SettingsKeys.SIZES] = sizes.entries.joinToString(",") { entry -> "${entry.key}:${entry.value}" }
        it[SettingsKeys.PRESET] = preset
    }
    suspend fun saveCity(name: String, lat: Double, lon: Double) = context.settingsStore.edit {
        it[SettingsKeys.CITY] = name
        it[SettingsKeys.LAT] = lat.toString()
        it[SettingsKeys.LON] = lon.toString()
    }
    suspend fun clearCity() = context.settingsStore.edit { it.remove(SettingsKeys.CITY); it.remove(SettingsKeys.LAT); it.remove(SettingsKeys.LON) }
}

data class SettingsState(
    val theme: String = "System", val dynamicColor: Boolean = true,
    val order: List<String> = DashboardDefaults, val hidden: Set<String> = emptySet(),
    val sizes: Map<String, String> = emptyMap(), val preset: String = "Expressive Bento",
    val city: String = "", val latitude: Double? = null, val longitude: Double? = null
)

val DashboardDefaults = listOf("Clock", "Weather", "Battery", "Now Playing", "Memory", "Storage", "Quick Actions", "Calendar")
