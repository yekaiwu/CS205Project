package com.example.cs205;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Custom View for the Block Puzzle Game.
 */
public class GameView extends View {

    // --- Constants ---
    private static final int GRID_WIDTH = 10; // Number of columns
    private static final int GRID_HEIGHT = 20; // Number of rows
    private static final int BLOCK_SPAWN_AREA_HEIGHT = 4; // Rows reserved for spawning new blocks below grid
    private static final long GAME_UPDATE_INTERVAL = 50; // Update game state every 50ms
    private static final long BLOCK_SPAWN_INTERVAL = 3000; // Spawn new block every 3s

    // --- Paints ---
    private Paint gridPaint;
    private Paint blockPaint;
    private Paint textPaint;
    private Paint progressPaint;
    private Paint starvingPaint; // For blocks waiting too long

    // --- Game State ---
    private int[][] grid; // Represents the CPU grid: 0 = empty, >0 = process ID + 1
    private List<ProcessBlock> activeProcesses; // Processes currently on the grid or waiting
    private ProcessBlock currentDraggingBlock = null; // Block being dragged by the user
    private Point dragOffset = new Point(); // Offset from touch point to block's top-left
    private int cellSize = 0; // Size of each grid cell in pixels
    private int gridOffsetX = 0; // Left offset for centering grid
    private int gridOffsetY = 0; // Top offset for grid

    // --- Timing ---
    private Timer gameTimer;
    private Handler handler = new Handler(Looper.getMainLooper());
    private long lastSpawnTime = 0;

