package com.example.lab1_task2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MetronomeActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private FrameLayout noteContainer;
    private EditText etBpm;
    private Button btnStartStop;
    private int bpm = 120;
    private boolean isRunning = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable metronomeRunnable;
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
        etBpm = findViewById(R.id.etBpm);
        etBpm.setText(String.valueOf(bpm));

        btnStartStop = findViewById(R.id.btnPlaySound);
        btnStartStop.setText("Start Metronome");

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnStartStop.setOnClickListener(v -> {
            if (isRunning) {
                stopMetronome();
            } else {
                startMetronome();
            }
        });
    }

    private void startMetronome() {
        updateBpm();
        if (bpm <= 0) return;

        isRunning = true;
        btnStartStop.setText("Stop Metronome");

        long interval = 60000 / bpm;

        metronomeRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isRunning) return;
                playSound();
                spawnNote();
                handler.postDelayed(this, interval);
            }
        };
        handler.post(metronomeRunnable);
    }

    private void stopMetronome() {
        isRunning = false;
        if (metronomeRunnable != null) {
            handler.removeCallbacks(metronomeRunnable);
        }
        btnStartStop.setText("Start Metronome");
    }

    private void updateBpm() {
        String bpmStr = etBpm.getText().toString();
        if (!bpmStr.isEmpty()) {
            try {
                bpm = Integer.parseInt(bpmStr);
                if (bpm <= 0) bpm = 120; // Default if invalid
            } catch (NumberFormatException e) {
                bpm = 120;
            }
        } else {
            bpm = 120;
        }
        etBpm.setText(String.valueOf(bpm));
    }

    private void playSound() {
        try {
            // Using a new MediaPlayer instance for each click/tick might be resource intensive.
            // However, for simplicity and matching previous behavior, we'll create it here.
            // A better way would be SoundPool or reusing MediaPlayer.
            MediaPlayer mp = MediaPlayer.create(this, R.raw.metronome_click);
            if (mp != null) {
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void spawnNote() {
        if (noteContainer == null) return;

        ImageView noteView = new ImageView(this);
        noteView.setImageResource(R.drawable.ic_musical_note);

        int color = noteColors[random.nextInt(noteColors.length)];
        noteView.setColorFilter(color);

        int size = (int) (40 * getResources().getDisplayMetrics().density);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        params.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        noteView.setLayoutParams(params);

        noteContainer.addView(noteView);

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
        stopMetronome();
    }
}