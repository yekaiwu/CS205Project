package com.example.cs205;

import android.graphics.Point;
import android.os.Process;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker thread for managing grid state updates
 * Handles processing block timers and clearing lines in the background
 * Also acts as a consumer for the process blocks
 */
public class GridWorker {
    private static final String LOG_TAG = "GridWorker";
    private static final int UPDATE_INTERVAL_MS = 100; // Check every 100ms
    
    private final Game gameInstance;
    private final int gridWidth;
    private final int gridHeight;
    
    private Thread workerThread;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    
    public GridWorker(Game gameInstance, int gridWidth, int gridHeight) {
        this.gameInstance = gameInstance;
        this.gridWidth = gridWidth;
        this.gridHeight = gridHeight;
    }
    
    /**
     * Start the worker thread to manage grid state
     */
    public void startWorker() {
        if (isRunning.get()) {
            Log.w(LOG_TAG, "Worker thread already running, not starting new one");
            return;
        }
        
        isRunning.set(true);
        isPaused.set(false);
        
        workerThread = new Thread(() -> {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
            Log.d(LOG_TAG, "Grid worker thread started");
            
            try {
                while (isRunning.get()) {
                    if (!isPaused.get()) {
                        performGridOperations();
                    }
                    
                    try {
                        Thread.sleep(UPDATE_INTERVAL_MS);
                    } catch (InterruptedException e) {
                        if (!isRunning.get()) {
                            Log.d(LOG_TAG, "Worker thread interrupted and shutting down");
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(LOG_TAG, "Error in grid worker thread", e);
            }
            
            Log.d(LOG_TAG, "Grid worker thread stopped");
        });
        
        workerThread.setName("GridWorkerThread");
        workerThread.start();
    }
    
    /**
     * Stop the worker thread
     */
    public void stopWorker() {
        isRunning.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
            try {
                workerThread.join(1000); // Wait up to 1 second for clean shutdown
            } catch (InterruptedException e) {
                Log.w(LOG_TAG, "Interrupted while waiting for worker thread to stop", e);
            }
        }
    }
    
    /**
     * Pause the worker operations (but keep thread running)
     */
    public void pauseWorker() {
        isPaused.set(true);
        Log.d(LOG_TAG, "Grid worker paused");
    }
    
    /**
     * Resume worker operations
     */
    public void resumeWorker() {
        isPaused.set(false);
        Log.d(LOG_TAG, "Grid worker resumed");
    }
    
    /**
     * Perform all grid-related operations that need to be done in background
     */
    private void performGridOperations() {
        try {
            // 1. Update timers for placed blocks
            List<ProcessBlock> finishedBlocks = gameInstance.updatePlacedBlockTimers();
            
            // 2. Remove any blocks that have finished their execution time
            if (!finishedBlocks.isEmpty()) {
                gameInstance.removeFinishedBlocks(finishedBlocks);
            }
            
            // 3. Check for completed lines and clear them
            checkAndClearLines();
            
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error performing grid operations", e);
        }
    }
    
    /**
     * Check for completed lines in the grid and clear them
     * This method also acts as a consumer in the producer-consumer pattern
     * by clearing lines and freeing up space for new blocks
     */
    private void checkAndClearLines() {
        // Get a copy of the grid for thread-safe access
        int[][] grid = gameInstance.getGridState();
        List<Point> cellsToRemove = new ArrayList<>();
        
        // Check for horizontal lines
        for (int y = 0; y < gridHeight; y++) {
            boolean completeLine = true;
            for (int x = 0; x < gridWidth; x++) {
                if (grid[y][x] == 0) {
                    completeLine = false;
                    break;
                }
            }
            
            if (completeLine) {
                Log.d(LOG_TAG, "Found complete horizontal line at y=" + y);
                for (int x = 0; x < gridWidth; x++) {
                    cellsToRemove.add(new Point(x, y));
                }
            }
        }
        
        // Check for vertical lines
        for (int x = 0; x < gridWidth; x++) {
            boolean completeLine = true;
            for (int y = 0; y < gridHeight; y++) {
                if (grid[y][x] == 0) {
                    completeLine = false;
                    break;
                }
            }
            
            if (completeLine) {
                Log.d(LOG_TAG, "Found complete vertical line at x=" + x);
                for (int y = 0; y < gridHeight; y++) {
                    cellsToRemove.add(new Point(x, y));
                }
            }
        }
        
        // If we found any lines, clear them
        if (!cellsToRemove.isEmpty()) {
            gameInstance.clearCells(cellsToRemove);
            Log.d(LOG_TAG, "Consumed blocks by clearing " + cellsToRemove.size() + " cells");
        }
    }
} 