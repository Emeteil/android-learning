package com.example.android_project.Activites

import android.Manifest
import android.app.ActivityManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import androidx.preference.PreferenceManager
import com.example.android_project.Location.LocationData
import com.example.android_project.Location.LocationManager
import com.example.android_project.Location.LocationService
import com.example.android_project.R

class LocationActivity : AppCompatActivity()
{
    private lateinit var locationManager: LocationManager
    private lateinit var info: TextView
    private lateinit var buttonGet: Button
    private lateinit var buttonService: Button
    private lateinit var buttonBack: Button

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

    private val backgroundLocationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
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

        info = findViewById(R.id.infoText)
        buttonGet = findViewById(R.id.buttonGet)
        buttonService = findViewById(R.id.buttonService)
        buttonBack = findViewById(R.id.buttonBack)

        val saved = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("folder_uri", null)

        if (saved != null)
            folderUri = saved.toUri() else folderPicker.launch(null)

        locationManager = LocationManager(this, folderUri)
    }

    override fun onResume()
    {
        super.onResume()

        serviceEnabled = IsLocationServiceRunning()
        UpdateServiceButton()

        buttonGet.setOnClickListener {
            buttonGet.isEnabled = false
            info.text = ""
            buttonGet.text = "Подождите..."

            val callbackEnd: (LocationData) -> Unit = { locationData ->
                if (locationData.time != 0L)
                    LocationManager.Companion.SaveJsonFile("locations", locationData.toJSONObject(), folderUri, this)

                info.text = locationData.toString()
                buttonGet.text = "Получить данные"
                buttonGet.isEnabled = true
            }

            locationManager.GetLastLocation(callbackEnd)
        }
        buttonService.setOnClickListener { ToggleService() }
        buttonBack.setOnClickListener { finish() }

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
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == LocationService::class.java.name }
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