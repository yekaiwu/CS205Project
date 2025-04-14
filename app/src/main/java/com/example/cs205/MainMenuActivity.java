package com.example.cs205;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);  // This should point to your main menu layout

        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch the game activity (MainActivity) when the start button is clicked
                Intent intent = new Intent(MainMenuActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
        
        Button howToPlayButton = findViewById(R.id.howToPlayButton);
        howToPlayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHowToPlayDialog();
            }
        });
    }
    
    private void showHowToPlayDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("How To Play")
                .setMessage("1. Drag and drop process blocks onto the CPU grid\n\n" +
                        "2. Each block takes time to process - watch the progress!\n\n" +
                        "3. Fill entire rows to complete them and earn points\n\n" +
                        "4. Don't let your waiting processes starve (turn red)\n\n" +
                        "5. Keep managing processes efficiently as long as possible!")
                .setPositiveButton("Got it!", null)
                .show();
    }
}