package com.example.cs205;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import android.database.sqlite.SQLiteDatabase;
import android.widget.TextView;
import android.content.pm.PackageManager;
import android.os.Build;
import android.Manifest;
/**
 * A class representing the main activity which contains a button to start the game.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // display highest score saved in local db
        HighScoreDatabaseHelper dbHelper = new HighScoreDatabaseHelper(this);
        TextView highScoreTextView = findViewById(R.id.highScoreTextView);

        // clear db for testing, comment out when not needed
        // run the app once w this code, then stop the app and comment out this code then run again
//         SQLiteDatabase dbTest = dbHelper.getWritableDatabase();
//         dbHelper.clearScores(dbTest);

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int highScore = dbHelper.getHighestScore(db);
        highScoreTextView.setText("High Score: " + highScore);

        // Find the start game button
        final Button startButton = findViewById(R.id.start_button);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
        
        // Set a listener to start the game on click
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startGame();
            }
        });
    }

    /**
     * Start the game activity.
     */
    private void startGame() {
        final Intent intent = new Intent(this, GameActivity.class);
        startActivity(intent);
    }
}