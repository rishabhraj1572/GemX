package com.gemx.gemx;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
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
        setContentView(R.layout.activity_h2);

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
