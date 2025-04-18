package com.example.weather_app.di

import android.app.Application
import com.example.weather_app.data.location.DefaultLocationTracker
import com.example.weather_app.data.location.ILocationTracker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LocationModule {

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(app: Application): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(app)
    }

    @Provides
    @Singleton
    fun provideLocationTracker(
        fusedLocationProviderClient: FusedLocationProviderClient,
        application: Application
    ): ILocationTracker {
        return DefaultLocationTracker(
            locationClient = fusedLocationProviderClient,
            application = application
        )
    }
} 