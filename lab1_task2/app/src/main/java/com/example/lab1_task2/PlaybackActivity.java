package com.example.lab1_task2;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;

public class PlaybackActivity extends AppCompatActivity {
    private static final String TAG = "PlaybackActivity";
    private MediaPlayer mediaPlayer;
    private Button btnPlay, btnStop;
    private String midiPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playback);

        midiPath = getIntent().getStringExtra("midi_path");

        btnPlay = findViewById(R.id.btnPlayMidi);
        btnStop = findViewById(R.id.btnStopMidi);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnPlay.setOnClickListener(v -> playMidi());
        btnStop.setOnClickListener(v -> stopMidi());

        if (midiPath == null) {
            btnPlay.setEnabled(false);
            Toast.makeText(this, "No MIDI file found", Toast.LENGTH_SHORT).show();
        }
    }

    private void playMidi() {
        if (midiPath == null) return;

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(midiPath);
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(mp -> stopMidi());
            mediaPlayer.start();

            btnPlay.setEnabled(false);
            btnStop.setEnabled(true);
        } catch (IOException e) {
            Log.e(TAG, "Error playing MIDI", e);
            Toast.makeText(this, "Error playing MIDI: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void stopMidi() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        btnPlay.setEnabled(true);
        btnStop.setEnabled(false);
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
