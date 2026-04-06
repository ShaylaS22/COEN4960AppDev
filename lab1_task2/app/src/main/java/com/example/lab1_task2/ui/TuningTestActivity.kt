package com.example.lab1_task2.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lab1_task2.R

class TuningTestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tuning_test)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, TuningFragment())
                .commit()
        }
    }
}
