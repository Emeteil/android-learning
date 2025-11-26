package com.example.android_project.Activites

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.android_project.R

class MobileNetworkActivity : AppCompatActivity()
{
    private lateinit var mobileNetworkManager: MobileNetworkManager
    private lateinit var info: TextView
    private lateinit var buttonGet: Button
    private lateinit var buttonBack: Button

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mobile_network)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        mobileNetworkManager = MobileNetworkManager(this)
        info = findViewById(R.id.infoText)
        buttonGet = findViewById(R.id.buttonGet)
        buttonBack = findViewById(R.id.buttonBack)
    }

    override fun onResume()
    {
        super.onResume()

        buttonBack.setOnClickListener { finish() }
        buttonGet.setOnClickListener { UpdateUi() }
    }

    private fun UpdateUi()
    {
        if (
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        )
        {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_PHONE_STATE,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION),
                1
            )
        }

        val result = mobileNetworkManager.GetDetailedNetworkInfo()
        info.text = result
    }
}