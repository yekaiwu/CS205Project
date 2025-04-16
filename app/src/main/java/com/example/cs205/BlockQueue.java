package com.example.cs205;

import android.util.Log;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

/**
 * Producer-Consumer queue for ProcessBlocks with fixed capacity
 */
public class BlockQueue {
    private static final String LOG_TAG = "BlockQueue";
    private static final int MAX_QUEUE_SIZE = 6;
    
    private final Queue<ProcessBlock> blockQueue = new LinkedList<>();
    private final Semaphore mutex = new Semaphore(1);
    private final Semaphore empty = new Semaphore(MAX_QUEUE_SIZE);
    private final Semaphore full = new Semaphore(0);
    private int overflowCount = 0; // Counter for blocks that couldn't be added
    
    /**
     * Add a block to the queue (producer)
     * @param block The block to add
     * @return true if block was added, false if queue is full
     */
    public boolean produce(ProcessBlock block) {
        try {
            // Check if there's space (non-blocking)
            if (!empty.tryAcquire()) {
                Log.d(LOG_TAG, "Queue full, cannot produce more blocks");
                overflowCount++; // Increment overflow counter
                Log.d(LOG_TAG, "Overflow count: " + overflowCount);
                return false;
            }
            
            // Get exclusive access to the queue
            mutex.acquire();
            
            try {
                blockQueue.add(block);
                Log.d(LOG_TAG, "Produced block ID: " + block.id + ", Queue size: " + blockQueue.size());
            } finally {
                mutex.release();
            }
            
            // Signal consumers there's a new item
            full.release();
            return true;
            
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Interrupted while producing block", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Take a block from the queue (consumer)
     * Non-blocking version - returns null if queue is empty
     */
    public ProcessBlock consumeNonBlocking() {
        if (!full.tryAcquire()) {
            return null; // No blocks available
        }
        
        try {
            mutex.acquire();
            
            try {
                ProcessBlock block = blockQueue.poll();
                Log.d(LOG_TAG, "Consumed block ID: " + (block != null ? block.id : "null") + 
                      ", Queue size: " + blockQueue.size());
                return block;
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Interrupted while consuming block", e);
            Thread.currentThread().interrupt();
            return null;
        } finally {
            empty.release(); // Signal producers there's a free slot
        }
    }
    
    /**
     * Get size of queue without modifying it (for display purposes)
     */
    public int getSize() {
        try {
            mutex.acquire();
            try {
                return blockQueue.size();
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Interrupted while getting queue size", e);
            Thread.currentThread().interrupt();
            return -1;
        }
    }
    
    /**
     * Get all blocks in the queue without removing them
     * Used for rendering
     */
    public ProcessBlock[] getQueuedBlocks() {
        try {
            mutex.acquire();
            try {
                return blockQueue.toArray(new ProcessBlock[0]);
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Interrupted while getting queued blocks", e);
            Thread.currentThread().interrupt();
            return new ProcessBlock[0];
        }
    }
    
    /**
     * Check if the queue is currently full
     */
    public boolean isFull() {
        return empty.availablePermits() == 0;
    }
    
    /**
     * Get the number of blocks that couldn't be added due to queue being full
     */
    public int getOverflowCount() {
        return overflowCount;
    }
    
    /**
     * Reset the overflow counter
     */
    public void resetOverflowCount() {
        try {
            mutex.acquire();
            try {
                overflowCount = 0;
            } finally {
                mutex.release();
            }
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, "Interrupted while resetting overflow count", e);
            Thread.currentThread().interrupt();
        }
    }
} 