package com.aicandy.imageclassification.mobilenet;

import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int MAX_IMAGE_DIMENSION = 1024;

    private Classifier classifier;
    private TextView statusText;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        statusText = findViewById(R.id.statusText);

        classifier = new Classifier(Utils.assetFilePath(this, "mobilenet-v2.pt"));

        // Lấy danh sách các file hình ảnh từ thư mục assets
        List<String> images = getFileFromAssetsFolder("image_test");

        Handler handler = new Handler(Looper.getMainLooper());
        int delay = 8000; // 8 giây

        for (int i = 0; i < images.size(); i++) {
            final String image = images.get(i);

            // Đặt task xử lý ảnh với delay
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    processImage(image);
                }
            }, i * delay); // Mỗi ảnh sẽ được xử lý sau một khoảng thời gian delay
        }
    }


    private List<String> getFileFromAssetsFolder(String folderPath) {
        List<String> imageFiles = new ArrayList<>();
        AssetManager assetManager = this.getAssets();

        try {
            String[] files = assetManager.list(folderPath);
            if (files != null) {
                for (String fileName : files) {
                    if (fileName.endsWith(".png") || fileName.endsWith(".jpg")) {
                        imageFiles.add(folderPath + "/" + fileName);
                    }
                }
            } else {
                System.out.println("Thư mục rỗng hoặc không tồn tại.");
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi truy cập thư mục assets: " + e.getMessage());
        }

        return imageFiles;
    }

    private void processImage(final String fileName) {
        updateStatus("Đang xử lý ảnh...");

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    updateStatus("Đang đọc ảnh...");
                    Bitmap originalBitmap = loadImageFromAssets(fileName);
                    if (originalBitmap == null) {
                        throw new IOException("Không thể đọc ảnh từ assets");
                    }

                    updateStatus("Đang resize ảnh...");
                    Bitmap resizedBitmap = resizeBitmap(originalBitmap);

                    updateStatus("Đang phân loại ảnh...");
                    String prediction = classifier.predict(originalBitmap);
                    Log.d(TAG, "AIcandy.vn - detected: " + prediction);

                    // Lưu ảnh đã resize
                    String tempImagePath = saveBitmapToTempFile(resizedBitmap);

                    // Dọn dẹp bộ nhớ
                    originalBitmap.recycle();
                    if (resizedBitmap != originalBitmap) {
                        resizedBitmap.recycle();
                    }

                    // Chuyển sang màn hình kết quả
                    showResult(tempImagePath, prediction);

                } catch (Exception e) {
                    Log.e(TAG, "Lỗi xử lý ảnh", e);
                    showError("Lỗi: " + e.getMessage());
                }
            }
        });
    }

    private void updateStatus(final String status) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                statusText.setVisibility(View.VISIBLE);
                statusText.setText(status);
            }
        });
    }

    private void showResult(final String imagePath, final String prediction) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                statusText.setVisibility(View.GONE);
                Intent resultView = new Intent(MainActivity.this, Result.class);
                resultView.putExtra("image_path", imagePath);
                resultView.putExtra("pred", prediction);
                startActivity(resultView);
            }
        });
    }

    private void showError(final String error) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                statusText.setText(error);
                statusText.setVisibility(View.VISIBLE);
            }
        });
    }

    private Bitmap loadImageFromAssets(String fileName) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = getAssets().open(fileName);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeStream(inputStream, null, options);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Lỗi khi đóng inputStream", e);
                }
            }
        }
    }

    private Bitmap resizeBitmap(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();

        float scale = Math.min(
                (float) MAX_IMAGE_DIMENSION / width,
                (float) MAX_IMAGE_DIMENSION / height);

        if (scale >= 1) return original;

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    private String saveBitmapToTempFile(Bitmap bitmap) throws IOException {
        File tempFile = File.createTempFile("temp_image", ".jpg", getCacheDir());
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    Log.e(TAG, "Lỗi khi đóng FileOutputStream", e);
                }
            }
        }
        return tempFile.getAbsolutePath();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}