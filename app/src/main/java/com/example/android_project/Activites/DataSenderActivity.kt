package com.example.android_project.Activites

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.android_project.Network.ApiClient
import com.example.android_project.R
import com.example.android_project.Services.DataSenderService

class DataSenderActivity : AppCompatActivity()
{
    private lateinit var infoText: TextView
    private lateinit var editTextServer: EditText
    private lateinit var editTextNickname: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonAuth: Button
    private lateinit var buttonSync: Button
    private lateinit var buttonBack: Button

    private lateinit var apiClient: ApiClient
    private var serviceRunning: Boolean = false
        get() { return DataSenderService.IsServiceRunning(this) }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_data_sender)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        infoText = findViewById(R.id.infoText)
        editTextServer = findViewById(R.id.editTextServer)
        editTextNickname = findViewById(R.id.editTextNickname)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonAuth = findViewById(R.id.buttonAuth)
        buttonSync = findViewById(R.id.buttonSync)
        buttonBack = findViewById(R.id.buttonBack)

        apiClient = ApiClient(this)

        LoadSavedData()
        UpdateUI()
    }

    override fun onResume()
    {
        super.onResume()

        UpdateSyncButton()

        buttonAuth.setOnClickListener { HandleAuth() }
        buttonSync.setOnClickListener { ToggleSync() }
        buttonBack.setOnClickListener { finish() }

        buttonAuth.isEnabled = true
        buttonSync.isEnabled = apiClient.IsLoggedIn()
    }

    private fun LoadSavedData()
    {
        val serverAddress = apiClient.GetServerAddress()
        if (serverAddress != null)
            editTextServer.setText(serverAddress)

        val nickname = apiClient.GetNickname()
        if (nickname != null)
            editTextNickname.setText(nickname)
    }

    private fun UpdateUI()
    {
        if (apiClient.IsLoggedIn())
        {
            editTextNickname.isEnabled = false
            editTextPassword.isEnabled = false
            buttonAuth.text = "Выйти"
            infoText.text = "Авторизован: ${apiClient.GetNickname()}"
        }
        else
        {
            editTextNickname.isEnabled = true
            editTextPassword.isEnabled = true
            buttonAuth.text = "Войти/Зарегистрироваться"
            infoText.text = "Не авторизован"
        }
        UpdateSyncButton()
    }

    private fun UpdateSyncButton()
    {
        buttonSync.text = if (serviceRunning) "Отключить передачу данных" else "Включить передачу данных"
    }

    private fun HandleAuth()
    {
        if (apiClient.IsLoggedIn())
        {
            apiClient.ClearAuthData()
            UpdateUI()
            buttonSync.isEnabled = false

            if (DataSenderService.IsServiceRunning(this))
            {
                DataSenderService.StopService(this)
                UpdateSyncButton()
            }

            Toast.makeText(this, "Вы вышли", Toast.LENGTH_SHORT).show()
            return
        }

        val server = editTextServer.text.toString().trim()
        val nickname = editTextNickname.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        if (server.isEmpty() || nickname.isEmpty() || password.isEmpty())
        {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show()
            return
        }

        apiClient.SaveServerAddress(server)

        buttonAuth.isEnabled = false
        buttonAuth.text = "Обработка..."
        infoText.text = "Попытка авторизации..."

        apiClient.Register(nickname, password, { success, message ->
            runOnUiThread {
                if (success)
                {
                    UpdateUI()
                    buttonSync.isEnabled = true
                    infoText.text = message
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
                else
                {
                    infoText.text = message
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }

                buttonAuth.isEnabled = true
                buttonAuth.text = if (apiClient.IsLoggedIn()) "Выйти" else "Войти/Зарегистрироваться"
            }
        })
    }

    private fun ToggleSync()
    {
        if (serviceRunning)
        {
            DataSenderService.StopService(this)
        }
        else
        {
            if (!apiClient.IsLoggedIn())
            {
                Toast.makeText(this, "Требуется авторизация", Toast.LENGTH_SHORT).show()
                return
            }

            apiClient.CheckLogin({ success, message ->
                runOnUiThread {
                    if (success)
                    {
                        DataSenderService.StartService(this)
                        infoText.text = "Отправка данных запущена"
                    }
                    else
                    {
                        apiClient.ClearAuthData()
                        UpdateUI()
                        buttonSync.isEnabled = false
                        infoText.text = message
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                    UpdateSyncButton()
                }
            })
        }

        UpdateSyncButton()
    }
}