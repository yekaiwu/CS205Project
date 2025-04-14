package com.example.overcooked

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {
    private val gridSize = 8

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Back button
        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // Goes back to MainActivity
        }

        val grid = findViewById<GridLayout>(R.id.gridBoard)
        grid.removeAllViews()

        val cellCount = gridSize * gridSize

        grid.post {
            val cellSize = grid.width / gridSize
            for (i in 0 until cellCount) {
                val cell = View(this)
                val params = GridLayout.LayoutParams().apply {
                    width = cellSize
                    height = cellSize
                    setMargins(1, 1, 1, 1) // Thin margin = grid line
                }
                cell.setBackgroundColor(Color.parseColor("#2C3E50")) // dark cell
                cell.layoutParams = params
                grid.addView(cell)
            }
        }
    }
}