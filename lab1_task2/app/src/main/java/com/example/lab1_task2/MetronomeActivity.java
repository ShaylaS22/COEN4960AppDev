package com.example.lab1_task2;

import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetronomeActivity extends AppCompatActivity {

    private SoundPool soundPool;
    private int clickSoundId;
    private EditText etBpm;
    private Button btnStartStop;
    private ImageView ivMetronome;
    private int bpm = 120;
    private boolean isRunning = false;
    private ScheduledExecutorService executorService;
    
    private int currentFrameIndex = 0;
    private final int[] metronomeFrames = {
            R.drawable.metronome_1, R.drawable.metronome_2, R.drawable.metronome_3,
            R.drawable.metronome_4, R.drawable.metronome_5, R.drawable.metronome_6,
            R.drawable.metronome_7, R.drawable.metronome_8, R.drawable.metronome_9,
            R.drawable.metronome_10, R.drawable.metronome_11, R.drawable.metronome_12
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metronome);

        initSoundPool();

        ivMetronome = findViewById(R.id.ivMetronome);
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
        clickSoundId = soundPool.load(this, R.raw.metronome_click, 1);
    }

    private void startMetronome() {
        updateBpm();
        if (bpm <= 0) return;

        isRunning = true;
        btnStartStop.setText("Stop Metronome");
        currentFrameIndex = 0;

        // 6 frames per beat (since clicks are on frames 1 and 7)
        long frameIntervalMs = 60000 / (bpm * 6);

        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            // Play sound on Frame 1 (index 0) and Frame 7 (index 6)
            if (currentFrameIndex == 0 || currentFrameIndex == 6) {
                if (soundPool != null) {
                    soundPool.play(clickSoundId, 1.0f, 1.0f, 1, 0, 1.0f);
                }
            }
            
            // Update UI frame
            final int frameRes = metronomeFrames[currentFrameIndex];
            runOnUiThread(() -> ivMetronome.setImageResource(frameRes));
            
            // Increment frame index
            currentFrameIndex = (currentFrameIndex + 1) % metronomeFrames.length;
            
        }, 0, frameIntervalMs, TimeUnit.MILLISECONDS);
    }

    private void stopMetronome() {
        isRunning = false;
        if (executorService != null) {
            executorService.shutdownNow();
            executorService = null;
        }
        btnStartStop.setText("Start Metronome");
        // Reset to first frame or a static icon
        runOnUiThread(() -> ivMetronome.setImageResource(R.drawable.metronome_icon));
    }

    private void updateBpm() {
        String bpmStr = etBpm.getText().toString();
        if (!bpmStr.isEmpty()) {
            try {
                bpm = Integer.parseInt(bpmStr);
                if (bpm <= 0) bpm = 120;
            } catch (NumberFormatException e) {
                bpm = 120;
            }
        } else {
            bpm = 120;
        }
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