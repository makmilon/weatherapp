package com.example.weather_app.ui.weather

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather_app.data.location.LocationTracker
import com.example.weather_app.data.model.LocationSuggestion
import com.example.weather_app.data.model.WeatherResponse
import com.example.weather_app.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val locationTracker: LocationTracker
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _locationSuggestions = MutableStateFlow<List<LocationSuggestion>>(emptyList())
    val locationSuggestions = _locationSuggestions.asStateFlow()

    private val _currentWeather = MutableStateFlow<WeatherResponse?>(null)
    val currentWeather = _currentWeather.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var lastLocationUpdate = 0L
    private val MIN_UPDATE_INTERVAL = 5 * 60 * 1000 // 5 minutes

    init {
        Log.d("WeatherViewModel", "Initializing WeatherViewModel")
        observeCurrentLocationWeather()
    }

    fun onLocationPermissionGranted() {
        Log.d("WeatherViewModel", "Location permission granted, fetching initial location")
        viewModelScope.launch {
            try {
                val lastLocation = locationTracker.getLastKnownLocation()
                if (lastLocation != null) {
                    Log.d("WeatherViewModel", "Initial location available: ${lastLocation.latitude}, ${lastLocation.longitude}")
                    updateWeatherForLocation(lastLocation)
                } else {
                    Log.d("WeatherViewModel", "No initial location available, waiting for location updates")
                    _error.value = "Waiting for location..."
                    _isLoading.value = false
                }
                // Start listening for location updates
                startLocationUpdates()
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error getting initial location", e)
                _error.value = "Unable to get location: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    private fun observeCurrentLocationWeather() {
        Log.d("WeatherViewModel", "Starting to observe current location weather")
        viewModelScope.launch {
            repository.getCurrentLocationWeather()
                .onStart { 
                    Log.d("WeatherViewModel", "Starting to collect weather updates") 
                }
                .catch { e ->
                    Log.e("WeatherViewModel", "Error observing current location weather", e)
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { weather ->
                    Log.d("WeatherViewModel", "Received weather update: ${weather?.location?.name}, isCurrent: ${weather != null}")
                    if (weather == null) {
                        Log.d("WeatherViewModel", "No current location weather found in database")
                    } else {
                        _currentWeather.value = weather
                        _isLoading.value = false
                    }
                }
        }
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            locationTracker.getLocationUpdates()
                .catch { e ->
                    Log.e("WeatherViewModel", "Error getting location updates", e)
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { location ->
                    Log.d("WeatherViewModel", "Location update received: ${location.latitude}, ${location.longitude}")
                    if (shouldUpdateWeather()) {
                        updateWeatherForLocation(location)
                    } else {
                        Log.d("WeatherViewModel", "Skipping weather update - too soon since last update")
                    }
                }
        }
    }

    private fun shouldUpdateWeather(): Boolean {
        val currentTime = System.currentTimeMillis()
        return currentTime - lastLocationUpdate >= MIN_UPDATE_INTERVAL
    }

    private suspend fun updateWeatherForLocation(location: Location) {
        try {
            _isLoading.value = true
            val query = "${location.latitude},${location.longitude}"
            Log.d("WeatherViewModel", "Fetching weather for location: $query")
            val weather = repository.fetchWeatherData(query, isCurrentLocation = true)
            Log.d("WeatherViewModel", "Weather fetched successfully for: ${weather.location.name}")
            lastLocationUpdate = System.currentTimeMillis()
            _error.value = null
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Error updating weather for location", e)
            _error.value = e.message
        } finally {
            _isLoading.value = false
        }
    }

    fun searchLocations(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            try {
                if (query.length >= 3) {
                    Log.d("WeatherViewModel", "Searching locations for query: $query")
                    val suggestions = repository.searchLocations(query)
                    Log.d("WeatherViewModel", "Found ${suggestions.size} suggestions")
                    _locationSuggestions.value = suggestions
                } else {
                    _locationSuggestions.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error searching locations", e)
                _error.value = e.message
            }
        }
    }

    fun selectLocation(location: LocationSuggestion) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val query = "${location.lat},${location.lon}"
                Log.d("WeatherViewModel", "Selecting location: ${location.name} with query: $query")
                val weather = repository.fetchWeatherData(query, isCurrentLocation = false)
                Log.d("WeatherViewModel", "Weather fetched successfully for selected location: ${weather.location.name}")
                _currentWeather.value = weather
                _error.value = null
                
                // Clear search state after selection
                _searchQuery.value = ""
                _locationSuggestions.value = emptyList()
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error selecting location", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun onLocationPermissionDenied() {
        Log.d("WeatherViewModel", "Location permission denied")
        _error.value = "Location permission is required to show weather for your current location"
        _isLoading.value = false
    }

    fun clearError() {
        _error.value = null
    }
} 