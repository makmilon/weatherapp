package com.example.weather_app.data.location

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface ILocationTracker {
    suspend fun getLastKnownLocation(): Location?
    fun getLocationUpdates(): Flow<Location>
} 