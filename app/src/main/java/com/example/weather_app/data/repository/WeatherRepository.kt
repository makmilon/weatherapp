package com.example.weather_app.data.repository

import android.util.Log
import com.example.weather_app.data.local.WeatherDao
import com.example.weather_app.data.local.WeatherEntity
import com.example.weather_app.data.model.LocationSuggestion
import com.example.weather_app.data.model.WeatherResponse
import com.example.weather_app.data.remote.WeatherApi
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    private val api: WeatherApi,
    private val dao: WeatherDao,
    private val gson: Gson
) {
    fun getCurrentLocationWeather(): Flow<WeatherResponse?> {
        Log.d("WeatherRepository", "Getting current location weather from database")
        return dao.getCurrentLocationWeather()
            .onStart { Log.d("WeatherRepository", "Starting to observe current location weather from DB") }
            .map { entity ->
                entity?.let {
                    Log.d("WeatherRepository", "Found weather data in DB for location: ${entity.locationName}, isCurrent: ${entity.isCurrent}")
                    gson.fromJson(it.weatherData, WeatherResponse::class.java)
                }.also { response ->
                    if (response == null) {
                        Log.d("WeatherRepository", "No current location weather data found in database")
                    }
                }
            }
    }

    suspend fun fetchWeatherData(location: String, isCurrentLocation: Boolean = false): WeatherResponse {
        try {
            Log.d("WeatherRepository", "Fetching weather data for location: $location (isCurrentLocation: $isCurrentLocation)")
            val response = api.getWeatherForecast(API_KEY, location)
            Log.d("WeatherRepository", "Successfully fetched weather data for: ${response.location.name}")

            // If this is current location, clear any previous current location data
            if (isCurrentLocation) {
                Log.d("WeatherRepository", "Clearing previous current location data")
                dao.clearCurrentLocationWeather()
            }

            // Store in local database
            insertWeather(response, isCurrentLocation)

            // Verify the data was properly stored
            val verifyEntity = dao.getWeatherByLocation(response.location.name)
            Log.d("WeatherRepository", "Verification - Found in DB: ${verifyEntity != null}, isCurrent: ${verifyEntity?.isCurrent}")

            return response
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error fetching weather data", e)
            throw e
        }
    }

    suspend fun insertWeather(weather: WeatherResponse, isCurrentLocation: Boolean) {
        try {
            val weatherEntity = WeatherEntity(
                locationName = weather.location.name,
                timestamp = System.currentTimeMillis(),
                weatherData = gson.toJson(weather),
                isCurrent = isCurrentLocation
            )
            Log.d("WeatherRepository", "Inserting weather data for ${weather.location.name}, isCurrent: $isCurrentLocation")
            dao.insertWeather(weatherEntity)
            Log.d("WeatherRepository", "Successfully inserted weather data for ${weather.location.name}")
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error inserting weather data", e)
            throw e
        }
    }

    suspend fun searchLocations(query: String): List<LocationSuggestion> {
        try {
            Log.d("WeatherRepository", "Searching locations for query: $query")
            val results = api.searchLocations(API_KEY, query)
            Log.d("WeatherRepository", "Found ${results.size} locations")
            return results
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error searching locations", e)
            throw e
        }
    }

    suspend fun clearOldData() {
        // Clear data older than 1 hour
        dao.deleteOldData(System.currentTimeMillis() - CACHE_DURATION)
    }

    suspend fun getLocationNameFromCoordinates(lat: Double, lon: Double): String {
        return try {
            Log.d("WeatherRepository", "Getting location name for coordinates: $lat, $lon")
            val response = api.getCurrentWeatherByCoordinates(API_KEY, "$lat,$lon")
            val locationName = response.location.name
            Log.d("WeatherRepository", "Found location name: $locationName")
            locationName
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error getting location name", e)
            "$lat,$lon" // fallback
        }
    }


    suspend fun clearCurrentLocationWeather() {
        try {
            Log.d("WeatherRepository", "Clearing previous current location weather data")
            dao.clearCurrentLocationWeather()
            Log.d("WeatherRepository", "Successfully cleared current location weather data")
        } catch (e: Exception) {
            Log.e("WeatherRepository", "Error clearing current location weather data", e)
            throw e
        }
    }

    companion object {
        // TODO: Replace this with your actual API key from https://www.weatherapi.com/
        private const val API_KEY = "4c81b8dabe98476389290514240809" // Get your key from: https://www.weatherapi.com/
        private const val CACHE_DURATION = 60 * 60 * 1000L // 1 hour in milliseconds
    }
}