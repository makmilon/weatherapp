package com.example.weather_app.ui.weather

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.weather_app.data.model.LocationSuggestion
import com.example.weather_app.data.model.WeatherResponse
import com.example.weather_app.ui.components.ErrorMessage
import com.example.weather_app.ui.components.LoadingIndicator
import com.example.weather_app.ui.components.WeatherCard
import com.example.weather_app.ui.components.WeatherIcon
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeatherScreen(
    viewModel: WeatherViewModel
) {
    val currentWeather by viewModel.currentWeather.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val locationSuggestions by viewModel.locationSuggestions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(top = 30.dp)
    ) {
        SearchBar(
            query = searchQuery,
            onQueryChange = viewModel::searchLocations,
            suggestions = locationSuggestions,
            onSuggestionSelected = viewModel::selectLocation
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            LoadingIndicator()
        } else if (currentWeather != null) {
            WeatherContent(weather = currentWeather!!)
        } else {
            // Show a message when no weather data is available
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No weather data available",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        error?.let { errorMessage ->
            ErrorMessage(
                message = errorMessage,
                onDismiss = viewModel::clearError
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<LocationSuggestion>,
    onSuggestionSelected: (LocationSuggestion) -> Unit
) {
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search location") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            singleLine = true
        )

        if (suggestions.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column {
                    suggestions.forEach { suggestion ->
                        ListItem(
                            headlineContent = { Text(suggestion.name) },
                            supportingContent = { 
                                Text(
                                    text = buildString {
                                        append(suggestion.region)
                                        if (suggestion.country.isNotEmpty()) {
                                            append(", ")
                                            append(suggestion.country)
                                        }
                                    }
                                )
                            },
                            modifier = Modifier
                                .clickable { 
                                    onSuggestionSelected(suggestion)
                                }
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun WeatherContent(weather: WeatherResponse) {
    Column {
        Text(
            text = "Current Weather for ${weather.location.name}",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        CurrentWeatherCard(weather = weather)
        Spacer(modifier = Modifier.height(16.dp))
        ForecastCard(weather = weather)
    }
}

@Composable
fun CurrentWeatherCard(weather: WeatherResponse) {
    WeatherCard(
        title = weather.location.name,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    WeatherIcon(
                        iconUrl = weather.current.condition.icon,
                        contentDescription = weather.current.condition.text,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "${weather.current.tempC}°C",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = weather.current.condition.text,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WeatherInfo("Humidity", "${weather.current.humidity}%")
                    WeatherInfo("Wind", "${weather.current.windKph} km/h")
                    WeatherInfo("Feels like", "${weather.current.feelsLikeC}°C")
                }
            }
        }
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ForecastCard(weather: WeatherResponse) {
    WeatherCard(
        title = "5-Day Forecast",
        content = {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                items(weather.forecast.forecastday) { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = formatDate(day.date),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        WeatherIcon(
                            iconUrl = day.day.condition.icon,
                            contentDescription = day.day.condition.text,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${day.day.maxTempC}°/${day.day.minTempC}°",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = day.day.condition.text,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun WeatherInfo(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
private fun formatDate(dateStr: String): String {
    val date = LocalDate.parse(dateStr)
    return date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
} 