    // --- Constructor ---
    public GameView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initPaints();
        initGame();
    }

    // --- Initialization ---
    private void initPaints() {
        gridPaint = new Paint();
        gridPaint.setColor(Color.DKGRAY);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2);

        blockPaint = new Paint();
        blockPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(20); // Adjust as needed
        textPaint.setTextAlign(Paint.Align.CENTER);

        progressPaint = new Paint();
        progressPaint.setColor(Color.argb(150, 255, 255, 255)); // Semi-transparent white
        progressPaint.setStyle(Paint.Style.FILL);

        starvingPaint = new Paint();
        starvingPaint.setColor(Color.RED); // Indicate starvation
        starvingPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        starvingPaint.setStrokeWidth(4);

    }

    private void initGame() {
        grid = new int[GRID_HEIGHT][GRID_WIDTH]; // Initialize empty grid
        activeProcesses = new ArrayList<>();
        lastSpawnTime = SystemClock.elapsedRealtime();
        spawnNewBlock(); // Spawn the first block

        // Start the game loop timer
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        gameTimer = new Timer();
        gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateGame();
                // Request redraw on the UI thread
                handler.post(() -> invalidate());
            }
        }, 0, GAME_UPDATE_INTERVAL);
    }

    // --- Game Logic ---
    private void updateGame() {
        long currentTime = SystemClock.elapsedRealtime();

        // Update timers for placed processes and check for completion
        List<ProcessBlock> finishedProcesses = new ArrayList<>();
        for (ProcessBlock process : activeProcesses) {
            if (process.isPlaced) {
                process.updateTimer();
                if (process.isFinished) {
                    finishedProcesses.add(process);
                }
            }
            // Check for starvation (visuals handled in onDraw)
        }

        // Remove finished processes from grid and active list
        if (!finishedProcesses.isEmpty()) {
            for (ProcessBlock finished : finishedProcesses) {
                removeFromGrid(finished); // Clears grid cells
                activeProcesses.remove(finished); // Removes from list
                Log.d("GameView", "Removed finished process ID: " + finished.id);
                // Add score, effects, etc. here
            }
        }


        // Check for line clears
        checkAndClearLines();

        // Spawn new blocks periodically
        if (currentTime - lastSpawnTime > BLOCK_SPAWN_INTERVAL) {
            // Only spawn if there's reasonable space in the waiting area (simple check)
            int waitingCount = 0;
            for(ProcessBlock p : activeProcesses) {
                if (!p.isPlaced) waitingCount++;
            }
            if (waitingCount < 5) { // Limit waiting blocks
                spawnNewBlock();
                lastSpawnTime = currentTime;
            }
        }
    }

    private void spawnNewBlock() {
        // Creates a new block and adds it to the active list.
        // Its visual position in the spawn area is determined during drawing.
        ProcessBlock newBlock = ProcessBlock.createRandomProcess();
        newBlock.position = new Point(-1, -1); // Mark as off-grid initially
        activeProcesses.add(newBlock);
        Log.d("GameView", "Spawned new block: ID " + newBlock.id);
    }

    // Check if a block can be placed at the target grid position
    private boolean canPlaceBlock(ProcessBlock block, int gridX, int gridY) {
        if (block == null) return false;

        for (int y = 0; y < block.getHeight(); y++) {
            for (int x = 0; x < block.getWidth(); x++) {
                if (block.shape[y][x] == 1) { // If this part of the shape exists
                    int checkX = gridX + x;
                    int checkY = gridY + y;

                    // Check grid boundaries
                    if (checkX < 0 || checkX >= GRID_WIDTH || checkY < 0 || checkY >= GRID_HEIGHT) {
                        return false; // Out of bounds
                    }

                    // Check for collision with existing blocks on the grid
                    // Make sure the cell is actually empty (value 0)
                    if (grid[checkY][checkX] != 0) {
                        return false; // Cell occupied
                    }
                }
            }
        }
        return true; // Placement is valid
    }

    // Place a block onto the grid data structure
    private void placeBlockOnGrid(ProcessBlock block, int gridX, int gridY) {
        // Pre-condition check moved to caller (onTouchEvent) for clarity
        // if (block == null || !canPlaceBlock(block, gridX, gridY)) return;

        block.position.set(gridX, gridY);
        block.isPlaced = true;

        // Update the grid data structure
        for (int y = 0; y < block.getHeight(); y++) {
            for (int x = 0; x < block.getWidth(); x++) {
                if (block.shape[y][x] == 1) {
                    // Make sure we don't write out of bounds (should be caught by canPlaceBlock, but safe)
                    if ((gridY + y < GRID_HEIGHT) && (gridX + x < GRID_WIDTH)) {
                        grid[gridY + y][gridX + x] = block.id + 1; // Store ID + 1 (0 means empty)
                    }
                }
            }
        }
        block.startTimer(); // Start the timer AFTER it's fully placed
        Log.d("GameView", "Placed block ID " + block.id + " at " + gridX + "," + gridY);
    }

    // Remove a block from the grid data structure ONLY
    // Does NOT remove from activeProcesses list
    private void removeFromGrid(ProcessBlock block) {
        if (block == null || !block.isPlaced) return;

        block.stopTimer(); // Stop timer when removed from grid

        int gridX = block.position.x;
        int gridY = block.position.y;

        // Clear the grid cells occupied by this block
        for (int y = 0; y < block.getHeight(); y++) {
            for (int x = 0; x < block.getWidth(); x++) {
                if (block.shape[y][x] == 1) {
                    int checkX = gridX + x;
                    int checkY = gridY + y;
                    // Check bounds before accessing grid array
                    if (checkX >= 0 && checkX < GRID_WIDTH && checkY >= 0 && checkY < GRID_HEIGHT) {
                        // Ensure we're clearing the correct block's ID from the grid
                        if (grid[checkY][checkX] == block.id + 1) {
                            grid[checkY][checkX] = 0; // Mark cell as empty
                        }
                    }
                }
            }
        }

        // Mark as not placed and reset grid position conceptually
        block.isPlaced = false;
        block.position.set(-1, -1);
        Log.d("GameView", "Removed block ID " + block.id + " data from grid.");
    }


    // Check for completed lines and clear them
    private void checkAndClearLines() {
        List<Integer> linesToClear = new ArrayList<>();
        // Check from bottom row up
        for (int y = GRID_HEIGHT - 1; y >= 0; y--) {
            boolean lineComplete = true;
            for (int x = 0; x < GRID_WIDTH; x++) {
                if (grid[y][x] == 0) { // If any cell in the row is empty
                    lineComplete = false;
                    break;
                }
            }
            if (lineComplete) {
                linesToClear.add(y);
            }
        }

        if (!linesToClear.isEmpty()) {
            Log.d("GameView", "Clearing lines: " + linesToClear.toString());

            List<ProcessBlock> newlyFinished = new ArrayList<>();

            // Identify processes in cleared lines and mark them finished
            for (int lineY : linesToClear) {
                for (int x = 0; x < GRID_WIDTH; x++) {
                    int processIdPlusOne = grid[lineY][x];
                    if (processIdPlusOne > 0) {
                        int processId = processIdPlusOne - 1;
                        ProcessBlock p = findProcessById(processId);
                        // Mark the process as finished if it exists and isn't already
                        if (p != null && !p.isFinished) {
                            p.isFinished = true; // Mark as finished due to line clear
                            p.stopTimer(); // Ensure timer is stopped
                            if (!newlyFinished.contains(p)) {
                                newlyFinished.add(p);
                            }
                            Log.d("GameView", "Process " + p.id + " part finished by line clear at [" + x + "," + lineY + "]");
                        }
                        grid[lineY][x] = 0; // Clear this cell immediately
                    }
                }
            }

            // Optional: Shift lines down (Tetris-like behavior)
            int linesClearedCount = linesToClear.size();
            int lowestClearedLine = linesToClear.get(linesToClear.size() - 1); // Get the lowest index (highest row number)

            // Shift down rows above the cleared lines
            // Start from the lowest cleared line index and move upwards
            int destRow = lowestClearedLine;
            for (int srcRow = lowestClearedLine - 1; srcRow >= 0; srcRow--) {
                // If the current destination row was cleared, skip it until we find a non-cleared source row
                while (destRow >= 0 && linesToClear.contains(destRow)) {
                    destRow--;
                }
                // If we ran out of destination rows, break
                if (destRow < 0) break;

                // If the source row was also cleared, skip it
                if (linesToClear.contains(srcRow)) {
                    continue;
                }

                // Copy the source row to the destination row
                for (int x = 0; x < GRID_WIDTH; x++) {
                    grid[destRow][x] = grid[srcRow][x];
                    // Update the position of the block that was moved (if any)
                    if (grid[destRow][x] != 0) {
                        ProcessBlock movedBlock = findProcessById(grid[destRow][x] - 1);
                        // This is tricky - a block spans multiple cells. We only need to update its main 'position' (top-left)
                        // if the top-most part of it moved. A simpler approach might be needed if exact tracking is vital.
                        // For now, we assume the block's internal position variable is still correct relative to its top-left.
                    }
                    grid[srcRow][x] = 0; // Clear the source cell after copying
                }
                destRow--; // Move to the next destination row up
            }

            // Fill the top rows (that were effectively cleared and shifted down from) with empty cells
            for (int y = 0; y < linesClearedCount; y++) {
                for (int x = 0; x < GRID_WIDTH; x++) {
                    // Check if the row index is valid (should be, but safety check)
                    if (y < GRID_HEIGHT) {
                        grid[y][x] = 0;
                    }
                }
            }


            // Remove the processes marked as finished from the active list
            if (!newlyFinished.isEmpty()) {
                Log.d("GameView", "Removing " + newlyFinished.size() + " processes finished by line clear.");
                activeProcesses.removeAll(newlyFinished);
            }
            // Add score based on linesClearedCount here
        }
    }


    // --- Drawing ---
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Calculate cell size and grid offset for centering
        int availableWidth = w;
        // Reserve space at the bottom for the spawn area
        int availableHeight = h - (int)(h * 0.2); // Reserve bottom 20% for spawn/info

        // Calculate cell size based on the main grid area
        int cellWidthBased = availableWidth / GRID_WIDTH;
        int cellHeightBased = availableHeight / GRID_HEIGHT;

        cellSize = Math.min(cellWidthBased, cellHeightBased);
        if (cellSize <= 0) cellSize = 1; // Prevent division by zero if view is tiny

        // Center the grid horizontally, place it near the top vertically
        int gridWidthPixels = cellSize * GRID_WIDTH;
        int gridHeightPixels = cellSize * GRID_HEIGHT;
        gridOffsetX = (w - gridWidthPixels) / 2;
        // gridOffsetY = (availableHeight - gridHeightPixels) / 2; // Center vertically in available space
        gridOffsetY = 20; // Place near top with a small margin

        Log.d("GameView", "onSizeChanged: w=" + w + ", h=" + h + ", cellSize=" + cellSize + ", gridOffset=("+gridOffsetX+","+gridOffsetY+")");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (canvas == null || cellSize <= 0) {
            Log.e("GameView", "Canvas is null or cellSize is invalid in onDraw.");
            return; // Skip drawing if not ready
        }


        canvas.drawColor(Color.BLACK); // Background

        // --- Draw Grid Lines ---
        for (int y = 0; y <= GRID_HEIGHT; y++) {
            float lineY = gridOffsetY + y * cellSize;
            canvas.drawLine(gridOffsetX, lineY, gridOffsetX + GRID_WIDTH * cellSize, lineY, gridPaint);
        }
        for (int x = 0; x <= GRID_WIDTH; x++) {
            float lineX = gridOffsetX + x * cellSize;
            canvas.drawLine(lineX, gridOffsetY, lineX, gridOffsetY + GRID_HEIGHT * cellSize, gridPaint);
        }

        // --- Draw Placed Blocks (using grid data) ---
        for (int y = 0; y < GRID_HEIGHT; y++) {
            for (int x = 0; x < GRID_WIDTH; x++) {
                int processIdPlusOne = grid[y][x];
                // Calculate cell position regardless of content
                int left = gridOffsetX + x * cellSize;
                int top = gridOffsetY + y * cellSize;

                if (processIdPlusOne != 0) { // If cell is occupied
                    int processId = processIdPlusOne - 1;
                    ProcessBlock block = findProcessById(processId);

                    // Check if this grid cell is the top-left corner of the block it belongs to
                    // This prevents overdrawing cells for multi-cell blocks.
                    // We only draw the full block starting from its 'position'.
                    boolean isTopLeftOfBlock = (block != null && block.position.x == x && block.position.y == y);

                    if (isTopLeftOfBlock) {
                        // Draw the entire block starting from this cell
                        drawBlockAtPixel(canvas, block, left, top, cellSize);
                    } else if (block == null) {
                        // Fallback: Draw a gray square if block ID exists but block object not found
                        Log.w("GameView", "Block object not found for ID " + processId + " at [" + x + "," + y + "]");
                        blockPaint.setColor(Color.GRAY);
                        canvas.drawRect(left, top, left + cellSize, top + cellSize, blockPaint);
                        canvas.drawRect(left, top, left + cellSize, top + cellSize, gridPaint); // Draw border
                    }
                    // If it's part of a block but not the top-left, drawBlockAtPixel handles it when called for the top-left.
                }
                // No need to explicitly draw empty cells, background shows through.
            }
        }

        // --- Draw Waiting/Spawning Blocks Area ---
        int spawnAreaTop = gridOffsetY + GRID_HEIGHT * cellSize + 30; // Position below the main grid
        int spawnAreaMargin = 20;
        int currentSpawnX = gridOffsetX + spawnAreaMargin; // Start position for waiting blocks
        int currentSpawnY = spawnAreaTop;
        int spawnAreaWidth = GRID_WIDTH * cellSize - (2 * spawnAreaMargin);
        int maxSpawnY = getHeight() - cellSize; // Limit drawing within view bounds

        // Draw a visual separator for the spawn area (optional)
        Paint spawnAreaPaint = new Paint();
        spawnAreaPaint.setColor(Color.argb(50, 100, 100, 100)); // Semi-transparent gray
        canvas.drawRect(gridOffsetX, spawnAreaTop - 10, gridOffsetX + GRID_WIDTH * cellSize, getHeight(), spawnAreaPaint);


        for (ProcessBlock block : activeProcesses) {
            // Only draw blocks that are NOT placed and NOT currently being dragged
            if (!block.isPlaced && block != currentDraggingBlock) {
                // Use a smaller cell size for waiting blocks if needed, or use main cellSize
                int waitingCellSize = cellSize > 20 ? cellSize * 3 / 4 : cellSize; // Slightly smaller
                int blockWidthPixels = block.getWidth() * waitingCellSize;
                int blockHeightPixels = block.getHeight() * waitingCellSize;

                // Simple horizontal layout with wrapping
                if (currentSpawnX + blockWidthPixels > gridOffsetX + spawnAreaWidth) {
                    currentSpawnX = gridOffsetX + spawnAreaMargin; // New row
                    currentSpawnY += waitingCellSize * 2 + 10; // Add vertical spacing (based on max height of blocks + gap)
                }

                // Check if block fits vertically
                if (currentSpawnY + blockHeightPixels > maxSpawnY) {
                    // Stop drawing waiting blocks if they go off-screen
                    // Optionally add a "..." indicator
                    break;
                }

                // Store the calculated draw position in the block temporarily (for touch detection)
                // This isn't ideal, better to have a separate layout structure.
                block.tempDrawX = currentSpawnX;
                block.tempDrawY = currentSpawnY;
                block.tempDrawCellSize = waitingCellSize;

                // Draw the waiting block
                drawBlockAtPixel(canvas, block, currentSpawnX, currentSpawnY, waitingCellSize);

                // Indicate starvation with a red border/overlay
                if (block.isStarving()) {
                    // Draw a red border around the block
                    Rect blockBounds = new Rect(currentSpawnX, currentSpawnY,
                            currentSpawnX + blockWidthPixels,
                            currentSpawnY + blockHeightPixels);
                    canvas.drawRect(blockBounds, starvingPaint); // Use the starvingPaint (red stroke)
                }

                currentSpawnX += blockWidthPixels + 15; // Add horizontal spacing
            }
        }


        // --- Draw Dragging Block (Last, so it's on top) ---
        if (currentDraggingBlock != null && lastTouchEvent != null) {
            // Calculate pixel position based on touch event less offset
            int currentX = (int) lastTouchEvent.getX() - dragOffset.x;
            int currentY = (int) lastTouchEvent.getY() - dragOffset.y;
            // Draw using the main grid's cellSize
            drawBlockAtPixel(canvas, currentDraggingBlock, currentX, currentY, cellSize);

            // Optional: Draw a ghost/preview on the grid where it would land
            int targetGridX = (currentX - gridOffsetX + cellSize / 2) / cellSize;
            int targetGridY = (currentY - gridOffsetY + cellSize / 2) / cellSize;
            drawPlacementPreview(canvas, currentDraggingBlock, targetGridX, targetGridY);

        }
    }

    // Helper to draw a single cell (part of a block)
    private void drawCell(Canvas canvas, int pixelX, int pixelY, int color, float progress, int currentCellSize) {
        blockPaint.setColor(color);
        // Draw the main cell color
        canvas.drawRect(pixelX, pixelY, pixelX + currentCellSize, pixelY + currentCellSize, blockPaint);

        // Draw progress overlay (darker shade) only if placed and running
        if (progress > 0 && progress < 1.0f) {
            // Make progress overlay slightly darker version of block color
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[2] *= 0.6f; // Reduce brightness
            progressPaint.setColor(Color.HSVToColor(hsv));

            int progressHeight = (int) (currentCellSize * (1.0f - progress));
            canvas.drawRect(pixelX, pixelY, pixelX + currentCellSize, pixelY + progressHeight, progressPaint);
        }

        // Draw border for the cell using gridPaint
        canvas.drawRect(pixelX, pixelY, pixelX + currentCellSize, pixelY + currentCellSize, gridPaint);
    }

    // Helper to draw a full block (composed of cells) at a specific pixel coordinate
    private void drawBlockAtPixel(Canvas canvas, ProcessBlock block, int pixelX, int pixelY, int currentCellSize) {
        if (block == null) return;

        float progress = block.isPlaced ? block.getProgress() : 0f; // Get progress only if placed

        for (int y = 0; y < block.getHeight(); y++) {
            for (int x = 0; x < block.getWidth(); x++) {
                if (block.shape[y][x] == 1) { // If this part of the shape exists
                    int cellPixelX = pixelX + x * currentCellSize;
                    int cellPixelY = pixelY + y * currentCellSize;
                    // Draw each cell of the block
                    drawCell(canvas, cellPixelX, cellPixelY, block.color, progress, currentCellSize);
                }
            }
        }
    }

    // Helper to draw a placement preview (ghost)
    private void drawPlacementPreview(Canvas canvas, ProcessBlock block, int gridX, int gridY) {
        if (block == null || cellSize <= 0) return;

        Paint previewPaint = new Paint();
        previewPaint.setStyle(Paint.Style.STROKE); // Outline
        previewPaint.setStrokeWidth(3);
        previewPaint.setAlpha(100); // Semi-transparent

        boolean canPlace = canPlaceBlock(block, gridX, gridY);
        previewPaint.setColor(canPlace ? Color.WHITE : Color.RED); // White if valid, Red if invalid

        for (int y = 0; y < block.getHeight(); y++) {
            for (int x = 0; x < block.getWidth(); x++) {
                if (block.shape[y][x] == 1) {
                    int cellPixelX = gridOffsetX + (gridX + x) * cellSize;
                    int cellPixelY = gridOffsetY + (gridY + y) * cellSize;
                    canvas.drawRect(cellPixelX, cellPixelY, cellPixelX + cellSize, cellPixelY + cellSize, previewPaint);
                }
            }
        }
    }


    // Find a process by its ID
    private ProcessBlock findProcessById(int id) {
        for (ProcessBlock p : activeProcesses) {
            if (p.id == id) {
                return p;
            }
        }
        return null; // Not found
    }

    // --- Touch Handling (Drag and Drop) ---
    private MotionEvent lastTouchEvent = null; // Store last event for drawing drag position

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (cellSize <= 0) return false; // Not initialized yet

        float touchX = event.getX();
        float touchY = event.getY();
        // Store raw event for drawing, but use a copy for state changes if needed later
        lastTouchEvent = MotionEvent.obtain(event);

        // Approximate grid coordinates for potential drop/lift check
        int touchedGridX = (int) ((touchX - gridOffsetX) / cellSize);
        int touchedGridY = (int) ((touchY - gridOffsetY) / cellSize);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 1. Check if touch is on a waiting block first
                currentDraggingBlock = findWaitingBlockAtTouch(touchX, touchY);

                if (currentDraggingBlock != null) {
                    // Calculate offset from top-left of the waiting block using stored draw position
                    dragOffset.set((int) touchX - currentDraggingBlock.tempDrawX,
                            (int) touchY - currentDraggingBlock.tempDrawY);
                    Log.d("DragDrop", "Picked up waiting block ID: " + currentDraggingBlock.id);
                    invalidate(); // Redraw to show dragging starting
                    return true; // Consume the event
                } else {
                    // 2. Check if touch is on a placed block on the grid
                    if (touchedGridX >= 0 && touchedGridX < GRID_WIDTH && touchedGridY >= 0 && touchedGridY < GRID_HEIGHT) {
                        int processIdPlusOne = grid[touchedGridY][touchedGridX];
                        if (processIdPlusOne > 0) { // If touched cell is occupied
                            ProcessBlock blockToLift = findProcessById(processIdPlusOne - 1);
                            // Ensure the block exists and is actually placed
                            if (blockToLift != null && blockToLift.isPlaced) {
                                currentDraggingBlock = blockToLift;
                                // Temporarily remove block data from the grid model for dragging
                                removeFromGrid(currentDraggingBlock); // Stops timer, clears grid cells, sets isPlaced=false

                                // Calculate offset based on the block's actual top-left corner pixel position
                                int blockPixelX = gridOffsetX + currentDraggingBlock.position.x * cellSize;
                                int blockPixelY = gridOffsetY + currentDraggingBlock.position.y * cellSize;
                                dragOffset.set((int) touchX - blockPixelX, (int) touchY - blockPixelY);

                                Log.d("DragDrop", "Lifted placed block ID: " + currentDraggingBlock.id + " from grid pos " + currentDraggingBlock.position.x + "," + currentDraggingBlock.position.y);
                                invalidate(); // Redraw to show dragging
                                return true; // Consume event
                            }
                        }
                    }
                }
                // If neither waiting nor placed block found, do nothing yet
                break;

            case MotionEvent.ACTION_MOVE:
                if (currentDraggingBlock != null) {
                    // Just redraw the view to show the block at the new touch position
                    invalidate();
                    return true; // Consume event while dragging
                }
                break;

            case MotionEvent.ACTION_UP:
                if (currentDraggingBlock != null) {
                    Log.d("DragDrop", "Dropped block ID: " + currentDraggingBlock.id);
                    // Calculate potential grid position based on the block's top-left corner at drop time
                    int dropPixelX = (int) touchX - dragOffset.x;
                    int dropPixelY = (int) touchY - dragOffset.y;
                    // Calculate target grid cell based on the center of the dragged block's top-left cell
                    int targetGridX = (dropPixelX - gridOffsetX + cellSize / 2) / cellSize;
                    int targetGridY = (dropPixelY - gridOffsetY + cellSize / 2) / cellSize;

                    // Check if placement is valid (within grid bounds and no collision)
                    if (canPlaceBlock(currentDraggingBlock, targetGridX, targetGridY)) {
                        // Valid placement - put it on the grid model
                        placeBlockOnGrid(currentDraggingBlock, targetGridX, targetGridY); // Sets isPlaced=true, starts timer
                        Log.d("DragDrop", "Placed block ID " + currentDraggingBlock.id + " successfully at " + targetGridX + "," + targetGridY);
                    } else {
                        // Invalid placement (collision or out of bounds)
                        Log.d("DragDrop", "Placement failed for block ID: " + currentDraggingBlock.id + " at target " + targetGridX + "," + targetGridY);
                        // Block was already removed from grid if lifted, just ensure it stays 'not placed'
                        currentDraggingBlock.isPlaced = false;
                        currentDraggingBlock.position.set(-1,-1); // Reset conceptual position
                        // The block will redraw in the waiting area in the next onDraw cycle
                    }

                    // Clear dragging state
                    currentDraggingBlock = null;
                    lastTouchEvent = null; // Clear stored event
                    invalidate(); // Redraw final state
                    return true; // Consume event
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                // Handle cancellation (e.g., finger moved off screen during drag)
                if (currentDraggingBlock != null) {
                    Log.d("DragDrop", "Drag cancelled for block ID: " + currentDraggingBlock.id);
                    // Treatเหมือน ACTION_UP with failed placement - return to waiting area
                    currentDraggingBlock.isPlaced = false;
                    currentDraggingBlock.position.set(-1,-1);
                    currentDraggingBlock = null;
                    lastTouchEvent = null;
                    invalidate();
                    return true;
                }
                break;
        }

        // Recycle the motion event object if we stored it
        if (lastTouchEvent != null && lastTouchEvent != event) {
            lastTouchEvent.recycle();
        }

        // Return false if the event wasn't handled by our drag/drop logic
        // This allows other system handling (like scrolling if inside a ScrollView, though not typical here)
        return false;
    }

    // Helper to find which waiting block is under the touch point
    // Uses the temporary draw coordinates stored during onDraw
    private ProcessBlock findWaitingBlockAtTouch(float touchX, float touchY) {
        // Iterate backwards as later drawn blocks are visually on top
        for (int i = activeProcesses.size() - 1; i >= 0; i--) {
            ProcessBlock block = activeProcesses.get(i);
            // Check only blocks that are not placed and have valid temp draw info
            if (!block.isPlaced && block.tempDrawX != -1 && block.tempDrawY != -1) {
                int blockWidthPixels = block.getWidth() * block.tempDrawCellSize;
                int blockHeightPixels = block.getHeight() * block.tempDrawCellSize;

                // Create bounds based on stored draw position and size
                Rect blockBounds = new Rect(block.tempDrawX, block.tempDrawY,
                        block.tempDrawX + blockWidthPixels,
                        block.tempDrawY + blockHeightPixels);

                // Check if the touch point is within these bounds
                if (blockBounds.contains((int) touchX, (int) touchY)) {
                    return block; // Found the block
                }
            }
        }
        return null; // No waiting block found at touch point
    }

    // --- Lifecycle ---
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Log.d("GameView", "onDetachedFromWindow - Cleaning up timer.");
        // Clean up timer when the view is removed to prevent leaks
        if (gameTimer != null) {
            gameTimer.cancel();
            gameTimer = null;
        }
        // Clean up handler messages
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        // Recycle last touch event if it exists
        if (lastTouchEvent != null) {
            lastTouchEvent.recycle();
            lastTouchEvent = null;
        }
    }
}