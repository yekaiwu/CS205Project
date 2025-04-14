package com.example.cs205;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Optional: Make activity fullscreen for a better game experience
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Set the content view to your layout file which contains the GameView
        // Make sure activity_main.xml exists in res/layout and contains:
        // <your.package.name.GameView
        //     android:id="@+id/gameView"
        //     android:layout_width="match_parent"
        //     android:layout_height="match_parent" />
        setContentView(R.layout.activity_main);

        // No need to get the GameView instance here unless you need to call specific public methods on it from the Activity
        // GameView gameView = findViewById(R.id.gameView);
    }
}