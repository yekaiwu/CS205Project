package com.example.cs205;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainMenuActivity extends AppCompatActivity {

    private ImageView trophyImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_menu);

        // Animate the trophy
        trophyImage = findViewById(R.id.trophyImage);
        Animation glow = AnimationUtils.loadAnimation(this, R.anim.glow);
        trophyImage.startAnimation(glow);

        // Start Game button
        Button startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, MainActivity.class);
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

        // Button listener
        Button okButton = dialogView.findViewById(R.id.okButton);
        okButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}