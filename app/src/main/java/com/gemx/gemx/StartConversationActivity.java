package com.gemx.gemx;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class StartConversationActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_start_conversation);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Mark that the user has seen the StartConversationActivity
        saveHasSeenStartConversation(true);

        Button start = findViewById(R.id.startconvobutton);

        start.setOnClickListener(v->{
            Intent i = new Intent(this, HomeActivity.class);
            startActivity(i);
        });
    }
    private void saveHasSeenStartConversation(boolean hasSeen) {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("has_seen_start_conversation", hasSeen)
                .apply();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}