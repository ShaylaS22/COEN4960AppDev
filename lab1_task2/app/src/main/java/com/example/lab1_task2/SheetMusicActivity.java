package com.example.lab1_task2;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SheetMusicActivity extends AppCompatActivity {
    private MediaPlayer mediaPlayer;
    private Button btnPlay;
    private Button btnStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sheet_music);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnPlay = findViewById(R.id.btnPlayMidi);
        btnStop = findViewById(R.id.btnStopMidi);

        btnPlay.setOnClickListener(v -> playMidi());
        btnStop.setOnClickListener(v -> stopMidi());
    }

    private void playMidi() {
        // Note: You need to place a midi file (e.g., sample.mid) in res/raw/
        // For now, this is a placeholder implementation.
        try {
            // If we had a midi file named 'sample_midi' in res/raw:
             mediaPlayer = MediaPlayer.create(this, R.raw.midi_test);
            
            // Since no midi file exists yet, we show a message.
            //Toast.makeText(this, "Please add a MIDI file to res/raw/ to enable playback.", Toast.LENGTH_LONG).show();
            

            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(mp -> {
                    stopMidi();
                });
                mediaPlayer.start();
                btnPlay.setEnabled(false);
                btnStop.setEnabled(true);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
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
