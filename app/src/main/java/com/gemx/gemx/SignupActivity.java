package com.gemx.gemx;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class SignupActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        TextView signinBtn = findViewById(R.id.signin);
        Button sendOTP = findViewById(R.id.send_btn);

        signinBtn.setOnClickListener(v->{
            Intent i = new Intent(this,LoginActivity.class);
            startActivity(i);
            finishAffinity();
        });

        sendOTP.setOnClickListener(v->{
            Intent i = new Intent(this,OTPActivity.class);
            startActivity(i);
        });
    }
}
