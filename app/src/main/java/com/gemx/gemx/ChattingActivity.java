package com.gemx.gemx;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.gemx.gemx.Adapters.ChatItemAdapter;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChattingActivity extends AppCompatActivity implements ChatItemAdapter.OnItemClickListener {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final int REQUEST_CODE_READ_STORAGE = 101;
    RecyclerView chatsView;
    List<String> sender, receiver,imageUrlList;
    private ChatItemAdapter itemAdapter;
    List<Content> history;
    GenerativeModel gm;
    TextView suggestion1,suggestion2,suggestion3,suggestion4;
    EditText messageEdit;
    private FirebaseFirestore db;
    FirebaseUser user;
    private FirebaseAuth mAuth;
    long epochTime;
    private boolean isFetchingData = false;
    TextView Texthello,Textanything;
    ScrollView scrollView;
    List<Long> ids;
    private int totalMessagesToLoad;
    private int messagesLoadedCount;
    String epch;
    LinearLayoutManager layoutManager;
    FirebaseStorage storage;
    Uri imageUri = null;
    ImageView previewImg;
    FrameLayout frameLayout;
    String chatId;
    private long epochTime1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatting);

        Texthello = findViewById(R.id.textViewhello);
        Textanything = findViewById(R.id.anything);
        suggestion1 = findViewById(R.id.suggestion1);
        suggestion2 = findViewById(R.id.suggestion2);
        suggestion3 = findViewById(R.id.suggestion3);
        suggestion4 = findViewById(R.id.suggestion4);
        Shader textShader = new LinearGradient(0, 0, 0, Textanything.getTextSize(),
                new int[]{
                        Color.parseColor("#3A59C7"),
                        Color.parseColor("#FFFFFF")
                }, null, Shader.TileMode.CLAMP);
        Textanything.getPaint().setShader(textShader);
        Textanything.invalidate();

        //chatting view components
        epochTime = System.currentTimeMillis();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        user = mAuth.getCurrentUser();
        chatsView = findViewById(R.id.chatsRecyclerView);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        chatsView.setLayoutManager(layoutManager);
        scrollView = findViewById(R.id.scroll);
        messageEdit = findViewById(R.id.editTextMessage);
        messageEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        ImageView sendBtn = findViewById(R.id.sendBtn);
        ImageView backBtn = findViewById(R.id.back);
        ImageView addImage = findViewById(R.id.addImg);
        ImageView cancelImg = findViewById(R.id.cancel_image);
        frameLayout = findViewById(R.id.frameLayout);
        frameLayout.bringToFront();
        previewImg = findViewById(R.id.attachedImage);

        cancelImg.setOnClickListener(v->{
            Log.d("Cancel","Cliked Cancel Button");
            frameLayout.setVisibility(View.GONE);
            imageUri = null;
        });

        //attaching image
        addImage.setOnClickListener(v-> checkAndRequestPermissions());


        //chatting components
        gm = new GenerativeModel("gemini-1.5-flash",AppConfig.geminiAPIkey);
        sender = new ArrayList<>();
        receiver = new ArrayList<>();
        imageUrlList = new ArrayList<>();
        itemAdapter = new ChatItemAdapter(ChattingActivity.this,sender, receiver,imageUrlList);
        itemAdapter.setOnItemClickListener(this);
        chatsView.setAdapter(itemAdapter);

        ids = new ArrayList<>();
        //getting data from HomeActivity for fetching history
        Intent intent = getIntent();
        chatId = intent.getStringExtra("historyItemId");
        if(chatId!=null){
            Log.d("History Chat id",chatId);
            epch = chatId.split("_")[1];
            retrieveHistory(chatId);
        }else {
            Log.d("History Chat id","New Item");
        }

        //send button
        sendBtn.setOnClickListener(v -> {
            String message = messageEdit.getText().toString().trim();
            if(chatId!=null){
                epochTime1 = System.currentTimeMillis();
                sendMessageToGeminiExistingChat(message,epochTime1);
            }else {
                epochTime1 = System.currentTimeMillis();
                sendMessageToGemini(message,epochTime1);
            }
        });

        backBtn.setOnClickListener(v -> finish());


        //sharing
        ImageView shareBtn = findViewById(R.id.share_btn);
        shareBtn.setOnClickListener(v->{
            if(sender.isEmpty() && receiver.isEmpty()){
                Toast.makeText(this, "Nothing to Share", Toast.LENGTH_SHORT).show();
            }else{
                if(chatId!=null){
                    epch = chatId.split("_")[1];
                    shareLink(epch);
                }else {
                    shareLink(String.valueOf(epochTime));
                }
            }
        });


        //all suggestion
        suggestion1.setOnClickListener(v->{
            String s = suggestion1.getText().toString().replace("●   ", "");
            imageUri =null;
            epochTime1 = System.currentTimeMillis();
            sendMessageToGemini(s,epochTime1);
        });
        suggestion2.setOnClickListener(v->{
            String s = suggestion2.getText().toString().replace("●   ","");
            imageUri =null;
            epochTime1 = System.currentTimeMillis();
            sendMessageToGemini(s,epochTime1);
        });
        suggestion3.setOnClickListener(v->{
            String s = suggestion3.getText().toString().replace("●   ","");
            imageUri =null;
            epochTime1 = System.currentTimeMillis();
            sendMessageToGemini(s,epochTime1);
        });
        suggestion4.setOnClickListener(v->{
            String s = suggestion4.getText().toString().replace("●   ","");
            imageUri =null;
            epochTime1 = System.currentTimeMillis();
            sendMessageToGemini(s,epochTime1);
        });
    }


    private void shareLink(String id) {
        String userUID = user.getUid();

        String urlToShare = "https://gemxapp.000webhostapp.com/chat.php?id="+userUID + "_" + id;
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, urlToShare);

        startActivity(Intent.createChooser(shareIntent, "Share Via"));
    }

    private void retrieveHistory(String historyId) {

        // Update UI visibility
        scrollView.setVisibility(View.GONE);
        chatsView.setVisibility(View.VISIBLE);
        Texthello.setVisibility(View.GONE);
        Textanything.setVisibility(View.GONE);

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
                            // Sort IDs in ascending order
                            Collections.sort(ids);
                            if(epochTime1 == 0){
                                epochTime1 = ids.get(ids.size() - 1);
                                System.out.println(epochTime1);
                            }
                            // Start loading messages
                            loadMessagesRecursively(historyId, 0);
                        } else {
                            Log.w("Firestore", "Error getting collections.", task.getException());
                            isFetchingData = false;
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
            // Display
            chatsView.setVisibility(View.VISIBLE);
            chatsView.scrollToPosition(receiver.size() - 1);
            Handler handler = new Handler(Looper.getMainLooper());
            handler.postDelayed(() -> {
                itemAdapter.updateLastItemPosition(receiver.size() - 1);
            }, 500);
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

                        onComplete.run(); //message loading complete

                        // Increment messages count
                        messagesLoadedCount++;


                        //history added to model
                        if (history == null) {
                            history = new ArrayList<>();
                        }
                        Content.Builder userMessageBuilder = new Content.Builder();
                        userMessageBuilder.setRole("user");
                        userMessageBuilder.addText(msgSent);
                        //image thing remaining
                        Content userMessage = userMessageBuilder.build();
                        history.add(userMessage);
                        Content.Builder modelResponse = new Content.Builder();
                        modelResponse.setRole("model");
                        modelResponse.addText(msgReceived);
                        history.add(modelResponse.build());


                        //show RecyclerView and scroll to last item
                        if (messagesLoadedCount == totalMessagesToLoad) {
                            chatsView.setVisibility(View.VISIBLE);
                            chatsView.scrollToPosition(receiver.size() - 1);
                        }
                    } else {
                        Log.w("Firestore", "Error getting collections.", task.getException());
                    }
                });
    }

    private void sendMessageToGemini1(String message) {

        if (message.isEmpty()) {
            return;
        }

        Content.Builder userMessageBuilder = new Content.Builder();
        userMessageBuilder.setRole("user");
        userMessageBuilder.addText(message);
        Content userMessage = userMessageBuilder.build();

        // Start a chat session with history
        if (history == null) {
            history = new ArrayList<>();
        }
        history.add(userMessage);
        ChatFutures chat = ChatFutures.from(gm.startChat(history));

        sender.add(message);
        receiver.add("Waiting For Response");
        imageUrlList.add("na");
        chatsView.smoothScrollToPosition(receiver.size() - 1);
        itemAdapter.notifyDataSetChanged();

        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = chat.sendMessage(userMessage);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                String resultText1 = resultText.replace("**","").replace("*","●").replace("##","");
                Log.d("Gemini Response", resultText);
                runOnUiThread(() -> {
                    receiver.set(receiver.size() - 1, resultText1);
                    itemAdapter.notifyDataSetChanged();
                    chatsView.smoothScrollToPosition(receiver.size() - 1);
                    Content.Builder modelResponse = new Content.Builder();
                    modelResponse.setRole("model");
                    modelResponse.addText(resultText1);
                    history.add(modelResponse.build());

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("chat_name", sender.get(0));
                    userData.put("last_update",epochTime);


                    String userUID = user.getUid();
                    db.collection("chats").document(userUID+"_"+epochTime)
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Save Status","Details Saved");

                            })
                            .addOnFailureListener(e -> {
                                Log.d("Save Status","Not Saved");
                            });

                    Map<String, Object> messageData = new HashMap<>();
                    messageData.put("message_sent", message);
                    messageData.put("message_received",resultText1);
                    long epochTime1 = System.currentTimeMillis();

                    db.collection("chats").document(userUID+"_"+epochTime)
                            .collection("messsages").document(String.valueOf(epochTime1))
                            .set(messageData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Save Status","Saved");
                            })
                            .addOnFailureListener(e -> {
                                Log.d("Save Status","Not Saved");
                            });


                });
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        }, executor);

        messageEdit.setText("");

        // Update UI visibility
        scrollView.setVisibility(View.GONE);
        chatsView.setVisibility(View.VISIBLE);
        Texthello.setVisibility(View.GONE);
        Textanything.setVisibility(View.GONE);
    }
    private void sendMessageToGemini(String message, long epochTime1) {
        if (message.isEmpty()) {
            return;
        }

        Content.Builder userMessageBuilder = new Content.Builder();
        userMessageBuilder.addText(message);
        userMessageBuilder.setRole("user");
        if(imageUri != null){
            frameLayout.setVisibility(View.GONE);
            try{
                userMessageBuilder.addImage(getBitmapFromUri(imageUri));
                imageUrlList.add(String.valueOf(imageUri));
            }catch (Exception ignored) {
            }
        }else{
            imageUrlList.add("na");
        }
        Content userMessage = userMessageBuilder.build();


        // Start a chat session with history
        if (history == null) {
            history = new ArrayList<>();
        }
        history.add(userMessage);
        ChatFutures chat = ChatFutures.from(gm.startChat(history));

        Publisher<GenerateContentResponse> streamingResponse = chat.sendMessageStream(userMessage);

        sender.add(message);
        receiver.add("Waiting For Response...");
//        imageUrlList.add("na");
        chatsView.smoothScrollToPosition(receiver.size() - 1);
        itemAdapter.notifyDataSetChanged();

        StringBuilder outputContent = new StringBuilder();
        StringBuilder msgBuilder = new StringBuilder();
        streamingResponse.subscribe(new Subscriber<GenerateContentResponse>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(GenerateContentResponse generateContentResponse) {
                String chunk = generateContentResponse.getText();
                String transformedChunk = chunk.replace("**", "").replace("*", "●").replace("##", "");
                msgBuilder.append(transformedChunk);
                String currentMessage = msgBuilder.toString();
                Log.d("Chunk", chunk);

                runOnUiThread(() -> {
                    receiver.set(receiver.size() - 1, currentMessage);
                    itemAdapter.notifyDataSetChanged();
                    itemAdapter.updateLastItemPosition(receiver.size() - 1);
                    chatsView.scrollToPosition(receiver.size() - 1); // Scroll to the bottom
                });

                outputContent.append(transformedChunk);
            }

            @Override
            public void onComplete() {
                String resultText = outputContent.toString();
                String resultText1 = resultText.replace("**", "").replace("*", "●").replace("##", "");
                Log.d("Gemini Response", resultText);

                runOnUiThread(() -> {
                    receiver.set(receiver.size() - 1, resultText1);
                    itemAdapter.notifyDataSetChanged();
//                    chatsView.smoothScrollToPosition(receiver.size() - 1);

                    Content.Builder modelResponse = new Content.Builder();
                    modelResponse.setRole("model");
                    modelResponse.addText(resultText1);
                    history.add(modelResponse.build());

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("chat_name", sender.get(0));
                    userData.put("last_update", epochTime);

                    String userUID = user.getUid();
                    db.collection("chats").document(userUID + "_" + epochTime)
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Save Status", "Details Saved");
                            })
                            .addOnFailureListener(e -> {
                                Log.d("Save Status", "Not Saved");
                            });

                    Map<String, Object> messageData = new HashMap<>();
                    messageData.put("message_sent", message);
                    messageData.put("message_received", resultText1);

                    if(imageUri != null){
                        if (user != null) {
                            String fileName = userUID + "_" + epochTime + "_" + epochTime1;
                            StorageReference storageReference = FirebaseStorage.getInstance().getReference("chatImages/" + fileName);

                            try{
                                UploadTask uploadTask = storageReference.putFile(imageUri);
                                uploadTask.addOnSuccessListener(taskSnapshot -> {
                                    imageUri = null;
//                                Toast.makeText(ChattingActivity.this, "Upload successful", Toast.LENGTH_SHORT).show();
                                    StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                                    StorageReference dateRef = storageRef.child("chatImages/" + userUID + "_" + epochTime + "_" + epochTime1);

                                    dateRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                        String link = uri.toString();
                                        System.out.println(link);
//                                    imageUrlList.set(imageUrlList.size() - 1, link);
//                                    itemAdapter.notifyDataSetChanged();
                                        messageData.put("imageUrl", link);
                                        db.collection("chats").document(userUID + "_" + epochTime)
                                                .collection("messsages").document(String.valueOf(epochTime1))
                                                .set(messageData)
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d("Save Status", "Saved");
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.d("Save Status", "Not Saved");
                                                });

                                    }).addOnFailureListener(exception -> {
                                        System.err.println("Error getting download URL: " + exception.getMessage());
                                        messageData.put("imageUrl", "na");
                                        db.collection("chats").document(userUID + "_" + epochTime)
                                                .collection("messsages").document(String.valueOf(epochTime1))
                                                .set(messageData)
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d("Save Status", "Saved");
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.d("Save Status", "Not Saved");
                                                });

                                    });
                                }).addOnFailureListener(e -> {
                                    Toast.makeText(ChattingActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    messageData.put("imageUrl", "na");
                                    db.collection("chats").document(userUID + "_" + epochTime)
                                            .collection("messsages").document(String.valueOf(epochTime1))
                                            .set(messageData)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d("Save Status", "Saved");
                                            })
                                            .addOnFailureListener(e1 -> {
                                                Log.d("Save Status", "Not Saved");
                                            });

                                });
                            }catch (Exception ignored){}
                        } else {
                            Toast.makeText(ChattingActivity.this, "User not authenticated", Toast.LENGTH_SHORT).show();
                            messageData.put("imageUrl","na");
                            db.collection("chats").document(userUID + "_" + epochTime)
                                    .collection("messsages").document(String.valueOf(epochTime1))
                                    .set(messageData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Save Status", "Saved");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.d("Save Status", "Not Saved");
                                    });

                        }

                    }else{
                        messageData.put("imageUrl","na");
                        db.collection("chats").document(userUID + "_" + epochTime)
                                .collection("messsages").document(String.valueOf(epochTime1))
                                .set(messageData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("Save Status", "Saved");
                                })
                                .addOnFailureListener(e -> {
                                    Log.d("Save Status", "Not Saved");
                                });

                    }

                });
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }
        });

        messageEdit.setText("");

        // Update UI visibility
        scrollView.setVisibility(View.GONE);
        chatsView.setVisibility(View.VISIBLE);
        Texthello.setVisibility(View.GONE);
        Textanything.setVisibility(View.GONE);
    }

    private void sendMessageToGeminiExistingChat(String message, long epochTime1) {
        if (message.isEmpty()) {
            return;
        }

        Content.Builder userMessageBuilder = new Content.Builder();
        userMessageBuilder.addText(message);
        userMessageBuilder.setRole("user");
        if(imageUri != null){
            frameLayout.setVisibility(View.GONE);
            try{
                userMessageBuilder.addImage(getBitmapFromUri(imageUri));
                imageUrlList.add(String.valueOf(imageUri));
            }catch (Exception ignored) {
            }
        }else{
            imageUrlList.add("na");
        }
        Content userMessage = userMessageBuilder.build();


        // Start a chat session with history
        if (history == null) {
            history = new ArrayList<>();
        }
        history.add(userMessage);
        ChatFutures chat = ChatFutures.from(gm.startChat(history));

        Publisher<GenerateContentResponse> streamingResponse = chat.sendMessageStream(userMessage);

        sender.add(message);
        receiver.add("Waiting For Response...");
//        imageUrlList.add("na");
        chatsView.smoothScrollToPosition(receiver.size() - 1);
        itemAdapter.notifyDataSetChanged();


        StringBuilder outputContent = new StringBuilder();
        StringBuilder msgBuilder = new StringBuilder();

        streamingResponse.subscribe(new Subscriber<GenerateContentResponse>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(GenerateContentResponse generateContentResponse) {
                String chunk = generateContentResponse.getText();
                String transformedChunk = chunk.replace("**", "").replace("*", "●").replace("##", "");
                msgBuilder.append(transformedChunk);
                String currentMessage = msgBuilder.toString();
                Log.d("Chunk", chunk);

                runOnUiThread(() -> {
                    receiver.set(receiver.size() - 1, currentMessage);
                    itemAdapter.notifyDataSetChanged();
                    itemAdapter.updateLastItemPosition(receiver.size() - 1);
                    chatsView.scrollToPosition(receiver.size() - 1); // Scroll to the bottom
                });

                outputContent.append(transformedChunk);
            }

            @Override
            public void onComplete() {
                String resultText = outputContent.toString();
                String resultText1 = resultText.replace("**", "").replace("*", "●").replace("##", "");
                Log.d("Gemini Response", resultText);

                runOnUiThread(() -> {
                    receiver.set(receiver.size() - 1, resultText1);
                    itemAdapter.notifyDataSetChanged();
//                    chatsView.smoothScrollToPosition(receiver.size() - 1);
                    Content.Builder modelResponse = new Content.Builder();
                    modelResponse.setRole("model");
                    modelResponse.addText(resultText1);
                    history.add(modelResponse.build());

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("chat_name", sender.get(0));
                    userData.put("last_update",epochTime);
                    String userUID = user.getUid();
                    db.collection("chats").document(userUID+"_"+epch)
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Save Status","Details Saved");

                            })
                            .addOnFailureListener(e -> {
                                Log.d("Save Status","Not Saved");
                            });
                    Map<String, Object> messageData = new HashMap<>();
                    messageData.put("message_sent", message);
                    messageData.put("message_received",resultText1);

                    if(imageUri != null){
                        if (user != null) {
                            String fileName = userUID + "_" + epch + "_" + epochTime1;
                            StorageReference storageReference = FirebaseStorage.getInstance().getReference("chatImages/" + fileName);

                            try{
                                UploadTask uploadTask = storageReference.putFile(imageUri);
                                uploadTask.addOnSuccessListener(taskSnapshot -> {
                                    imageUri = null;
//                                Toast.makeText(ChattingActivity.this, "Upload successful", Toast.LENGTH_SHORT).show();
                                    StorageReference storageRef = FirebaseStorage.getInstance().getReference();
                                    StorageReference dateRef = storageRef.child("chatImages/" + userUID + "_" + epch + "_" + epochTime1);

                                    dateRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                        String link = uri.toString();
                                        System.out.println(link);
//                                    imageUrlList.set(imageUrlList.size() - 1, link);
//                                    itemAdapter.notifyDataSetChanged();
                                        messageData.put("imageUrl", link);
                                        db.collection("chats").document(userUID + "_" + epch)
                                                .collection("messsages").document(String.valueOf(epochTime1))
                                                .set(messageData)
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d("Save Status", "Saved");
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.d("Save Status", "Not Saved");
                                                });

                                    }).addOnFailureListener(exception -> {
                                        System.err.println("Error getting download URL: " + exception.getMessage());
                                        messageData.put("imageUrl", "na");
                                        db.collection("chats").document(userUID + "_" + epch)
                                                .collection("messsages").document(String.valueOf(epochTime1))
                                                .set(messageData)
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d("Save Status", "Saved");
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.d("Save Status", "Not Saved");
                                                });

                                    });
                                }).addOnFailureListener(e -> {
                                    Toast.makeText(ChattingActivity.this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    messageData.put("imageUrl", "na");
                                    db.collection("chats").document(userUID + "_" + epch)
                                            .collection("messsages").document(String.valueOf(epochTime1))
                                            .set(messageData)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d("Save Status", "Saved");
                                            })
                                            .addOnFailureListener(e1 -> {
                                                Log.d("Save Status", "Not Saved");
                                            });

                                });
                            }catch (Exception ignored){}
                        } else {
                            Toast.makeText(ChattingActivity.this, "User not authenticated", Toast.LENGTH_SHORT).show();
                            messageData.put("imageUrl","na");
                            db.collection("chats").document(userUID + "_" + epch)
                                    .collection("messsages").document(String.valueOf(epochTime1))
                                    .set(messageData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Save Status", "Saved");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.d("Save Status", "Not Saved");
                                    });

                        }

                    }else{
                        messageData.put("imageUrl","na");
                        db.collection("chats").document(userUID + "_" + epch)
                                .collection("messsages").document(String.valueOf(epochTime1))
                                .set(messageData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("Save Status", "Saved");
                                })
                                .addOnFailureListener(e -> {
                                    Log.d("Save Status", "Not Saved");
                                });

                    }


                    /*db.collection("chats").document(userUID+"_"+epch)
                            .collection("messsages").document(String.valueOf(epochTime1))
                            .set(messageData)
                            .addOnSuccessListener(aVoid -> {
                                Log.d("Save Status","Saved");
                            })
                            .addOnFailureListener(e -> {
                                Log.d("Save Status","Not Saved");
                            });*/
                });
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }
        });

        messageEdit.setText("");

        // Update UI visibility
        scrollView.setVisibility(View.GONE);
        chatsView.setVisibility(View.VISIBLE);
        Texthello.setVisibility(View.GONE);
        Textanything.setVisibility(View.GONE);
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }
    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        return BitmapFactory.decodeStream(inputStream);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            imageUri = data.getData();
            frameLayout.setVisibility(View.VISIBLE);
