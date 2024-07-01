package com.gemx.gemx;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class EmailVerified extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verified_page);


        Button continueBtn = findViewById(R.id.verify_btn);
        ImageView back = findViewById(R.id.back);

        back.setOnClickListener(v->finish());

        continueBtn.setOnClickListener(v->{
            startActivity(new Intent(this,StartConversationActivity.class));
            finishAffinity();
        });
    }
}
