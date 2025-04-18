package com.example.weather_app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "weather_data")
data class WeatherEntity(
    @PrimaryKey
    val locationName: String,
    val timestamp: Long,
    val weatherData: String, // JSON string of WeatherResponse
    val isCurrent: Boolean = false
)

@Dao
interface WeatherDao {
    @Query("SELECT * FROM weather_data WHERE isCurrent = 1 LIMIT 1")
    fun getCurrentLocationWeather(): Flow<WeatherEntity?>

    @Query("SELECT * FROM weather_data WHERE locationName = :location LIMIT 1")
    suspend fun getWeatherByLocation(location: String): WeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeather(weather: WeatherEntity)

    @Query("DELETE FROM weather_data WHERE locationName = :location")
    suspend fun deleteWeather(location: String)

    @Query("DELETE FROM weather_data WHERE timestamp < :timestamp")
    suspend fun deleteOldData(timestamp: Long)

    @Query("UPDATE weather_data SET isCurrent = 0 WHERE isCurrent = 1")
    suspend fun clearCurrentLocationWeather()
}

@Database(entities = [WeatherEntity::class], version = 1)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
} 