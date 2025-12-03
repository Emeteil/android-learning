package com.example.android_project

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.android_project.Activites.CalculatorActivity
import com.example.android_project.Activites.DataSenderActivity
import com.example.android_project.Activites.LocationActivity
import com.example.android_project.Activites.MediaPlayerActivity
import com.example.android_project.Activites.MobileNetworkActivity

class MainActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        findViewById<CardView>(R.id.calculatorCard).setOnClickListener({
            startActivity(Intent(this, CalculatorActivity::class.java))
        })
        findViewById<CardView>(R.id.mediaPlayerCard).setOnClickListener({
            startActivity(Intent(this, MediaPlayerActivity::class.java))
        })
        findViewById<CardView>(R.id.locationCard).setOnClickListener({
            startActivity(Intent(this, LocationActivity::class.java))
        })
        findViewById<CardView>(R.id.mobileNetworkCard).setOnClickListener({
            startActivity(Intent(this, MobileNetworkActivity::class.java))
        })
        findViewById<CardView>(R.id.dataSenderCard).setOnClickListener({
            startActivity(Intent(this, DataSenderActivity::class.java))
        })
    }
}