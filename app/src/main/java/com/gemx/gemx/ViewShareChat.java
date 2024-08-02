package com.gemx.gemx;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gemx.gemx.Adapters.ChatItemAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ViewShareChat extends AppCompatActivity {

    RecyclerView chatsView;
    List<String> sender, receiver,imageUrlList;
    private ChatItemAdapter itemAdapter;
    private FirebaseFirestore db;
    private boolean isFetchingData = false;
    List<Long> ids;
    private int totalMessagesToLoad;
    private int messagesLoadedCount;
    FirebaseStorage storage;
    LinearLayoutManager layoutManager;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_chat);

        progressDialog = new ProgressDialog(this,R.style.MyAlertDialogStyle);
        progressDialog.setTitle("Please Wait");
        progressDialog.setMessage("Verifying...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        chatsView = findViewById(R.id.chatsRecyclerView);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatsView.setLayoutManager(layoutManager);

        sender = new ArrayList<>();
        receiver = new ArrayList<>();
        imageUrlList = new ArrayList<>();
        itemAdapter = new ChatItemAdapter(this,sender, receiver,imageUrlList);
        chatsView.setAdapter(itemAdapter);
        ids = new ArrayList<>();

        ImageView backBtn = findViewById(R.id.back);
        backBtn.setOnClickListener(v -> finish());

        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            String historyId = data.getQueryParameter("id");

            if(historyId!=null){
                retrieveHistory(historyId);
                //sharing
                ImageView shareBtn = findViewById(R.id.share_btn);
                shareBtn.setOnClickListener(v-> shareLink(historyId));
            }else{
                Toast.makeText(this, "Invalid Url", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Invalid Url", Toast.LENGTH_SHORT).show();
            finish();
        }


    }

    private void shareLink(String id) {

        String urlToShare = "https://gemx.infinityfreeapp.com/chat.php?id="+ id;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, urlToShare);

        startActivity(Intent.createChooser(shareIntent, "Share Via"));
    }

    private void retrieveHistory(String historyId) {
        if (!isFetchingData) {
            isFetchingData = true;
            sender.clear();
            receiver.clear();
            imageUrlList.clear();
            ids.clear();
            db.collection("chats").document(historyId).collection("messsages")
                    .get()
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {
                            totalMessagesToLoad = task.getResult().size(); // total messages
                            messagesLoadedCount = 0; // reset messages count

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String id = document.getId();
                                ids.add(Long.valueOf(id));
                                Log.d("IDS", document.getId());
                            }

                            if(ids.isEmpty()){
                                Toast.makeText(this, "Data Not Found", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                            // Sort IDs in ascending order
                            Collections.sort(ids);
                            // Start loading messages
                            loadMessagesRecursively(historyId, 0);
                            progressDialog.dismiss();
                        } else {
                            Log.w("Firestore", "Error getting collections.", task.getException());
                            isFetchingData = false;
                            progressDialog.dismiss();
                        }
                    });
        }
    }

    private void loadMessagesRecursively(String historyId, int index) {
        if (index < ids.size()) {
            String id = String.valueOf(ids.get(index));
            loadMessages(historyId, id, () -> {
                // Load next message recursively
                loadMessagesRecursively(historyId, index + 1);
            });
        } else {
            // All loaded
            isFetchingData = false;
        }
    }

    private void loadMessages(String historyId, String id, Runnable onComplete) {
        db.collection("chats").document(historyId).collection("messsages").document(id)
                .get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        String msgSent = task.getResult().getString("message_sent");
                        String msgReceived = task.getResult().getString("message_received");
                        String imageLink = task.getResult().getString("imageUrl");

                        Log.d("MSG DATA", msgSent + ":" + msgReceived);

                        sender.add(msgSent);
                        receiver.add(msgReceived);
                        imageUrlList.add(imageLink);
                        itemAdapter.notifyItemInserted(sender.size() - 1);

                        onComplete.run();

                        messagesLoadedCount++;

                    } else {
                        Log.w("Firestore", "Error getting collections.", task.getException());
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

}
