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
import android.location.LocationManager
import android.content.Context
import android.app.AlertDialog
import android.provider.Settings
import android.content.Intent

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
        when {
            // Check if we have both permissions
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                // Both permissions are granted, check if location is enabled
                checkLocationServicesAndProceed()
            }
            else -> {
                // Request permissions
                locationPermissionRequest.launch(arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ))
            }
        }
    }

    private fun checkLocationServicesAndProceed() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (isGpsEnabled || isNetworkEnabled) {
            // Location services are enabled, proceed with getting weather
            viewModel.onLocationPermissionGranted()
        } else {
            // Show location settings dialog
            showLocationSettingsDialog()
        }
    }

    private fun showLocationSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Location Services Required")
            .setMessage("Please enable location services to get weather for your current location")
            .setPositiveButton("Open Settings") { _, _ ->
                // Open location settings
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                viewModel.onLocationPermissionDenied()
            }
            .show()
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                // Precise location granted, check location services
                checkLocationServicesAndProceed()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Approximate location granted, check location services
                checkLocationServicesAndProceed()
            }
            else -> {
                // No location access granted
                viewModel.onLocationPermissionDenied()
            }
        }
    }
}