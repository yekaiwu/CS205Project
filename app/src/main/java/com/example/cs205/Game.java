package com.example.cs205;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * A class representing the main logic of the game.
 */
public class Game {
    // --- Constants ---
    private final static int GRID_WIDTH = 10; // Number of columns
    private final static int GRID_HEIGHT = 10; // Number of rows
    private final static int BLOCK_SPAWN_AREA_HEIGHT = 4; // Rows reserved for spawning new blocks 
    private final static int targetFps = 30;
    private final static long BLOCK_SPAWN_INTERVAL = 3000; // Spawn new block every 3s

    private final String LOG_TAG = Game.class.getSimpleName();
    private final Object mutex = new Object();
    private final Predicate<Consumer<Canvas>> useCanvas;
    private final Runnable runnable;

    // --- Paints ---
    private final Paint gridPaint = new Paint();
    private final Paint blockPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint progressPaint = new Paint();
    private final Paint starvingPaint = new Paint(); // For blocks waiting too long

    // --- Game State ---
    private int[][] grid = new int[GRID_HEIGHT][GRID_WIDTH]; // Represents the CPU grid: 0 = empty, >0 = process ID + 1
    private List<ProcessBlock> activeProcesses = new ArrayList<>();
    private ProcessBlock currentDraggingBlock = null; // Block being dragged by the user
    private Point dragOffset = new Point(); // Offset from touch point to block's top-left
    
    private int width = 0;
    private int height = 0;
    private int cellSize = 0; // Size of each grid cell in pixels
    private int gridOffsetX = 0; // Left offset for centering grid
    private int gridOffsetY = 0; // Top offset for grid
    
    private long lastSpawnTime = 0;
    private long lastUpdateTime = 0;

    public Game(final Runnable runnable, final Predicate<Consumer<Canvas>> useCanvas) {
        this.runnable = runnable;
        this.useCanvas = useCanvas;
        
        initPaints();
        initGame();
    }
    
    // --- Initialization ---
    private void initPaints() {
        gridPaint.setColor(Color.DKGRAY);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(2);

        blockPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(20);
        textPaint.setTextAlign(Paint.Align.CENTER);

        progressPaint.setColor(Color.argb(150, 255, 255, 255)); 
        progressPaint.setStyle(Paint.Style.FILL);

        starvingPaint.setColor(Color.RED);
        starvingPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        starvingPaint.setStrokeWidth(4);
    }

