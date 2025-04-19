package com.example.cs205;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
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

        // Keep the screen always on during gameplay.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        gameView = findViewById(R.id.gameView); // get game view

        timerTextView = findViewById(R.id.timerTextView); // get timer text view
        timer = new Timer(120000, 1000, this); // 1 minute timer with 1 second interval
        dbHelper = new HighScoreDatabaseHelper(this);
        
        // Find and configure reset overflow button
//        Button resetButton = findViewById(R.id.reset_overflow_button);
//        if (resetButton != null) {
//            resetButton.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (gameView != null && gameView.getGame() != null) {
//                        gameView.getGame().resetOverflowCounter();
//                    }
//                }
//            });
//        }

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

// In GameActivity.java

    @Override
    public void onFinish() {
        Log.d("GameActivity", "Timer finished");
        timerTextView.setText("Time's up!");

        int currentScore = gameView.endGame();
        
        // Get the statistics from the game
        Game game = gameView.getGame();
        int processesCleared = game.getProcessesCleared();
        int processesStarved = game.getProcessesStarved();
        
        saveHighestCounter(currentScore);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int highestScore = dbHelper.getHighestScore(db);

        // Inflate custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_how_to_play, null);

        TextView titleText = dialogView.findViewById(R.id.dialogTitle);
        TextView messageText = dialogView.findViewById(R.id.dialogMessage);
        Button okButton = dialogView.findViewById(R.id.okButton);

        titleText.setText("Game Over!");
        messageText.setText(
            "Your score: " + currentScore + 
            "\nHighest score: " + highestScore +
            "\n\nStatistics:" +
            "\nProcesses cleared: " + processesCleared + 
            "\nProcesses starved: " + processesStarved
        );

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        okButton.setOnClickListener(v -> {
            dialog.dismiss();
            Intent intent = new Intent(GameActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        dialog.show();

        NotificationPublisher.showNotification(this);
    }


    private void saveHighestCounter(int score) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        dbHelper.saveHighestScore(db, score);
    }
} 