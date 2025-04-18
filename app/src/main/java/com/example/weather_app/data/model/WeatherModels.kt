package com.example.weather_app.data.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val location: Location,
    val current: Current,
    val forecast: Forecast
)

data class Location(
    val name: String,
    val region: String,
    val country: String,
    val lat: Double,
    val lon: Double,
    @SerializedName("localtime")
    val localTime: String
)

data class Current(
    @SerializedName("temp_c")
    val tempC: Double,
    @SerializedName("temp_f")
    val tempF: Double,
    val condition: Condition,
    @SerializedName("wind_kph")
    val windKph: Double,
    @SerializedName("wind_dir")
    val windDir: String,
    val humidity: Int,
    @SerializedName("feelslike_c")
    val feelsLikeC: Double
)

data class Condition(
    val text: String,
    val icon: String,
    val code: Int
)

data class Forecast(
    val forecastday: List<ForecastDay>
)

data class ForecastDay(
    val date: String,
    val day: Day,
    val astro: Astro,
    val hour: List<Hour>
)

data class Day(
    @SerializedName("maxtemp_c")
    val maxTempC: Double,
    @SerializedName("mintemp_c")
    val minTempC: Double,
    val condition: Condition,
    @SerializedName("daily_chance_of_rain")
    val chanceOfRain: Int
)

data class Astro(
    val sunrise: String,
    val sunset: String
)

data class Hour(
    val time: String,
    @SerializedName("temp_c")
    val tempC: Double,
    val condition: Condition
)

data class LocationSuggestion(
    val id: Int,
    val name: String,
    val region: String,
    val country: String,
    val lat: Double,
    val lon: Double
) 