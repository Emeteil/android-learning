package com.example.android_project

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.documentfile.provider.DocumentFile
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONObject
import java.io.IOException

class Location : AppCompatActivity()
{
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var info: TextView
    private lateinit var buttonGet: Button
    private lateinit var buttonService: Button

    private var folderUri: Uri? = null
    private var serviceEnabled = false

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null)
            return@registerForActivityResult

        contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit { putString("folder_uri", uri.toString()) }

        folderUri = uri
    }

    private val backgroundLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted)
            return@registerForActivityResult

        Toast.makeText(this, "Нужно разрешить доступ в фоне для коректной работы приложения!", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_location)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        info = findViewById(R.id.infoText)
        buttonGet = findViewById(R.id.buttonGet)
        buttonService = findViewById(R.id.buttonService)

        val saved = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("folder_uri", null)

        if (saved != null)
            folderUri = saved.toUri() else folderPicker.launch(null)
    }

    override fun onResume()
    {
        super.onResume()

        serviceEnabled = IsLocationServiceRunning()
        UpdateServiceButton()

        buttonGet.setOnClickListener { GetLastLocation() }
        buttonService.setOnClickListener { ToggleService() }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun IsLocationServiceRunning(): Boolean
    {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == LocationService::class.java.name }
    }

    private fun GetLastLocation()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        buttonGet.isEnabled = false
        info.text = ""
        buttonGet.text = "Подождите..."

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1).build()

        fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        val lat = location.latitude
                        val lon = location.longitude
                        val alt = location.altitude
                        val time = System.currentTimeMillis()

                        info.text = "Lat: $lat\nLon: $lon\nAlt: $alt\nTime: $time"

                        val data = JSONObject()
                        data.put("Latitude", lat)
                        data.put("Longitude", lon)
                        data.put("Altitude", alt)
                        data.put("Time", time)

                        SaveJsonFile("locations", data)

                        fusedLocationClient.removeLocationUpdates(this)
                        buttonGet.text = "Получить данные"
                        buttonGet.isEnabled = true
                    }
                }
            },
            Looper.getMainLooper()
        )
    }

    private fun SaveJsonFile(fileName: String, jsonData: JSONObject): Boolean
    {
        val dir = folderUri?.let { DocumentFile.fromTreeUri(this, it) } ?:
        return false

        if (!dir.canWrite())
            return false

        val jsonFileName = if (fileName.endsWith(".txt")) fileName else "$fileName.txt"

        val file = dir.findFile(jsonFileName) ?: dir.createFile("text/plain",
            jsonFileName.removeSuffix(".txt")) ?: return false

        try
        {
            contentResolver.openOutputStream(file.uri, "wa")?.use { outputStream ->
                outputStream.write(jsonData.toString().toByteArray())
                outputStream.write("\n".toByteArray())
            }
            return true
        }
        catch (e: IOException) { return false }
    }

    private fun UpdateServiceButton()
    {
        buttonService.text = if (serviceEnabled) "Остановить сбор в фоне" else "Запустить сбор в фоне"
    }

    private fun ToggleService()
    {
        val intent = Intent(this, LocationService::class.java)
        if (serviceEnabled)
        {
            stopService(intent)
            serviceEnabled = false
        }
        else
        {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                startForegroundService(intent)
                serviceEnabled = true
            }
            else
            {
                Toast.makeText(this, "Необходимы разрешения на доступ к местоположению", Toast.LENGTH_SHORT).show()
            }
        }
        UpdateServiceButton()
    }
}