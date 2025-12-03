package com.example.android_project.Services

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.example.android_project.Activites.MobileNetworkManager
import com.example.android_project.DataClasses.LocationData
import com.example.android_project.Network.ApiClient
import com.example.android_project.Network.WebSocketClient
import com.google.android.gms.location.*
import java.util.concurrent.Executors

class DataSenderService : Service(), WebSocketClient.ConnectionListener
{
    private val CHANNEL_ID = "data_sync_channel"
    private val NOTIFICATION_ID = 2

    private lateinit var webSocketClient: WebSocketClient
    private lateinit var apiClient: ApiClient
    private lateinit var mobileNetworkManager: MobileNetworkManager
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var serviceRunning = false
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    private val locationCallback = object : LocationCallback()
    {
        @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        override fun onLocationResult(locationResult: LocationResult)
        {
            locationResult.lastLocation?.let { location ->
                SendDataToServer(location.latitude, location.longitude, location.altitude)
            }
        }
    }

    override fun onCreate()
    {
        super.onCreate()

        apiClient = ApiClient(this)
        webSocketClient = WebSocketClient(this)
        webSocketClient.SetListener(this)
        mobileNetworkManager = MobileNetworkManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Передача данных",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Передача данных")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        if (intent?.action == "start")
            StartSend()
        else if (intent?.action == "stop")
            StopSend()
        else
            if(!serviceRunning)
                StartSend()

        return START_STICKY
    }

    override fun onDestroy()
    {
        StopSend()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun StartSend()
    {
        if (serviceRunning) return

        if (!apiClient.IsLoggedIn())
        {
            Toast.makeText(this, "Требуется авторизация", Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(this, "Требуются разрешения на геолокацию", Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        }

        serviceRunning = true
        webSocketClient.Connect()

        Toast.makeText(this, "webSocketClient Connected", Toast.LENGTH_SHORT).show()
    }

    private fun StopSend()
    {
        if (!serviceRunning) return

        serviceRunning = false
        scheduler.shutdown()
        webSocketClient.Disconnect()
        StopLocationUpdates()

        Toast.makeText(this, "webSocketClient Disconnected", Toast.LENGTH_SHORT).show()
    }

    private fun StartLocationUpdates()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
        { return }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 7000).build()

        fusedLocationClient.requestLocationUpdates(
            request,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun StopLocationUpdates()
    {
        try
        { fusedLocationClient.removeLocationUpdates(locationCallback) }
        catch (e: Exception) {}
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    private fun SendDataToServer(latitude: Double, longitude: Double, altitude: Double)
    {
        if (!serviceRunning || !webSocketClient.IsConnected()) return

        try
        {
            val locationData = LocationData(latitude, longitude, altitude, System.currentTimeMillis())
            val mobileNetworkDataList = mobileNetworkManager.GetMobileNetworkData()

            webSocketClient.SendData(mobileNetworkDataList, locationData)
        }
        catch (e: Exception)
        {
            e.printStackTrace()
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun OnConnected()
    {
        StartLocationUpdates()
    }

    override fun OnDisconnected()
    {
        StopLocationUpdates()
    }

    override fun OnError(message: String) {}

    override fun OnMessage(message: String) {}

    companion object
    {
        fun StartService(context: Context)
        {
            val intent = Intent(context, DataSenderService::class.java)
            intent.action = "start"
            context.startForegroundService(intent)
        }

        fun StopService(context: Context)
        {
            val intent = Intent(context, DataSenderService::class.java)
            intent.action = "stop"
            context.stopService(intent)
        }

        fun IsServiceRunning(context: Context): Boolean
        {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            return manager.getRunningServices(Integer.MAX_VALUE)
                .any { it.service.className == DataSenderService::class.java.name }
        }
    }
}