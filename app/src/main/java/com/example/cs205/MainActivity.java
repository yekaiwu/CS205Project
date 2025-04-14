package com.example.cs205;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * A class representing the main activity which contains a button to start the game.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the start game button
        final Button startButton = findViewById(R.id.start_button);
        
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