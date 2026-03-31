package com.example.lab1_task2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.view.Gravity;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetronomeActivity extends AppCompatActivity {

    private SoundPool soundPool;
    private int clickSoundId;
    private FrameLayout noteContainer;
    private EditText etBpm;
    private Button btnStartStop;
    private int bpm = 120;
    private boolean isRunning = false;
    private ScheduledExecutorService executorService;
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

        initSoundPool();

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

    private void initSoundPool() {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();
        // Loading the click sound. Ensure R.raw.metronome_click exists.
        clickSoundId = soundPool.load(this, R.raw.metronome_click, 1);
    }

    private void startMetronome() {
        updateBpm();
        if (bpm <= 0) return;

        isRunning = true;
        btnStartStop.setText("Stop Metronome");

        long intervalMs = 60000 / bpm;

        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            // Play sound immediately on the background thread for low latency
            if (soundPool != null) {
                soundPool.play(clickSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
            }
            
            // UI updates must be on the main thread
            runOnUiThread(this::spawnNote);
            
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    private void stopMetronome() {
        isRunning = false;
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
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
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}