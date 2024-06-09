package com.gemx.gemx;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ForgotPasswordActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot);

        TextView signinBtn = findViewById(R.id.signin);
        TextView back_to_login = findViewById(R.id.back_to_login);
        ImageView backBtn = findViewById(R.id.back);
        Button sendOTP = findViewById(R.id.send_btn);

        backBtn.setOnClickListener(v-> onBackPressed());
        back_to_login.setOnClickListener(v->onBackPressed());
        signinBtn.setOnClickListener(v-> onBackPressed());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
