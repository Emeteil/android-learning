package com.example.android_project

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity()
{
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        findViewById<CardView>(R.id.calculatorCard).setOnClickListener({
            startActivity(Intent(this, CalculatorActivity::class.java))
        })
        findViewById<CardView>(R.id.mediaPlayerCard).setOnClickListener({ })
    }
}