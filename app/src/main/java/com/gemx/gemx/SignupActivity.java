package com.gemx.gemx;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


public class SignupActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        TextView signinBtn = findViewById(R.id.signin);
        Button sendOTP = findViewById(R.id.send_btn);
        ImageView backBtn = findViewById(R.id.back);

        backBtn.setOnClickListener(v-> finish());

        signinBtn.setOnClickListener(v-> onBackPressed());

        sendOTP.setOnClickListener(v->{
            Intent i = new Intent(this,OTPActivity.class);
            startActivity(i);
        });

        EditText userName = findViewById(R.id.et_username);
        EditText userId = findViewById(R.id.et_phone_email);
        EditText userPass = findViewById(R.id.et_password);

        userId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // No need
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (isValidEmail(charSequence.toString())) {
                    userPass.setVisibility(View.VISIBLE);
                    sendOTP.setText("Continue");
                } else {
                    userPass.setVisibility(View.GONE);
                    sendOTP.setText("Send OTP");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // No need
            }
        });

    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
