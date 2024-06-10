package com.gemx.gemx;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        Button loginBtn = findViewById(R.id.btn_login);
        TextView createBtn = findViewById(R.id.create);
        TextView forgotBtn = findViewById(R.id.tv_forgot_password);
        ImageView backBtn = findViewById(R.id.back);

        backBtn.setOnClickListener(v-> finish());

        createBtn.setOnClickListener(v->{
            Intent i = new Intent(this,SignupActivity.class);
            startActivity(i);
        });

        forgotBtn.setOnClickListener(v->{
            Intent i = new Intent(this,ForgotPasswordActivity.class);
            startActivity(i);
        });

        loginBtn.setOnClickListener(v->{
            Intent i = new Intent(this,StartConversationActivity.class);
            startActivity(i);
            finish();
        });
    }
}