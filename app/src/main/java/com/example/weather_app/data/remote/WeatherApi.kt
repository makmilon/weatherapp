package com.example.weather_app.data.remote

import com.example.weather_app.data.model.LocationSuggestion
import com.example.weather_app.data.model.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi {

    @GET("v1/forecast.json")
    suspend fun getWeatherForecast(
        @Query("key") apiKey: String,
        @Query("q") query: String,
        @Query("days") days: Int = 5,
        @Query("aqi") aqi: String = "no"
    ): WeatherResponse

    @GET("v1/search.json")
    suspend fun searchLocations(
        @Query("key") apiKey: String,
        @Query("q") query: String
    ): List<LocationSuggestion>

    @GET("v1/current.json")
    suspend fun getCurrentWeatherByCoordinates(
        @Query("key") apiKey: String,
        @Query("q") query: String
    ): WeatherResponse

    companion object {
        const val BASE_URL = "https://api.weatherapi.com/"
    }
}
