package com.gemx.gemx;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gemx.gemx.Adapters.ChatHistoryItemAdapter;
import com.gemx.gemx.RecyclerItems.ChatHistoryItems;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ChatHistoryItemAdapter itemAdapter;
    private List<ChatHistoryItems> itemList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ConstraintLayout chatWithGemx = findViewById(R.id.chatwithGemx);
        ImageView talkWithGemx = findViewById(R.id.talkWithGemX);
        chatWithGemx.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start ChattingActivity
                Intent intent = new Intent(HomeActivity.this, ChattingActivity.class);
                startActivity(intent);
            }
        });
        talkWithGemx.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TalkActivity.class);
            startActivity(intent);
        });

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        itemList = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            itemList.add(new ChatHistoryItems("Item " + i));
        }

        itemAdapter = new ChatHistoryItemAdapter(itemList);
        recyclerView.setAdapter(itemAdapter);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
