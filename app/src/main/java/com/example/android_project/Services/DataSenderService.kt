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

    private var dataSentCount = 0
    private var errorCount = 0
    private var currentStatus = "Инициализация"
    private var lastSendTime = System.currentTimeMillis()
    private var reconnectHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var reconnectRunnable: Runnable? = null

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
            .setContentTitle("Статус: $currentStatus")
            .setContentText("Отправлено: $dataSentCount | Ошибок: $errorCount")
            .setSmallIcon(com.example.android_project.R.mipmap.ic_launcher)
            .setWhen(lastSendTime)
            .setUsesChronometer(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun UpdateNotification()
    {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Статус: $currentStatus")
            .setContentText("Отправлено: $dataSentCount | Ошибок: $errorCount")
            .setSmallIcon(com.example.android_project.R.mipmap.ic_launcher)
            .setWhen(lastSendTime)
            .setUsesChronometer(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun Reconnect()
    {
        currentStatus = "Ожидание сети..."
        UpdateNotification()
        
        if (!serviceRunning) return
        reconnectRunnable?.let { reconnectHandler.removeCallbacks(it) }
        reconnectRunnable = Runnable {
            if (serviceRunning && !webSocketClient.IsConnected())
            {
                currentStatus = "Переподключение..."
                UpdateNotification()
                webSocketClient.Connect()
            }
        }
        reconnectHandler.postDelayed(reconnectRunnable!!, 5000)
    }

    private fun ShowAuthErrorNotification()
    {
        val channelId = "auth_error_channel"
        val channel = NotificationChannel(
            channelId,
            "Ошибка авторизации",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val intent = Intent(this, com.example.android_project.Activites.DataSenderActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Ошибка авторизации")
            .setContentText("Токен истек, войдите заново")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(3, notification)
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
        currentStatus = "Подключение..."
        lastSendTime = System.currentTimeMillis()
        UpdateNotification()
        webSocketClient.Connect()

        Toast.makeText(this, "webSocketClient Connected", Toast.LENGTH_SHORT).show()
    }

    private fun StopSend()
    {
        if (!serviceRunning) return

        serviceRunning = false
        scheduler.shutdown()
        reconnectRunnable?.let { reconnectHandler.removeCallbacks(it) }
        webSocketClient.Disconnect()
        StopLocationUpdates()

        Toast.makeText(this, "webSocketClient Disconnected", Toast.LENGTH_SHORT).show()
    }

    private fun StartLocationUpdates()
    {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)
        { return }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1500).build()

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

            val success = webSocketClient.SendData(mobileNetworkDataList, locationData)
            if (success)
            {
                dataSentCount++
                currentStatus = "Отправка данных..."
                lastSendTime = System.currentTimeMillis()
                UpdateNotification()
            }
            else
            {
                errorCount++
                currentStatus = "Ошибка отправки"
                UpdateNotification()
                Reconnect()
            }
        }
        catch (e: Exception)
        {
            errorCount++
            currentStatus = "Исключение при отправке"
            UpdateNotification()
            Reconnect()
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    override fun OnConnected()
    {
        currentStatus = "Подключено"
        lastSendTime = System.currentTimeMillis()
        UpdateNotification()
        StartLocationUpdates()
    }

    override fun OnDisconnected()
    {
        StopLocationUpdates()
        errorCount++
        currentStatus = "Отключено от сервера"
        UpdateNotification()
        Reconnect()
    }

    override fun OnError(message: String)
    {
        if (message == "token_expired")
        {
            ShowAuthErrorNotification()
            StopSend()
            apiClient.ClearAuthData()
        }
        else if (message == "config_error")
        {
            StopSend()
        }
        else
        {
            errorCount++
            currentStatus = "Ошибка сети"
            UpdateNotification()
            Reconnect()
        }
    }

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