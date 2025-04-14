package com.example.cs205;

import android.annotation.SuppressLint;
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
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * A class representing a view for the game activity.
 */
public class GameView extends SurfaceView implements SurfaceHolder.Callback {

    private final String LOG_TAG = GameView.class.getSimpleName();
    private final Game game = new Game(this::sendNotification, this::useCanvas);
    private GameThread gameThread;

    @SuppressLint("ClickableViewAccessibility")
    public GameView(final Context context) {
        super(context);
        setKeepScreenOn(true);
        getHolder().addCallback(this);
        setFocusable(View.FOCUSABLE);
        setOnTouchListener((view, event) -> {
            handleTouchEvent(event);
            return true;
        });
    }

    private void sendNotification() {
        // This would be used for game completion or other notifications
        Log.d(LOG_TAG, "Game notification triggered");
    }

    private boolean useCanvas(final Consumer<Canvas> onDraw) {
        boolean result = false;
        Canvas canvas = null;
        try {
            final SurfaceHolder holder = getHolder();
            canvas = holder.lockCanvas();
            if (canvas != null) {
                // Pass the canvas to onDraw (in this case draw method from Game class)
                onDraw.accept(canvas);
                holder.unlockCanvasAndPost(canvas);
                result = true;
            }
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Error in useCanvas: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public void surfaceCreated(final SurfaceHolder surfaceHolder) {
        Log.d(LOG_TAG, "Surface created");
        
        // Check if thread exists and its state
        if (gameThread == null) {
            Log.d(LOG_TAG, "Creating new game thread");
            gameThread = new GameThread(game);
            final android.graphics.Rect rect = getHolder().getSurfaceFrame();
            game.resize(rect.width(), rect.height());
            gameThread.startLoop();
        } else if (gameThread.getState() == Thread.State.TERMINATED) {
            Log.d(LOG_TAG, "Thread was terminated, creating new one");
            gameThread = new GameThread(game);
            final android.graphics.Rect rect = getHolder().getSurfaceFrame();
            game.resize(rect.width(), rect.height());
            gameThread.startLoop();
        } else {
            // Thread exists and is not terminated, so it's either NEW or RUNNABLE
            Log.d(LOG_TAG, "Thread already exists in state: " + gameThread.getState());
            if (gameThread.getState() == Thread.State.NEW) {
                // Thread created but not started yet
                gameThread.startLoop();
            } else {
                // Thread is already running, just update dimensions
                final android.graphics.Rect rect = getHolder().getSurfaceFrame();
                game.resize(rect.width(), rect.height());
            }
        }
    }

    @Override
    public void surfaceChanged(final SurfaceHolder surfaceHolder, final int format, final int width, final int height) {
        Log.d(LOG_TAG, "Surface changed: width=" + width + ", height=" + height);
        game.resize(width, height);
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder surfaceHolder) {
        Log.d(LOG_TAG, "Surface destroyed");
        gameThread.stopLoop();
        gameThread = null;
    }

    public void pause() {
        Log.d(LOG_TAG, "Pausing game");
        if (gameThread != null) {
            gameThread.stopLoop();
        }
    }

    public void resume() {
        Log.d(LOG_TAG, "Resuming game");
        if (gameThread == null) {
            Log.d(LOG_TAG, "No thread exists, creating through surfaceCreated");
            surfaceCreated(getHolder());
        } else if (gameThread.getState() == Thread.State.TERMINATED) {
            Log.d(LOG_TAG, "Thread terminated, recreating through surfaceCreated");
            surfaceCreated(getHolder());
        } else if (!gameThread.isAlive()) {
            Log.d(LOG_TAG, "Thread exists but not alive, starting it");
            gameThread.startLoop();
        } else {
            Log.d(LOG_TAG, "Thread already running in state: " + gameThread.getState());
        }
    }

    @Override
    public void draw(final Canvas canvas) {
        super.draw(canvas);
        game.draw(); // Initial draw at the creation of SurfaceView
    }

    // Handle touch events and pass them to the Game class
    private void handleTouchEvent(MotionEvent event) {
        // Obtain a reusable event to avoid memory allocation for log messages
        MotionEvent touchEvent = MotionEvent.obtain(event);
        
        try {
            float touchX = touchEvent.getX();
            float touchY = touchEvent.getY();
            
            // Process the event based on its action
            switch (touchEvent.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    game.click(touchEvent);
                    ProcessBlock block = game.findBlockAtTouch(touchX, touchY);
                    if (block != null) {
                        game.startDragging(block, touchX, touchY);
                        Log.d(LOG_TAG, "Started dragging block at " + touchX + "," + touchY);
                    }
                    break;
                    
                case MotionEvent.ACTION_MOVE:
                    // Handle continuous dragging (most frequent event)
                    game.updateDragging(touchX, touchY);
                    break;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Handle drag completion or cancellation
                    game.stopDragging(touchX, touchY);
                    Log.d(LOG_TAG, "Stopped dragging block at " + touchX + "," + touchY);
                    break;
            }
        } finally {
            // Always recycle the obtained event when done to avoid memory leaks
            touchEvent.recycle();
        }
    }
}