package com.jmrc.jaboard.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jmrc.jaboard.feature.CityResult
import com.jmrc.jaboard.feature.WeatherClient
import com.jmrc.jaboard.feature.WeatherReport
import com.jmrc.jaboard.feature.toCache
import com.jmrc.jaboard.feature.toReport
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

 data class WeatherUiState(val report: WeatherReport? = null, val loading: Boolean = false, val error: String? = null)

class JaBoardViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as JaBoardApplication
    private val db = app.database
    private val _settings = MutableStateFlow(SettingsState())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()
    private val _weather = MutableStateFlow(WeatherUiState())
    val weather: StateFlow<WeatherUiState> = _weather.asStateFlow()
    private val _cityResults = MutableStateFlow<List<CityResult>>(emptyList())
    val cityResults: StateFlow<List<CityResult>> = _cityResults.asStateFlow()
    private val _cityError = MutableStateFlow<String?>(null)
    val cityError: StateFlow<String?> = _cityError.asStateFlow()
    val notes = db.notes().observeAll()
    val tasks = db.todos().observeAll()
    private var lastCoordinates: Pair<Double, Double>? = null
    private var noteSaveJob: Job? = null
    private var citySearchJob: Job? = null

    init {
        viewModelScope.launch {
            app.settings.values.collectLatest { value ->
                _settings.value = value
                val lat = value.latitude
                val lon = value.longitude
                if (lat != null && lon != null) {
                    val coordinates = lat to lon
                    if (lastCoordinates != coordinates) {
                        lastCoordinates = coordinates
                        val cached = runCatching { db.weatherCache().get() }.getOrNull()
                        if (cached != null && cached.latitude == lat && cached.longitude == lon) {
                            _weather.value = WeatherUiState(report = runCatching { cached.toReport() }.getOrNull())
                        } else {
                            _weather.value = WeatherUiState()
                        }
                        refreshWeather()
                    }
                } else {
                    lastCoordinates = null
                    _weather.value = WeatherUiState()
                }
            }
        }
    }

    fun refreshWeather() {
        val current = _settings.value
        val lat = current.latitude ?: return
        val lon = current.longitude ?: return
        viewModelScope.launch {
            _weather.value = _weather.value.copy(loading = true, error = null)
            try {
                val report = WeatherClient.forecast(lat, lon)
                db.weatherCache().put(report.toCache(current.city, lat, lon))
                _weather.value = WeatherUiState(report = report)
            } catch (e: Exception) {
                _weather.value = _weather.value.copy(loading = false, error = e.message ?: "Couldn't update weather")
            }
        }
    }
    fun searchCities(query: String) {
        citySearchJob?.cancel()
        if (query.trim().length < 2) { _cityResults.value = emptyList(); _cityError.value = null; return }
        citySearchJob = viewModelScope.launch {
            delay(350)
            try { _cityResults.value = WeatherClient.searchCity(query); _cityError.value = null }
            catch (e: Exception) { if (e !is kotlinx.coroutines.CancellationException) { _cityResults.value = emptyList(); _cityError.value = e.message } }
        }
    }
    fun chooseCity(city: CityResult) { viewModelScope.launch { app.settings.saveCity(city.label, city.latitude, city.longitude); _cityResults.value = emptyList(); _cityError.value = null } }
    fun chooseTheme(value: String) { viewModelScope.launch { app.settings.setTheme(value) } }
    fun setDynamic(value: Boolean) { viewModelScope.launch { app.settings.setDynamic(value) } }
    fun updateDashboard(order: List<String> = _settings.value.order, hidden: Set<String> = _settings.value.hidden, sizes: Map<String, String> = _settings.value.sizes, preset: String = _settings.value.preset) {
        viewModelScope.launch { app.settings.saveDashboard(order, hidden, sizes, preset) }
    }
    fun resetDashboard() { updateDashboard(DashboardDefaults, emptySet(), emptyMap(), "Expressive Bento") }
    fun clearWeatherCache() { viewModelScope.launch { db.weatherCache().clear(); _weather.value = WeatherUiState() } }

    fun addNote(onCreated: (Long) -> Unit) = viewModelScope.launch {
        val id = db.notes().insert(Note(title = "Untitled note", body = ""))
        onCreated(id)
    }
    fun saveNote(note: Note) {
        noteSaveJob?.cancel()
        noteSaveJob = viewModelScope.launch {
            delay(250)
            db.notes().update(note.copy(updatedAt = System.currentTimeMillis()))
        }
    }
    fun deleteNote(note: Note) { viewModelScope.launch { db.notes().delete(note) } }
    fun togglePin(note: Note) { viewModelScope.launch { db.notes().update(note.copy(pinned = !note.pinned, updatedAt = System.currentTimeMillis())) } }

    fun addTask(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { db.todos().insert(TodoTask(title = title.trim())) }
    }
    fun toggleTask(task: TodoTask) { viewModelScope.launch { db.todos().update(task.copy(completed = !task.completed)) } }
    fun deleteTask(task: TodoTask) { viewModelScope.launch { db.todos().delete(task) } }
}
