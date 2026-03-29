package com.example.lab1_task2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MetronomeActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private FrameLayout noteContainer;
    private final Random random = new Random();

    // Editable list of colors for the musical notes
    private final int[] noteColors = {
            Color.parseColor("#F44336"), // Red
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#FFEB3B"), // Yellow
            Color.parseColor("#9C27B0"), // Purple
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#00BCD4")  // Cyan
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metronome);

        noteContainer = findViewById(R.id.noteContainer);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnPlaySound).setOnClickListener(v -> {
            playSound();
            spawnNote();
        });
    }

    private void playSound() {
        // Note: R.raw.metronome_sfx_placeholder must exist in res/raw/ for this to work.
        // If it doesn't exist, create it or update the resource name here.
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.metronome_sfx_placeholder);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                });
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void spawnNote() {
        if (noteContainer == null) return;

        ImageView noteView = new ImageView(this);
        noteView.setImageResource(R.drawable.ic_musical_note);

        // Select random color from the editable list
        int color = noteColors[random.nextInt(noteColors.length)];
        noteView.setColorFilter(color);

        // Set size
        int size = (int) (40 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        noteView.setLayoutParams(params);

        noteContainer.addView(noteView);

        // Animation: Rise quickly and decelerate
        float distance = 200 * getResources().getDisplayMetrics().density;
        ObjectAnimator animator = ObjectAnimator.ofFloat(noteView, "translationY", 0, -distance);
        animator.setDuration(400);
        animator.setInterpolator(new DecelerateInterpolator());

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                noteContainer.removeView(noteView);
            }
        });

        animator.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}