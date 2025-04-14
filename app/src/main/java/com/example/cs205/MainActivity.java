package com.example.cs205;

import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.Manifest;

import androidx.appcompat.app.AppCompatActivity;

//public class MainActivity extends AppCompatActivity implements Timer.TimerListener {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        // Optional: Make activity fullscreen for a better game experience
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
//        // Set the content view to your layout file which contains the GameView
//        // Make sure activity_main.xml exists in res/layout and contains:
//        // <your.package.name.GameView
//        //     android:id="@+id/gameView"
//        //     android:layout_width="match_parent"
//        //     android:layout_height="match_parent" />
//        setContentView(R.layout.activity_main);
//
//        // No need to get the GameView instance here unless you need to call specific public methods on it from the Activity
//        // GameView gameView = findViewById(R.id.gameView);
//    }
//}

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity implements Timer.TimerListener {

    private Button startButton;
    private Button stopButton;
    private TextView timerTextView;
    private Timer timer;

    private Button incrementButton;
    private int counter = 0;
    private final ReentrantLock lock = new ReentrantLock();
    private HighScoreDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
        dbHelper = new HighScoreDatabaseHelper(this);
        incrementButton = findViewById(R.id.incrementButton);
        TextView highScoreTextView = findViewById(R.id.highScoreTextView);
        TextView currentCounterTextView = findViewById(R.id.currentCounterTextView);
        // Display the highest score
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int highScore = dbHelper.getHighestCounter(db);
        highScoreTextView.setText("High Score: " + highScore);

        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        timerTextView = findViewById(R.id.timerTextView);

        // Example: 30-second timer with 1-second interval
        timer = new Timer(10000, 1000, this);

        startButton.setOnClickListener(v -> {
            if (!timer.isRunning()) {
                timer.start();
            }
        });

        stopButton.setOnClickListener(v -> timer.stop());

        incrementButton.setOnClickListener(v -> {
            lock.lock();
            try {
                counter++;
                currentCounterTextView.setText("Current Counter: " + counter);
            } finally {
                lock.unlock();
            }
        });
    }

    @Override
    public void onTick(long millisUntilFinished) {
        long minutes = (millisUntilFinished / 1000) / 60;
        long seconds = (millisUntilFinished / 1000) % 60;
        String timeFormatted = String.format("%02d:%02d", minutes, seconds);
        timerTextView.setText("Time left: " + timeFormatted);
    }
    @Override
    public void onFinish() {
        timerTextView.setText("Time's up!");
        saveHighestCounter();
        // push notification
        NotificationPublisher.showNotification(this);
        recreate();
    }

    private void saveHighestCounter() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        dbHelper.saveHighestCounter(db, counter);
    }


}