package com.example.cs205;

import android.os.SystemClock;
import android.util.Log;

/**
 * A class representing the game loop.
 */
public class GameThread extends Thread {
    private static final String LOG_TAG = GameThread.class.getSimpleName();
    private volatile boolean isRunning = false;
    
    private final Game game;
    private long lastFrameTime;

    public GameThread(final Game game) {
        super("GameThread");
        this.game = game;
    }

    public void startLoop() {
        isRunning = true;
        lastFrameTime = SystemClock.elapsedRealtime();
        start();
    }

    public void stopLoop() {
        isRunning = false;
        
        // Interrupt if sleeping
        interrupt();
    }

    @Override
    public void run() {
        super.run();
        Log.d(LOG_TAG, "GameThread started");
        
        try {
            while (isRunning) {
                try {
                    // Get the current time
                    long currentTime = SystemClock.elapsedRealtime();
                    
                    // Draw the game state
                    game.draw();
                    
                    // Calculate how long it took to draw
                    long drawTime = SystemClock.elapsedRealtime() - currentTime;
                    
                    // Calculate sleep time based on target frame rate
                    long sleepTime = game.getSleepTime() - drawTime;
                    if (sleepTime > 0) {
                        sleep(sleepTime);
                    }
                    
                    // Update game state
                    game.update();
                    
                    // Update frame time
                    lastFrameTime = currentTime;
                } catch (InterruptedException e) {
                    // Thread was interrupted, check if we should exit
                    if (!isRunning) break;
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Error in game loop: " + e.getMessage(), e);
                    // Sleep a bit to avoid rapid failure loops
                    try {
                        sleep(100);
                    } catch (InterruptedException ie) {
                        if (!isRunning) break;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Fatal error in game thread: " + e.getMessage(), e);
        }
        Log.d(LOG_TAG, "GameThread stopped");
    }
} 