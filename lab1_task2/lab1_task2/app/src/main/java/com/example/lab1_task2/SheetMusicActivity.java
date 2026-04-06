package com.example.lab1_task2;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SheetMusicActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sheet_music);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}