package com.example.lab1_task2;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.InputStream;

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
        try {
            // Convert sample MusicXML to MIDI
            File midiFile = new File(getCacheDir(), "temp_midi.mid");
            InputStream xmlInput = getResources().openRawResource(R.raw.sample_score);
            MusicXmlToMidiConverter.convert(xmlInput, midiFile);
            
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(midiFile.getAbsolutePath());
            mediaPlayer.prepare();

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
            Toast.makeText(this, "Error playing MusicXML: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
