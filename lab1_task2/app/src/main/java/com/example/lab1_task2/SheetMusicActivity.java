package com.example.lab1_task2;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SheetMusicActivity extends AppCompatActivity {
    private static final String TAG = "SheetMusicActivity";
    // Ensure this matches your running Ngrok URL
    private static final String COLAB_URL = "https://unvaried-unsubtle-footgear.ngrok-free.dev/process";

    private PreviewView viewFinder;
    private ImageView ivPreview;
    private Button btnCapture, btnSelect;
    private ProgressBar progressBar;
    private TextView tvStatus;

    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build();

    private final ActivityResultLauncher<String> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    showPreview(uri);
                }
            }
    );

    private final ActivityResultLauncher<String> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sheet_music);

        viewFinder = findViewById(R.id.viewFinder);
        ivPreview = findViewById(R.id.ivPreview);
        btnCapture = findViewById(R.id.btnCapture);
        btnSelect = findViewById(R.id.btnSelect);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        btnCapture.setOnClickListener(v -> takePhoto());
        btnSelect.setOnClickListener(v -> selectImageLauncher.launch("image/*"));

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Camera setup failed", e);
                tvStatus.setText("Camera initialization error");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        if (imageCapture == null) return;
        File photoFile = new File(getExternalFilesDir(null), "temp_capture.jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = FileProvider.getUriForFile(SheetMusicActivity.this, getPackageName() + ".provider", photoFile);
                showPreview(savedUri);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed", exception);
                runOnUiThread(() -> Toast.makeText(SheetMusicActivity.this, "Capture failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showPreview(Uri uri) {
        viewFinder.setVisibility(View.GONE);
        ivPreview.setVisibility(View.VISIBLE);
        ivPreview.setImageURI(uri);
        btnCapture.setText("Send to Colab");
        btnCapture.setOnClickListener(v -> uploadImage(uri));
        btnSelect.setText("Cancel");
        btnSelect.setOnClickListener(v -> resetUI());
    }

    private void resetUI() {
        viewFinder.setVisibility(View.VISIBLE);
        ivPreview.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        btnCapture.setText("Capture");
        btnCapture.setOnClickListener(v -> takePhoto());
        btnSelect.setText("Upload");
        btnSelect.setOnClickListener(v -> selectImageLauncher.launch("image/*"));
        tvStatus.setText("Capture or select sheet music");
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void uploadImage(Uri uri) {
        if (!isNetworkAvailable()) {
            tvStatus.setText("No internet connection available");
            Toast.makeText(this, "Check your network settings", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("OMR Processing... (May take 1 min)");

        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                tvStatus.setText("Error: File not found");
                progressBar.setVisibility(View.GONE);
                return;
            }

            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            byte[] imageBytes = byteBuffer.toByteArray();
            inputStream.close();

            if (imageBytes.length == 0) {
                tvStatus.setText("Error: Empty image file");
                progressBar.setVisibility(View.GONE);
                return;
            }

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "sheet.jpg",
                            RequestBody.create(imageBytes, MediaType.parse("image/jpeg")))
                    .build();

            Request request = new Request.Builder()
                    .url(COLAB_URL)
                    .addHeader("ngrok-skip-browser-warning", "true")
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        tvStatus.setText("Network error: " + e.getLocalizedMessage());
                        Log.e(TAG, "Connection failed", e);
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    final int code = response.code();
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            byte[] xmlBytes = response.body().bytes();
                            File xmlFile = new File(getFilesDir(), "output.musicxml");
                            try (FileOutputStream fos = new FileOutputStream(xmlFile)) {
                                fos.write(xmlBytes);
                            }
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                tvStatus.setText("Success! XML saved.");
                                Toast.makeText(SheetMusicActivity.this, "Transcription Complete!", Toast.LENGTH_LONG).show();

                                Intent intent = new Intent(SheetMusicActivity.this, TranscriptorActivity.class);
                                intent.putExtra("xml_path", xmlFile.getAbsolutePath());
                                startActivity(intent);
                            });
                        } else {
                            String errorDetail = response.body() != null ? response.body().string() : "No details";
                            runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                if (code == 404) tvStatus.setText("Error 404: Server route not found");
                                else tvStatus.setText("Server Error (" + code + ")");
                                Log.e(TAG, "Server Error (" + code + "): " + errorDetail);
                            });
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error reading server response", e);
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            tvStatus.setText("File save error");
                        });
                    } finally {
                        response.close();
                    }
                }
            });
        } catch (IOException e) {
            Log.e(TAG, "Local file error", e);
            tvStatus.setText("Local file read error");
            progressBar.setVisibility(View.GONE);
        }
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
