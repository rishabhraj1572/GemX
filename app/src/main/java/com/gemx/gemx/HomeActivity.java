package com.gemx.gemx;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gemx.gemx.Adapters.ChatHistoryItemAdapter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements ChatHistoryItemAdapter.OnItemClickListener {

    private RecyclerView recyclerView;
    private ChatHistoryItemAdapter itemAdapter;
    private List<String> itemList, itemId;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private boolean isFetchingData = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initializeUI();
        setupRecyclerView();

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        ImageView menu = findViewById(R.id.menu);
        menu.setOnClickListener(v-> mAuth.signOut());

        // Fetch history data
        retrieveHistory();
    }

    private void initializeUI() {
        ConstraintLayout chatWithGemx = findViewById(R.id.chatwithGemx);
        ImageView talkWithGemx = findViewById(R.id.talkWithGemX);

        chatWithGemx.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ChattingActivity.class)));
        talkWithGemx.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, TalkActivity.class)));
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        itemList = new ArrayList<>();
        itemId = new ArrayList<>();
        itemAdapter = new ChatHistoryItemAdapter(itemList, itemId);
        itemAdapter.setOnItemClickListener(this);
        recyclerView.setAdapter(itemAdapter);
    }

    private void retrieveHistory() {
        if (!isFetchingData) {
            isFetchingData = true;
            itemList.clear();
            itemId.clear();
            db = FirebaseFirestore.getInstance();
            db.collection("chats")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            List<Pair<Long, String>> collectionList = new ArrayList<>();
                            List<Task<?>> tasks = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String collectionId = document.getId();
                                String getId = user.getUid() + "_";
                                if (collectionId.startsWith(getId)) {
                                    Task<?> docTask = db.collection("chats").document(collectionId)
                                            .get()
                                            .addOnCompleteListener(task1 -> {
                                                if (task1.isSuccessful() && task1.getResult().exists()) {
                                                    long lastUpdate = task1.getResult().getLong("last_update");
                                                    collectionList.add(new Pair<>(lastUpdate, collectionId));
                                                } else {
                                                    Log.w("Firestore", "Document does not exist or task failed: ", task1.getException());
                                                }
                                            });
                                    tasks.add(docTask);
                                }
                            }
                            Tasks.whenAllComplete(tasks).addOnCompleteListener(allTasks -> {
                                Collections.sort(collectionList, (o1, o2) -> Long.compare(o2.first, o1.first));
                                runOnUiThread(() -> startProcessing(0, collectionList));
                            });
                        } else {
                            Log.w("Firestore", "Error getting collections.", task.getException());
                        }
                        isFetchingData = false;
                    });
        }
    }

    private void startProcessing(int index, List<Pair<Long, String>> collectionList) {
        if (index < collectionList.size()) {
            String collectionId = collectionList.get(index).second;
            fetchDocument(index, collectionId, collectionList);
        } else {
            itemAdapter.notifyDataSetChanged();
        }
    }

    private void fetchDocument(int index, String collectionId, List<Pair<Long, String>> collectionList) {
        db.collection("chats").document(collectionId)
                .get()
                .addOnCompleteListener(chatTask -> {
                    if (chatTask.isSuccessful() && chatTask.getResult() != null) {
                        String chatName = chatTask.getResult().getString("chat_name");
                        itemList.add(chatName);
                        itemId.add(collectionId);
                    } else {
                        Log.w("Firestore", "Error getting document: " + collectionId, chatTask.getException());
                    }

                    if (index + 1 == collectionList.size()) {
                        runOnUiThread(() -> itemAdapter.notifyDataSetChanged());
                    } else {
                        startProcessing(index + 1, collectionList);
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
    @Override
    protected void onResume() {
        super.onResume();
            retrieveHistory();
    }


    @Override
    public void onItemClick(int position) {
        Intent i = new Intent(this, ChattingActivity.class);
        String historyItemId = itemId.get(position);
        i.putExtra("historyItemId", historyItemId);
        Log.d("Clicked Item", historyItemId);
        startActivity(i);
    }
}
