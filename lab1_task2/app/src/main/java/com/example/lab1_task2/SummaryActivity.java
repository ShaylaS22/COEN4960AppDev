package com.example.lab1_task2;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SummaryActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_summary);

        // Fetch the data from the intent and display it in the UI
        TextView textViewName = findViewById(R.id.textViewName);
        TextView textViewAgeGroup = findViewById(R.id.textViewAgeGroup);
        TextView textViewCategories = findViewById(R.id.textViewCategories);

        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        String ageGroup = intent.getStringExtra("ageGroup");
        String categories = intent.getStringExtra("categories");

        textViewName.setText("Name: " + name);
        textViewAgeGroup.setText("Age Group: " + ageGroup);
        textViewCategories.setText("Favorite Categories: " + categories);
    }
}