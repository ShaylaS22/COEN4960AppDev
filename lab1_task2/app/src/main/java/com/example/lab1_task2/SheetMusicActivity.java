package com.example.lab1_task2;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SheetMusicActivity extends AppCompatActivity {
    private static final String TAG = "SheetMusicActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private PreviewView viewFinder;
    private OverlayView overlayView;
    private TextView tvStatus;
    private ExecutorService cameraExecutor;

    private Interpreter segmentationModel;
    private Interpreter encoderModel;
    private Interpreter decoderModel;

    private List<String> rhythmVocab, pitchVocab, liftVocab, articulationVocab, positionVocab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sheet_music);

        viewFinder = findViewById(R.id.viewFinder);
        overlayView = findViewById(R.id.overlayView);
        tvStatus = findViewById(R.id.tvStatus);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        initModelsResilient();
    }

    private void initModelsResilient() {
        cameraExecutor.execute(() -> {
            try {
                Log.d(TAG, "Starting resilient model load...");
                Interpreter.Options options = new Interpreter.Options();
                options.setNumThreads(4);
                // Enable Flex Ops (TF Select) for Transformer layers
                options.setUseXNNPACK(false);

                // Load labels
                try {
                    rhythmVocab = FileUtil.loadLabels(this, "rhythm_vocab.txt");
                    pitchVocab = FileUtil.loadLabels(this, "pitch_vocab.txt");
                    liftVocab = FileUtil.loadLabels(this, "lift_vocab.txt");
                    articulationVocab = FileUtil.loadLabels(this, "articulation_vocab.txt");
                    positionVocab = FileUtil.loadLabels(this, "position_vocab.txt");
                } catch (Exception e) { Log.e(TAG, "Labels missing", e); }

                // Load models independently
                try {
                    segmentationModel = new Interpreter(FileUtil.loadMappedFile(this, "sim_segnet_308-3296ccd40960f90ca6ab9c035cca945675d30a0f_fp16_float32.tflite"), options);
                    Log.d(TAG, "SegNet OK");
                } catch (Throwable t) { Log.e(TAG, "SegNet FAIL", t); }

                try {
                    encoderModel = new Interpreter(FileUtil.loadMappedFile(this, "sim_encoder_pytorch_model_331-e10346542968cc71fbcce0c0696f3ac963f11ae1_fp16_float32.tflite"), options);
                    Log.d(TAG, "Encoder OK");
                } catch (Throwable t) { Log.e(TAG, "Encoder FAIL", t); }

                try {
                    decoderModel = new Interpreter(FileUtil.loadMappedFile(this, "sim_decoder_pytorch_model_331-e10346542968cc71fbcce0c0696f3ac963f11ae1_fp16_float32.tflite"), options);
                    Log.d(TAG, "Decoder OK");
                } catch (Throwable t) { 
                    Log.e(TAG, "Decoder FAIL: Incompatible op detected.", t); 
                }

                runOnUiThread(() -> {
                    String status = "Ready: " + (segmentationModel != null ? "S " : "") + 
                                    (encoderModel != null ? "E " : "") + 
                                    (decoderModel != null ? "D" : "");
                    tvStatus.setText(status);
                    overlayView.setEngineRunning(true);
                });
            } catch (Exception e) { Log.e(TAG, "Engine load failed", e); }
        });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                imageAnalysis.setAnalyzer(cameraExecutor, new SheetMusicAnalyzer());
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis);
            } catch (Exception e) { Log.e(TAG, "Camera Error", e); }
        }, ContextCompat.getMainExecutor(this));
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) startCamera();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (segmentationModel != null) segmentationModel.close();
        if (encoderModel != null) encoderModel.close();
        if (decoderModel != null) decoderModel.close();
        cameraExecutor.shutdown();
    }

    private class SheetMusicAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(@NonNull ImageProxy image) {
            runOnUiThread(() -> overlayView.updateDetections(new ArrayList<>(), new ArrayList<>()));
            if (segmentationModel == null) { image.close(); return; }

            Bitmap bitmap = toBitmap(image);
            if (bitmap == null) { image.close(); return; }

            try {
                Bitmap resizedSeg = Bitmap.createScaledBitmap(bitmap, 512, 512, true);
                float[][][][] segInput = bitmapToFloatArray(resizedSeg, 512, 512);
                float[][][][] segOutput = new float[1][512][512][1];
                segmentationModel.run(segInput, segOutput);

                List<RectF> staffRects = parse(segOutput[0], overlayView.getWidth(), overlayView.getHeight());

                if (!staffRects.isEmpty() && encoderModel != null && decoderModel != null) {
                    processFullWorkflow(bitmap, staffRects);
                }

                runOnUiThread(() -> {
                    float max = findMax(segOutput[0]);
                    tvStatus.setText(String.format(Locale.US, "AI Conf: %.2f | Staffs: %d", max, staffRects.size()));
                    overlayView.updateDetections(new ArrayList<>(), getYs(staffRects));
                });
            } catch (Exception e) { Log.e(TAG, "Cycle failed", e); }
            finally { image.close(); }
        }

        private List<RectF> parse(float[][][] mask, int vw, int vh) {
            List<RectF> res = new ArrayList<>();
            float threshold = 0.15f; boolean in = false; int sy = 0;
            for (int y = 0; y < 512; y++) {
                float val = (mask[y][128][0] + mask[y][256][0] + mask[y][384][0]) / 3f;
                if (val > threshold && !in) { in = true; sy = y; }
                else if (val < threshold && in) {
                    in = false;
                    if (y - sy > 2) res.add(new RectF(0, (sy/512f)*vh, vw, (y/512f)*vh));
                }
            }
            return res;
        }

        private List<Float> getYs(List<RectF> rs) {
            List<Float> ys = new ArrayList<>();
            for (RectF r : rs) ys.add(r.centerY());
            return ys;
        }

        private float findMax(float[][][] mask) {
            float max = 0;
            for(int y=100; y<400; y+=20)
                for(int x=128; x<384; x+=20)
                    if(mask[y][x][0] > max) max = mask[y][x][0];
            return max;
        }

        private void processFullWorkflow(Bitmap full, List<RectF> rects) {
            try {
                for (RectF r : rects) {
                    Bitmap crop = cropStaff(full, r);
                    Bitmap resized = Bitmap.createScaledBitmap(crop, 1024, 128, true);
                    float[][][][] input = bitmapToFloatArray(resized, 1024, 128);
                    float[][][] encOut = new float[1][128][512];
                    encoderModel.run(input, encOut);

                    float[][] rOut = new float[1][100]; float[][] pOut = new float[1][100];
                    float[][] lOut = new float[1][100]; float[][] aOut = new float[1][100];
                    float[][] poOut = new float[1][100];
                    
                    Map<Integer, Object> outputs = new HashMap<>();
                    outputs.put(0, rOut); outputs.put(1, pOut); outputs.put(2, lOut);
                    outputs.put(3, aOut); outputs.put(4, poOut);
                    
                    decoderModel.runForMultipleInputsOutputs(new Object[]{encOut}, outputs);
                }
            } catch (Exception e) { Log.e(TAG, "WF Fail", e); }
        }

        private Bitmap toBitmap(ImageProxy image) {
            try {
                ImageProxy.PlaneProxy[] planes = image.getPlanes();
                ByteBuffer y = planes[0].getBuffer(), u = planes[1].getBuffer(), v = planes[2].getBuffer();
                byte[] nv21 = new byte[y.remaining() + u.remaining() + v.remaining()];
                y.get(nv21, 0, y.remaining()); v.get(nv21, y.remaining(), v.remaining()); u.get(nv21, y.remaining() + v.remaining(), u.remaining());
                YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                yuv.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 100, out);
                Bitmap bm = BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size());
                Matrix m = new Matrix(); m.postRotate(image.getImageInfo().getRotationDegrees());
                return Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
            } catch (Exception e) { return null; }
        }

        private float[][][][] bitmapToFloatArray(Bitmap bm, int w, int h) {
            float[][][][] res = new float[1][h][w][3];
            int[] px = new int[w * h];
            bm.getPixels(px, 0, w, 0, 0, w, h);
            for (int i = 0; i < px.length; i++) {
                float v = (((px[i] >> 16) & 0xFF) / 255f);
                res[0][i/w][i%w][0] = v; res[0][i/w][i%w][1] = v; res[0][i/w][i%w][2] = v;
            }
            return res;
        }

        private Bitmap cropStaff(Bitmap original, RectF rect) {
            float sx = (float) original.getWidth() / overlayView.getWidth();
            float sy = (float) original.getHeight() / overlayView.getHeight();
            int l = (int)(rect.left * sx), t = (int)(rect.top * sy);
            int w = (int)(rect.width() * sx), h = (int)(rect.height() * sy);
            l = Math.max(0, l); t = Math.max(0, t);
            w = Math.min(original.getWidth() - l, Math.max(1, w));
            h = Math.min(original.getHeight() - t, Math.max(1, h));
            return Bitmap.createBitmap(original, l, t, w, h);
        }
    }
}