    private void initGame() {
        // Initialize with empty grid already done in field init
        lastSpawnTime = SystemClock.elapsedRealtime();
        spawnNewBlock(); // Spawn the first block
    }
    
    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        
        // Calculate cell size and grid offset based on available space
        cellSize = Math.min(width / GRID_WIDTH, height / (GRID_HEIGHT + BLOCK_SPAWN_AREA_HEIGHT));
        gridOffsetX = (width - (GRID_WIDTH * cellSize)) / 2;
        gridOffsetY = 20; // Small top margin
    }
    
    public void draw() {
        // Pass drawing method to the GameView's useCanvas method
        try {
            boolean success = useCanvas.test(this::draw);
            if (!success) {
                Log.w(LOG_TAG, "Failed to draw - canvas operation returned false");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error during draw operation: " + e.getMessage(), e);
        }
    }
    
    private void draw(Canvas canvas) {
        if (canvas == null) {
            return;
        }
        
        // Clear the canvas
        canvas.drawColor(Color.BLACK);
        
        // Draw the grid
        drawGrid(canvas);
        
        // Draw placed blocks on the grid
        drawPlacedBlocks(canvas);
        
        // Draw waiting blocks in the spawn area
        drawWaitingBlocks(canvas);
        
        // Draw the currently dragging block, if any
        if (currentDraggingBlock != null) {
            // Ensure we're always drawing with the most current values
            drawDraggingBlock(canvas);
        }
    }
    
    private void drawGrid(Canvas canvas) {
        // Draw grid background
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.rgb(20, 20, 50));
        canvas.drawRect(
            gridOffsetX, 
            gridOffsetY, 
            gridOffsetX + GRID_WIDTH * cellSize, 
            gridOffsetY + GRID_HEIGHT * cellSize, 
            bgPaint
        );
        
        // Draw grid lines
        for (int i = 0; i <= GRID_WIDTH; i++) {
            canvas.drawLine(
                gridOffsetX + i * cellSize, 
                gridOffsetY, 
                gridOffsetX + i * cellSize, 
                gridOffsetY + GRID_HEIGHT * cellSize, 
                gridPaint
            );
        }
        
        for (int i = 0; i <= GRID_HEIGHT; i++) {
            canvas.drawLine(
                gridOffsetX, 
                gridOffsetY + i * cellSize, 
                gridOffsetX + GRID_WIDTH * cellSize, 
                gridOffsetY + i * cellSize, 
                gridPaint
            );
        }
    }
    
    private void drawPlacedBlocks(Canvas canvas) {
        synchronized (mutex) {
            for (ProcessBlock block : activeProcesses) {
                if (block.isPlaced && block != currentDraggingBlock) {
                    int pixelX = gridOffsetX + block.position.x * cellSize;
                    int pixelY = gridOffsetY + block.position.y * cellSize;
                    drawBlock(canvas, block, pixelX, pixelY, cellSize);
                }
            }
        }
    }
    
    private void drawWaitingBlocks(Canvas canvas) {
        // Draw waiting blocks in a row below the grid
        int waitingBlocksCount = 0;
        
        synchronized (mutex) {
            for (ProcessBlock block : activeProcesses) {
                if (!block.isPlaced && block != currentDraggingBlock) {
                    int blockWidth = block.getWidth() * cellSize;
                    int blockHeight = block.getHeight() * cellSize;
                    
                    // Position blocks evenly in the waiting area
                    int spacing = Math.max(10, (width - 5 * blockWidth) / 6);
                    int pixelX = spacing + waitingBlocksCount * (blockWidth + spacing);
                    int pixelY = gridOffsetY + GRID_HEIGHT * cellSize + 20;
                    
                    // Store the drawing position for touch detection
                    block.tempDrawX = pixelX;
                    block.tempDrawY = pixelY;
                    block.tempDrawCellSize = cellSize;
                    
                    drawBlock(canvas, block, pixelX, pixelY, cellSize);
                    
                    // Indicate starving blocks with a red outline
                    if (block.isStarving()) {
                        canvas.drawRect(
                            pixelX - 3, 
                            pixelY - 3, 
                            pixelX + blockWidth + 3, 
                            pixelY + blockHeight + 3, 
                            starvingPaint
                        );
                    }
                    
                    waitingBlocksCount++;
                }
            }
        }
    }
    
    private void drawDraggingBlock(Canvas canvas) {
        synchronized (mutex) {
            if (currentDraggingBlock != null) {
                // Draw shadow/preview if over grid
                int pixelX = currentDraggingBlock.tempDrawX;
                int pixelY = currentDraggingBlock.tempDrawY;
                
                if (isOverGrid(pixelX, pixelY)) {
                    int gridX = (pixelX - gridOffsetX) / cellSize;
                    int gridY = (pixelY - gridOffsetY) / cellSize;
                    drawPlacementPreview(canvas, currentDraggingBlock, gridX, gridY);
                }
                
                // Draw the actual dragging block
                drawBlock(canvas, currentDraggingBlock, pixelX, pixelY, cellSize);
                
                // Add a debug indicator - red dot at drag point for visibility
                Paint debugPaint = new Paint();
                debugPaint.setColor(Color.RED);
                canvas.drawCircle(pixelX, pixelY, 5, debugPaint);
            }
        }
    }
    
    private void drawBlock(Canvas canvas, ProcessBlock block, int pixelX, int pixelY, int cellSize) {
        for (int y = 0; y < block.getHeight(); y++) {
            for (int x = 0; x < block.getWidth(); x++) {
                if (block.shape[y][x] == 1) {
                    drawCell(canvas, pixelX + x * cellSize, pixelY + y * cellSize, 
                            block.color, block.getProgress(), cellSize);
                }
            }
        }
    }
    
    private void drawCell(Canvas canvas, int pixelX, int pixelY, int color, float progress, int size) {
        // Draw the cell background
        blockPaint.setColor(color);
        canvas.drawRect(pixelX, pixelY, pixelX + size, pixelY + size, blockPaint);
        
        // Draw progress overlay
        if (progress > 0.0f) {
            int progressHeight = (int)(size * progress);
            canvas.drawRect(
                pixelX, 
                pixelY + size - progressHeight, 
                pixelX + size, 
                pixelY + size, 
                progressPaint
            );
        }
    }
    
    private void drawPlacementPreview(Canvas canvas, ProcessBlock block, int gridX, int gridY) {
        boolean canPlace = canPlaceBlock(block, gridX, gridY);
        
        Paint previewPaint = new Paint();
        previewPaint.setColor(canPlace ? Color.GREEN : Color.RED);
        previewPaint.setStyle(Paint.Style.STROKE);
        previewPaint.setStrokeWidth(2);
        
        int pixelX = gridOffsetX + gridX * cellSize;
        int pixelY = gridOffsetY + gridY * cellSize;
        
        for (int y = 0; y < block.getHeight(); y++) {
            for (int x = 0; x < block.getWidth(); x++) {
                if (block.shape[y][x] == 1) {
                    canvas.drawRect(
                        pixelX + x * cellSize,
                        pixelY + y * cellSize,
                        pixelX + x * cellSize + cellSize,
                        pixelY + y * cellSize + cellSize,
                        previewPaint
                    );
                }
            }
        }
    }
    
    private boolean isOverGrid(int pixelX, int pixelY) {
        return pixelX >= gridOffsetX && 
               pixelX < gridOffsetX + GRID_WIDTH * cellSize &&
               pixelY >= gridOffsetY && 
               pixelY < gridOffsetY + GRID_HEIGHT * cellSize;
    }
    
    public void update() {
        long currentTime = SystemClock.elapsedRealtime();
        long deltaTime = currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;
        
        synchronized (mutex) {
            // Update timers for placed processes and check for completion
            List<ProcessBlock> finishedProcesses = new ArrayList<>();
            for (ProcessBlock process : activeProcesses) {
                if (process.isPlaced) {
                    process.updateTimer();
                    if (process.isFinished) {
                        finishedProcesses.add(process);
                    }
                }
            }
    
            // Remove finished processes
            if (!finishedProcesses.isEmpty()) {
                for (ProcessBlock finished : finishedProcesses) {
                    removeFromGrid(finished);
                    activeProcesses.remove(finished);
                }
            }
    
            // Check for line clears
            checkAndClearLines();
    
            // Spawn new blocks periodically
            if (currentTime - lastSpawnTime > BLOCK_SPAWN_INTERVAL) {
                int waitingCount = 0;
                for (ProcessBlock p : activeProcesses) {
                    if (!p.isPlaced) waitingCount++;
                }
                
                if (waitingCount < 5) {
                    spawnNewBlock();
                    lastSpawnTime = currentTime;
                }
            }
        }
    }
    
    private void spawnNewBlock() {
        ProcessBlock newBlock = ProcessBlock.createRandomProcess();
        newBlock.position = new Point(-1, -1); // Mark as off-grid initially
        synchronized (mutex) {
            activeProcesses.add(newBlock);
        }
        Log.d(LOG_TAG, "Spawned new block: ID " + newBlock.id);
    }
    
    // Check if a block can be placed at the target grid position
    private boolean canPlaceBlock(ProcessBlock block, int gridX, int gridY) {
        if (block == null) return false;

        for (int y = 0; y < block.getHeight(); y++) {
            for (int x = 0; x < block.getWidth(); x++) {
                if (block.shape[y][x] == 1) {
                    int checkX = gridX + x;
                    int checkY = gridY + y;

                    // Check grid boundaries
                    if (checkX < 0 || checkX >= GRID_WIDTH || checkY < 0 || checkY >= GRID_HEIGHT) {
                        return false; // Out of bounds
                    }

                    // Check for collision with existing blocks on the grid
                    if (grid[checkY][checkX] != 0) {
                        return false; // Cell occupied
                    }
                }
            }
        }
        return true;
    }

    // Place a block onto the grid data structure
    public boolean placeBlockOnGrid(ProcessBlock block, int gridX, int gridY) {
        if (block == null || !canPlaceBlock(block, gridX, gridY)) return false;

        block.position.set(gridX, gridY);
        block.isPlaced = true;
        block.startTimer();

        // Update the grid data structure
        synchronized (mutex) {
            for (int y = 0; y < block.getHeight(); y++) {
                for (int x = 0; x < block.getWidth(); x++) {
                    if (block.shape[y][x] == 1) {
                        grid[gridY + y][gridX + x] = block.id + 1; // +1 to avoid 0 (empty cell)
                    }
                }
            }
        }
        return true;
    }

    private void removeFromGrid(ProcessBlock block) {
        if (block == null || !block.isPlaced) return;

        int gridX = block.position.x;
        int gridY = block.position.y;

        // Clear grid cells
        synchronized (mutex) {
            for (int y = 0; y < block.getHeight(); y++) {
                for (int x = 0; x < block.getWidth(); x++) {
                    if (block.shape[y][x] == 1) {
                        int clearX = gridX + x;
                        int clearY = gridY + y;
                        if (clearX >= 0 && clearX < GRID_WIDTH && clearY >= 0 && clearY < GRID_HEIGHT) {
                            grid[clearY][clearX] = 0;
                        }
                    }
                }
            }
        }

        block.isPlaced = false;
    }

    private void checkAndClearLines() {
        boolean needShift = false;
        
        // Check for filled rows
        synchronized (mutex) {
            for (int y = GRID_HEIGHT - 1; y >= 0; y--) {
                boolean rowFilled = true;
                for (int x = 0; x < GRID_WIDTH; x++) {
                    if (grid[y][x] == 0) {
                        rowFilled = false;
                        break;
                    }
                }
                
                if (rowFilled) {
                    // Clear this row
                    for (int x = 0; x < GRID_WIDTH; x++) {
                        // Find and update the block that occupies this cell
                        int blockId = grid[y][x] - 1; // -1 to get actual ID
                        ProcessBlock block = findProcessById(blockId);
                        if (block != null) {
                            removeFromGrid(block);
                        }
                        grid[y][x] = 0;
                    }
                    needShift = true;
                }
            }
            
            // Check for filled columns
            for (int x = 0; x < GRID_WIDTH; x++) {
                boolean colFilled = true;
                for (int y = 0; y < GRID_HEIGHT; y++) {
                    if (grid[y][x] == 0) {
                        colFilled = false;
                        break;
                    }
                }
                
                if (colFilled) {
                    // Clear this column
                    for (int y = 0; y < GRID_HEIGHT; y++) {
                        // Find and update the block that occupies this cell
                        int blockId = grid[y][x] - 1; // -1 to get actual ID
                        ProcessBlock block = findProcessById(blockId);
                        if (block != null) {
                            removeFromGrid(block);
                        }
                        grid[y][x] = 0;
                    }
                    needShift = true;
                }
            }
        }
    }
    
    private ProcessBlock findProcessById(int id) {
        for (ProcessBlock process : activeProcesses) {
            if (process.id == id) {
                return process;
            }
        }
        return null;
    }
    
    public ProcessBlock findBlockAtTouch(float touchX, float touchY) {
        // First check if touch is on a waiting block
        synchronized (mutex) {
            for (ProcessBlock block : activeProcesses) {
                if (!block.isPlaced && block.tempDrawX >= 0) {
                    int blockWidth = block.getWidth() * block.tempDrawCellSize;
                    int blockHeight = block.getHeight() * block.tempDrawCellSize;
                    
                    if (touchX >= block.tempDrawX && 
                        touchX < block.tempDrawX + blockWidth &&
                        touchY >= block.tempDrawY && 
                        touchY < block.tempDrawY + blockHeight) {
                        return block;
                    }
                }
            }
        
            // Then check if touch is on a placed block
            if (isOverGrid((int)touchX, (int)touchY)) {
                int gridX = (int)((touchX - gridOffsetX) / cellSize);
                int gridY = (int)((touchY - gridOffsetY) / cellSize);
                
                if (gridX >= 0 && gridX < GRID_WIDTH && gridY >= 0 && gridY < GRID_HEIGHT) {
                    int blockId = grid[gridY][gridX] - 1; // -1 to get actual ID
                    if (blockId >= 0) {
                        return findProcessById(blockId);
                    }
                }
            }
        }
        
        return null;
    }
    
    public void startDragging(ProcessBlock block, float touchX, float touchY) {
        synchronized (mutex) {
            if (block != null) {
                Log.d(LOG_TAG, "Starting to drag block ID: " + block.id);
                currentDraggingBlock = block;
                
                // Calculate drag offset based on touch location within block
                if (!block.isPlaced) {
                    dragOffset.x = (int)(touchX - block.tempDrawX);
                    dragOffset.y = (int)(touchY - block.tempDrawY);
                } else {
                    // Remove from grid if it was placed
                    removeFromGrid(block);
                    
                    int pixelX = gridOffsetX + block.position.x * cellSize;
                    int pixelY = gridOffsetY + block.position.y * cellSize;
                    dragOffset.x = (int)(touchX - pixelX);
                    dragOffset.y = (int)(touchY - pixelY);
                }
            }
        }
    }
    
    public void updateDragging(float touchX, float touchY) {
        synchronized (mutex) {
            if (currentDraggingBlock != null) {
                // Update the block's drawing position
                int oldX = currentDraggingBlock.tempDrawX;
                int oldY = currentDraggingBlock.tempDrawY;
                
                currentDraggingBlock.tempDrawX = (int)(touchX - dragOffset.x);
                currentDraggingBlock.tempDrawY = (int)(touchY - dragOffset.y);
                currentDraggingBlock.tempDrawCellSize = cellSize;
                
                Log.d(LOG_TAG, "Dragging block ID: " + currentDraggingBlock.id + 
                      " from (" + oldX + "," + oldY + ") to (" + 
                      currentDraggingBlock.tempDrawX + "," + currentDraggingBlock.tempDrawY + ")");
            }
        }
    }
    
    public void stopDragging(float touchX, float touchY) {
        synchronized (mutex) {
            if (currentDraggingBlock != null) {
                // Check if over grid
                if (isOverGrid((int)touchX, (int)touchY)) {
                    int gridX = (int)((touchX - dragOffset.x - gridOffsetX) / cellSize);
                    int gridY = (int)((touchY - dragOffset.y - gridOffsetY) / cellSize);
                    
                    // Try to place the block
                    placeBlockOnGrid(currentDraggingBlock, gridX, gridY);
                }
                
                currentDraggingBlock = null;
            }
        }
    }
    
    public void click(MotionEvent event) {
        // Handle click events, can be used for block rotation, etc.
        Log.d(LOG_TAG, "click event");
    }
    
    public long getSleepTime() {
        synchronized (mutex) {
            // Use a shorter sleep time when dragging for smoother interaction
            if (currentDraggingBlock != null) {
                return 8; // About 120 FPS for dragging
            }
            return 1000 / targetFps;
        }
    }
} 