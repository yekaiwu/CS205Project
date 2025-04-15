package com.example.cs205;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import android.database.sqlite.SQLiteDatabase;
/**
 * A class representing the game activity.
 */
public class GameActivity extends AppCompatActivity implements Timer.TimerListener {

    private GameView gameView;
    private TextView timerTextView;
    private Timer timer;
    private HighScoreDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide the action bar for a more immersive experience
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        
        setContentView(R.layout.activity_game);
        
        gameView = findViewById(R.id.gameView); // get game view

        timerTextView = findViewById(R.id.timerTextView); // get timer text view
        timer = new Timer(20000, 1000, this); // 1 minute timer with 1 second interval
        dbHelper = new HighScoreDatabaseHelper(this);
        
        // Find and configure reset overflow button
        Button resetButton = findViewById(R.id.reset_overflow_button);
        if (resetButton != null) {
            resetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (gameView != null && gameView.getGame() != null) {
                        gameView.getGame().resetOverflowCounter();
                    }
                }
            });
        }

        if (!timer.isRunning()) {
            Log.d("GameActivity", "Timer started");
            timer.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null) {
            gameView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (gameView != null) {
            gameView.resume();
        }
    }

    @Override
    public void onTick(long millisUntilFinished) {
        long minutes = (millisUntilFinished / 1000) / 60;
        long seconds = (millisUntilFinished / 1000) % 60;
        String timeFormatted = String.format("%02d:%02d", minutes, seconds);
        timerTextView.setText("Time left: " + timeFormatted);
    }

    @Override
    public void onFinish() {
        Log.d("GameActivity", "Timer finished");
        timerTextView.setText("Time's up!");
        int score = gameView.endGame();
        saveHighestCounter(score);
        // push notification
        NotificationPublisher.showNotification(this);
        // should display score stats page, now it just go back to main activity page
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void saveHighestCounter(int score) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        dbHelper.saveHighestScore(db, score);
    }
} 