package com.example.lab1_task2;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class TunerActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tuner);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}