//            Picasso.get().load(imageUri).into(previewImg);
            Glide.with(this)
                    .load(imageUri)
                    .into(previewImg);
//            uploadImageToFirebase(imageUri);
        }
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT <= 32) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_READ_STORAGE);
            } else {
                // Permission already granted
                pickImage();
            }
        } else if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES)) {
                    Toast.makeText(this, "Storage permission is needed to pick an image", Toast.LENGTH_LONG).show();
                }
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_REQUEST_CODE);
            }else{
                pickImage();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                // Permission denied
//                Log.d(TAG, "Permission denied");
                Toast.makeText(this, "Permission denied. Cannot pick image.", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == REQUEST_CODE_READ_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                // Permission denied
                Toast.makeText(this, "Read storage permission denied", Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    public void onRefresh(int position) {
        refreshResponse(position);
    }

    private void refreshResponse(int position) {
        String message = sender.get(position);
        if(chatId!=null){
            int Last = sender.size() - 1;
            history.remove(Last);
            sender.remove(Last);
            imageUrlList.remove(Last);
            receiver.remove(Last);
            sendMessageToGeminiExistingChat(message,epochTime1);

        }else {
            int Last = sender.size() - 1;
            history.remove(Last);
            sender.remove(Last);
            imageUrlList.remove(Last);
            receiver.remove(Last);
            sendMessageToGemini(message,epochTime1);
        }
    }


}
