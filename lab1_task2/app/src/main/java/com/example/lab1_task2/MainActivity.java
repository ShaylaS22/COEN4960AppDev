package com.example.lab1_task2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ViewFlipper viewFlipper;
    private String name; //Holds the user's name
    private String ageGroup; //Holds the user's identified age group
    private String categories = ""; //Holds the user's selected favorite categories of literature

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewFlipper = findViewById(R.id.viewFlipper); //Used to navigate between each screen

        //Section 1: Name Input
        EditText editTextName = findViewById(R.id.NameInput);

        Button nextButton1 = findViewById(R.id.nextButton1);
        nextButton1.setOnClickListener(v -> {
            name = editTextName.getText().toString();
            if (name.trim().isEmpty()) {
                editTextName.setError("Please enter your name");
            } else {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                viewFlipper.showNext();
            }
        });

        //Section 2: Age Group Selection
        RadioGroup ageRadioGroup = findViewById(R.id.ageRadioGroup);
        Button nextButton2 = findViewById(R.id.nextButton2);
        nextButton2.setOnClickListener(v -> {
            int selectedId = ageRadioGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(MainActivity.this, "Please select an age group", Toast.LENGTH_SHORT).show();
            } else {
                RadioButton selectedRadioButton = findViewById(selectedId);
                ageGroup = selectedRadioButton.getText().toString();
                viewFlipper.showNext();
            }
        });

        //Section 3: Category Selection
        Button submitButton = findViewById(R.id.submitButton);
        submitButton.setOnClickListener(v -> {
            categories = "";
            CheckBox checkBox1 = findViewById(R.id.checkBox1);
            if (checkBox1.isChecked()) {
                categories += "Fiction ";
            }
            CheckBox checkBox2 = findViewById(R.id.checkBox2);
            if (checkBox2.isChecked()) {
                categories += "Non-Fiction ";
            }
            CheckBox checkBox3 = findViewById(R.id.checkBox3);
            if (checkBox3.isChecked()) {
                categories += "Mystery ";
            }
            CheckBox checkBox4 = findViewById(R.id.checkBox4);
            if (checkBox4.isChecked()) {
                categories += "Sci-Fi ";
            }
            CheckBox checkBox5 = findViewById(R.id.checkBox5);
            if (checkBox5.isChecked()) {
                categories += "Romance ";
            }
            CheckBox checkBox6 = findViewById(R.id.checkBox6);
            if (checkBox6.isChecked()) {
                categories += "Manga ";
            }

            if (categories.trim().isEmpty()) {
                Toast.makeText(MainActivity.this, "Please select at least one category", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(MainActivity.this, SummaryActivity.class);
                intent.putExtra("name", name);
                intent.putExtra("ageGroup", ageGroup);
                intent.putExtra("categories", categories.trim());
                startActivity(intent);
            }
        });
    }
}