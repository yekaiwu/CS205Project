package com.example.cs205;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.database.sqlite.SQLiteDatabase;
import android.content.pm.PackageManager;
import android.Manifest;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * MainActivity shows the main menu screen and displays the high score.
 * Handles animation, SQLite logic, and navigation to the game and help dialog.
 */
public class MainActivity extends AppCompatActivity {

    private ImageView trophyImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);

        // Animate the trophy
        trophyImage = findViewById(R.id.trophyImage);
        Animation glow = AnimationUtils.loadAnimation(this, R.anim.glow);
        trophyImage.startAnimation(glow);

        // Handle notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        // Show high score from local SQLite database
        HighScoreDatabaseHelper dbHelper = new HighScoreDatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int highScore = dbHelper.getHighestScore(db);
        TextView highScoreTextView = findViewById(R.id.highScoreTextView);
        highScoreTextView.setText("High Score: " + highScore);

        // Start Game button
        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, GameActivity.class);
            startActivity(intent);
        });

        // How To Play button
        Button howToPlayButton = findViewById(R.id.howToPlayButton);
        howToPlayButton.setOnClickListener(v -> showHowToPlayDialog());
    }

    private void showHowToPlayDialog() {
        // Inflate the custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_how_to_play, null);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // OK button listener to dismiss
        Button okButton = dialogView.findViewById(R.id.okButton);
        okButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}