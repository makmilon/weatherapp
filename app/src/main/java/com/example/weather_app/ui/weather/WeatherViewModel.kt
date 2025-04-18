package com.example.weather_app.ui.weather

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather_app.data.location.ILocationTracker
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
    private val locationTracker: ILocationTracker
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
        // Start observing database for any existing weather data
        observeCurrentLocationWeather()
    }

    fun onLocationPermissionGranted() {
        Log.d("WeatherViewModel", "Location permission granted, fetching initial location")
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val lastLocation = locationTracker.getLastKnownLocation()
                if (lastLocation != null) {
                    Log.d("WeatherViewModel", "Initial location available: ${lastLocation.latitude}, ${lastLocation.longitude}")
                    // Fetch and store weather data for current location
                    fetchWeatherForCurrentLocation(lastLocation)
                } else {
                    Log.d("WeatherViewModel", "No initial location available, waiting for location updates")
                    _error.value = "Waiting for location..."
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

    private suspend fun fetchWeatherForCurrentLocation(location: Location) {
        try {
            _isLoading.value = true
            val lat = location.latitude
            val lon = location.longitude
            
            // First get the location name from coordinates
            val locationName = repository.getLocationNameFromCoordinates(lat, lon)
            Log.d("WeatherViewModel", "Got location name: $locationName for coordinates: $lat, $lon")
            
            // Clear any previous current location data before fetching new data
            repository.clearCurrentLocationWeather()
            
            // Use the location name to fetch weather data
            Log.d("WeatherViewModel", "Fetching weather for current location: $locationName")
            val weather = repository.fetchWeatherData(locationName, isCurrentLocation = true)
            Log.d("WeatherViewModel", "Successfully fetched weather for current location: ${weather.location.name}")
            
            // Weather update will come through Room observation
            lastLocationUpdate = System.currentTimeMillis()
            _error.value = null
        } catch (e: Exception) {
            Log.e("WeatherViewModel", "Error fetching current location weather", e)
            _error.value = e.message
        } finally {
            _isLoading.value = false
        }
    }

    private fun observeCurrentLocationWeather() {
        Log.d("WeatherViewModel", "Starting to observe current location weather")
        viewModelScope.launch {
            repository.getCurrentLocationWeather()
                .onStart { 
                    Log.d("WeatherViewModel", "Starting to collect weather updates from database") 
                }
                .catch { e ->
                    Log.e("WeatherViewModel", "Error observing current location weather", e)
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { weather ->
                    Log.d("WeatherViewModel", "Received weather update from database: ${weather?.location?.name}")
                    _currentWeather.value = weather
                    if (weather != null) {
                        _isLoading.value = false
                        _error.value = null
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
                }
                .collect { location ->
                    Log.d("WeatherViewModel", "Location update received: ${location.latitude}, ${location.longitude}")
                    if (shouldUpdateWeather()) {
                        fetchWeatherForCurrentLocation(location)
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