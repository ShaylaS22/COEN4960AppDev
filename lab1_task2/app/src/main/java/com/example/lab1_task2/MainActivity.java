package com.example.lab1_task2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    // Animation components
    private FrameLayout animationContainer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        animationContainer = findViewById(R.id.animationContainer);
        startMusicNoteAnimation();
    }

    private void startMusicNoteAnimation() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                createMusicNote();
                // Increased frequency: Randomly schedule the next note between 200ms and 1000ms
                handler.postDelayed(this, 200 + random.nextInt(800));
            }
        });
    }

    // Animator methods
    private void createMusicNote() {
        if (animationContainer == null) return;

        ImageView noteView = new ImageView(this);
        noteView.setImageResource(R.drawable.ic_musical_note);
        noteView.setAlpha(0.5f); // Start slightly transparent

        // Increased size: Set random size between 40dp and 80dp
        float density = getResources().getDisplayMetrics().density;
        int size = (int) ((40 + random.nextInt(40)) * density);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        
        // Random horizontal position
        int xPos = random.nextInt(Math.max(1, animationContainer.getWidth() - size));
        noteView.setX(xPos);
        noteView.setY(animationContainer.getHeight()); // Start at bottom

        animationContainer.addView(noteView, params);

        // Animate Y position (rising up)
        ObjectAnimator moveUp = ObjectAnimator.ofFloat(noteView, "translationY", animationContainer.getHeight(), -size);
        // Animate alpha (fading out)
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(noteView, "alpha", 0.5f, 0f);

        moveUp.setDuration(4000 + random.nextInt(4000)); // 4-8 seconds
        fadeOut.setDuration(moveUp.getDuration());

        moveUp.start();
        fadeOut.start();

        moveUp.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animationContainer.removeView(noteView);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
