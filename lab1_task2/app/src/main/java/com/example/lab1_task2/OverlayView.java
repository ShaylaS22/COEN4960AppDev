package com.example.lab1_task2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {
    private final Paint paint = new Paint();
    private List<RectF> detectedNotes = new ArrayList<>();
    private List<Float> staffLines = new ArrayList<>();
    private long lastFrameTime = 0;
    private boolean isEngineRunning = false;

    public OverlayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5.0f);
        paint.setTextSize(40.0f);
        paint.setAntiAlias(true);
    }

    public void setEngineRunning(boolean running) {
        this.isEngineRunning = running;
        postInvalidate();
    }

    public void updateDetections(List<RectF> notes, List<Float> lines) {
        this.detectedNotes = notes;
        this.staffLines = lines;
        this.lastFrameTime = System.currentTimeMillis();
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // 1. Draw "Heartbeat" Indicator (Top-Left)
        // This proves the UI is refreshing and the analyzer is sending data
        if (System.currentTimeMillis() - lastFrameTime < 1000) {
            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(50, 50, 20, paint);
        } else {
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(50, 50, 20, paint);
        }
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5.0f);

        // 2. Draw Staff Lines (Red)
        paint.setColor(Color.RED);
        for (Float y : staffLines) {
            canvas.drawLine(0, y, getWidth(), y, paint);
            // Draw a label for feedback
            canvas.drawText("Staff", 10, y - 10, paint);
        }

        // 3. Draw Detections (Green Boxes)
        paint.setColor(Color.GREEN);
        for (RectF rect : detectedNotes) {
            canvas.drawRect(rect, paint);
        }
        
        // 4. Debug: If nothing detected, show message
        if (staffLines.isEmpty() && detectedNotes.isEmpty()) {
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextSize(30f);
            canvas.drawText("No symbols detected. Check light/distance.", getWidth()/2f - 200, getHeight() - 100, paint);
        }
    }
}
