package com.example.overcooked

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.content.Intent

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnHowToPlay = findViewById<Button>(R.id.btnHowToPlay)

        btnStart.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }

        btnHowToPlay.setOnClickListener {
            // TODO: Replace this with intent to HowToPlayActivity
            Toast.makeText(this, "How to Play clicked!", Toast.LENGTH_SHORT).show()
        }
    }
}