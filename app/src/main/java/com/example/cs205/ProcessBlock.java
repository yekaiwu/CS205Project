package com.example.cs205;
import android.graphics.Color;
import android.graphics.Point;
import android.os.SystemClock;
import android.util.Log;

import java.util.Random;

class ProcessBlock {
    // --- Fields ---
    int id; // Unique ID for the process
    int[][] shape; // 2D array defining the block's shape (e.g., {{1,1}, {1,1}} for a square)
    int color; // Color of the block
    Point position; // Top-left position on the grid (grid coordinates, not pixels)
    long startTimeMillis = -1; // Time when placed on the grid, -1 if not placed
    long timeLimitMillis; // How long this process needs to run
    long timeElapsedMillis = 0; // How long it has run so far
    boolean isPlaced = false; // Is the block currently on the CPU grid?
    boolean isFinished = false; // Has the process completed execution?
    long creationTimeMillis; // When the block was created (for starvation)
    long maxWaitTimeMillis = 30000; // Max time to wait before becoming "impatient" (30s)

    private static int nextId = 0;
    private static final Random random = new Random();

    transient int tempDrawX = -1;
    transient int tempDrawY = -1;
    transient int tempDrawCellSize = -1;

    // --- Constructor ---
    public ProcessBlock(int[][] shape, int color, long timeLimitMillis) {
        this.id = nextId++;
        this.shape = shape;
        this.color = color;
        this.position = new Point(-1, -1); // Initially off-grid
        this.timeLimitMillis = timeLimitMillis;
        this.creationTimeMillis = SystemClock.elapsedRealtime();
    }

    // --- Methods ---
    public int getWidth() {
        // Basic validation in case shape is unexpectedly empty
        if (shape == null || shape.length == 0 || shape[0] == null) return 0;
        return shape[0].length;
    }

    public int getHeight() {
        // Basic validation
        if (shape == null) return 0;
        return shape.length;
    }

    // Starts the process timer
    public void startTimer() {
        if (isPlaced && startTimeMillis == -1) {
            startTimeMillis = SystemClock.elapsedRealtime();
            Log.d("ProcessBlock", "Process " + id + " timer started.");
        }
    }

    // Stops the process timer and updates elapsed time
    public void stopTimer() {
        if (isPlaced && startTimeMillis != -1) {
            timeElapsedMillis += (SystemClock.elapsedRealtime() - startTimeMillis);
            startTimeMillis = -1; // Reset start time as it's paused
            Log.d("ProcessBlock", "Process " + id + " timer stopped. Elapsed: " + timeElapsedMillis);
        }
    }

    // Updates the timer if currently running
    public void updateTimer() {
        if (isPlaced && startTimeMillis != -1) {
            long currentTime = SystemClock.elapsedRealtime();
            long currentRunTime = timeElapsedMillis + (currentTime - startTimeMillis);
            if (currentRunTime >= timeLimitMillis) {
                isFinished = true;
                timeElapsedMillis = timeLimitMillis; // Cap elapsed time
                startTimeMillis = -1; // Stop timer
                Log.d("ProcessBlock", "Process " + id + " finished execution.");
            }
        }
    }

    // Check if the block is starving (waiting too long)
    public boolean isStarving() {
        return !isPlaced && (SystemClock.elapsedRealtime() - creationTimeMillis) > maxWaitTimeMillis;
    }

    // Get remaining time percentage (0.0 to 1.0)
    public float getProgress() {
        if (timeLimitMillis <= 0) return 0f;
        long currentRunTime = timeElapsedMillis;
        if (isPlaced && startTimeMillis != -1) {
            currentRunTime += (SystemClock.elapsedRealtime() - startTimeMillis);
        }
        return Math.min(1.0f, (float) currentRunTime / timeLimitMillis);
    }

    // --- Static Factory for creating random blocks ---
    public static ProcessBlock createRandomProcess() {
        int type = random.nextInt(5); // Example: 5 types of blocks
        int color;
        int[][] shape;
        long timeLimit = (random.nextInt(10) + 5) * 1000; // 5-14 seconds runtime

        switch (type) {
            case 0: // I shape
                shape = new int[][]{{1, 1, 1, 1}};
                color = Color.CYAN;
                break;
            case 1: // O shape
                shape = new int[][]{{1, 1}, {1, 1}};
                color = Color.YELLOW;
                break;
            case 2: // T shape
                shape = new int[][]{{1, 1, 1}, {0, 1, 0}};
                color = Color.MAGENTA;
                break;
            case 3: // L shape
                shape = new int[][]{{1, 0}, {1, 0}, {1, 1}};
                color = Color.rgb(255, 165, 0); // Orange
                break;
            case 4: // S shape
            default:
                shape = new int[][]{{0, 1, 1}, {1, 1, 0}};
                color = Color.GREEN;
                break;
        }
        return new ProcessBlock(shape, color, timeLimit);
    }
}