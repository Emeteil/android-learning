package com.example.android_project.Services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.example.android_project.Location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONObject

class LocationService : Service()
{
    private val CHANNEL_ID = "location_service_channel"
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var request: LocationRequest
    private var folderUri: Uri? = null

    private val locationCallback = object : LocationCallback()
    {
        override fun onLocationResult(locationResult: LocationResult)
        {
            locationResult.lastLocation?.let { location ->
                val data = JSONObject().apply {
                    put("Latitude", location.latitude)
                    put("Longitude", location.longitude)
                    put("Altitude", location.altitude)
                    put("Time", System.currentTimeMillis())
                }
                LocationManager.Companion.SaveJsonFile("locations", data, folderUri, applicationContext)
            }
        }
    }

    override fun onCreate()
    {
        super.onCreate()

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_ID,
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Отслеживание местоположения")
            .build()

        startForeground(1, notification)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()

        val saved = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("folder_uri", null)

        if (saved != null)
            folderUri = Uri.parse(saved)

        StartUpdates()
        Toast.makeText(this, "LocationService запущен", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy()
    {
        try
        {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            stopForeground(true)
            super.onDestroy()
        } catch (e: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun StartUpdates()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            stopSelf()
            return
        }

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            null
        )
    }
}