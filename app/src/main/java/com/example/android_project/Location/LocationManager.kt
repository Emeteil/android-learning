package com.example.android_project.Location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.documentfile.provider.DocumentFile
import com.example.android_project.DataClasses.LocationData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONObject
import java.io.IOException

class LocationManager(private val context: Context, private var folderUri: Uri?)
{
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    init
    {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    }

    public fun GetLastLocation(callbackEnd: (LocationData) -> Unit)
    {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            val locationData = LocationData(0.0, 0.0, 0.0, 0L)
            callbackEnd(locationData)
            return
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1).build()

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val lat = location.latitude
                    val lon = location.longitude
                    val alt = location.altitude
                    val time = System.currentTimeMillis()

                    val locationData = LocationData(lat, lon, alt, time)

                    SaveJsonFile("locations", locationData.toJSONObject(), folderUri, context)

                    fusedLocationClient.removeLocationUpdates(this)
                    callbackEnd(locationData)
                }
            }
        },
            Looper.getMainLooper()
        )
    }

    companion object
    {
        public fun SaveJsonFile(fileName: String, jsonData: JSONObject, folderUri: Uri?, context: Context): Boolean
        {
            val dir = folderUri?.let { DocumentFile.fromTreeUri(context, it) } ?: return false

            if (!dir.canWrite())
                return false

            val jsonFileName = if (fileName.endsWith(".txt")) fileName else "$fileName.txt"

            val file = dir.findFile(jsonFileName) ?: dir.createFile(
                "text/plain",
                jsonFileName.removeSuffix(".txt")
            ) ?: return false

            try {
                val contentResolver = context.contentResolver
                contentResolver.openOutputStream(file.uri, "wa")?.use { outputStream ->
                    outputStream.write(jsonData.toString().toByteArray())
                    outputStream.write("\n".toByteArray())
                }
                return true
            } catch (e: IOException) {
                return false
            }
        }
    }
}