package com.gemx.gemx;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gemx.gemx.Adapters.ChatItemAdapter;
import com.google.ai.client.generativeai.Chat;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChattingActivity extends AppCompatActivity {

    RecyclerView chatsView;
    List<String> sender, receiver;
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
        TextView gradientTextView = findViewById(R.id.anything);
        Shader textShader = new LinearGradient(0, 0, 0, gradientTextView.getTextSize(),
                new int[]{
                        Color.parseColor("#3A59C7"),
                        Color.parseColor("#FFFFFF")
                }, null, Shader.TileMode.CLAMP);
        gradientTextView.getPaint().setShader(textShader);
        gradientTextView.invalidate();

        //chatting view components
        epochTime = System.currentTimeMillis();
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
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

        //chatting componentsh
        gm = new GenerativeModel("gemini-1.5-flash",AppConfig.geminiAPIkey);
        sender = new ArrayList<>();
        receiver = new ArrayList<>();
        itemAdapter = new ChatItemAdapter(sender, receiver);
        chatsView.setAdapter(itemAdapter);

        ids = new ArrayList<>();
        //getting data from HomeActivity for fetching history
        Intent intent = getIntent();
        String chatId = intent.getStringExtra("historyItemId");
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
                sendMessageToGeminiExistingChat(message);
            }else {
                sendMessageToGemini(message);
            }
        });

        backBtn.setOnClickListener(v -> finish());


        //all suggestion
        suggestion1.setOnClickListener(v->{
            String s = suggestion1.getText().toString().replace("●   ", "");
            sendMessageToGemini(s);
        });
        suggestion2.setOnClickListener(v->{
            String s = suggestion2.getText().toString().replace("●   ","");
            sendMessageToGemini(s);
        });
        suggestion3.setOnClickListener(v->{
            String s = suggestion3.getText().toString().replace("●   ","");
            sendMessageToGemini(s);
        });
        suggestion4.setOnClickListener(v->{
            String s = suggestion4.getText().toString().replace("●   ","");
            sendMessageToGemini(s);
        });
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
            ids.clear(); // Clearing IDs list before fetching
            db.collection("chats").document(historyId).collection("messsages")
                    .get()
                    .addOnCompleteListener(task -> {

                        if (task.isSuccessful()) {
                            totalMessagesToLoad = task.getResult().size(); // Total messages
                            messagesLoadedCount = 0; // Reset messages count

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String id = document.getId();
                                ids.add(Long.valueOf(id));
                                Log.d("IDS", document.getId());
                            }
                            // Sort IDs in ascending order
                            Collections.sort(ids);
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
        }
    }

    private void loadMessages(String historyId, String id, Runnable onComplete) {
        db.collection("chats").document(historyId).collection("messsages").document(id)
                .get()
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        String msgSent = task.getResult().getString("message_sent");
                        String msgReceived = task.getResult().getString("message_received");

                        Log.d("MSG DATA", msgSent + ":" + msgReceived);

                        sender.add(msgSent);
                        receiver.add(msgReceived);
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
    private void sendMessageToGemini(String message) {
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

        Publisher<GenerateContentResponse> streamingResponse = chat.sendMessageStream(userMessage);

        sender.add(message);
        receiver.add("Waiting For Response");
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
                    long epochTime1 = System.currentTimeMillis();

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

    private void sendMessageToGeminiExistingChat(String message) {

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

        Publisher<GenerateContentResponse> streamingResponse = chat.sendMessageStream(userMessage);

        sender.add(message);
        receiver.add("Waiting For Response... ");
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
                    long epochTime1 = System.currentTimeMillis();

                    db.collection("chats").document(userUID+"_"+epch)
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
            public void onError(Throwable t) {
                t.printStackTrace();
            }
        });

       /* Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
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
                    long epochTime1 = System.currentTimeMillis();

                    db.collection("chats").document(userUID+"_"+epch)
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
        }, executor);*/

        messageEdit.setText("");

        // Update UI visibility
        scrollView.setVisibility(View.GONE);
        chatsView.setVisibility(View.VISIBLE);
        Texthello.setVisibility(View.GONE);
        Textanything.setVisibility(View.GONE);
    }

}

