package com.aicandy.imageclassification.mobilenet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;


import java.io.File;

public class Result extends AppCompatActivity {
    private static final String TAG = "Result";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        String imagePath = getIntent().getStringExtra("image_path");
        String pred = getIntent().getStringExtra("pred");

        Log.d(TAG, "Hiển thị kết quả phân loại: " + pred);

        if (imagePath != null) {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
                ImageView imageView = findViewById(R.id.image);
                imageView.setImageBitmap(bitmap);
            } else {
                Log.e(TAG, "Không tìm thấy file ảnh");
            }
        }

        TextView textView = findViewById(R.id.label);
        textView.setText(pred);
    }
}