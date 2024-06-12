package com.gemx.gemx;

import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ChattingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chatting);
        TextView gradientTextView = findViewById(R.id.anything);
        Shader textShader = new LinearGradient(0, 0, 0, gradientTextView.getTextSize(),
                new int[]{
                        Color.parseColor("#3A59C7"),
                        Color.parseColor("#FFFFFF")
                }, null, Shader.TileMode.CLAMP);
        gradientTextView.getPaint().setShader(textShader);
        gradientTextView.invalidate();


        ImageView backBtn = findViewById(R.id.back);

        backBtn.setOnClickListener(v-> finish());

    }
}