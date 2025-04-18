package com.example.weather_app

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.weather_app.ui.weather.WeatherScreen
import com.example.weather_app.ui.weather.WeatherViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.content.pm.PackageManager
import com.example.weather_app.ui.theme.WeatherAppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: WeatherViewModel by viewModels()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Request location permissions when app starts
        requestLocationPermissions()

        setContent {
            WeatherAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherScreen(viewModel = viewModel)
                }
            }
        }
    }

    private fun requestLocationPermissions() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, start getting weather
            viewModel.onLocationPermissionGranted()
        } else {
            // Request permission
            locationPermissionRequest.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                // Precise location granted, start getting weather
                viewModel.onLocationPermissionGranted()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Approximate location granted, start getting weather
                viewModel.onLocationPermissionGranted()
            }
            else -> {
                // No location access granted
                viewModel.onLocationPermissionDenied()
            }
        }
    }
}