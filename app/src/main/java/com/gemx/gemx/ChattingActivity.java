package com.gemx.gemx;

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

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChattingActivity extends AppCompatActivity {

    RecyclerView chatsView;
    List<String> sender, receiver;
    private ChatItemAdapter itemAdapter;
    List<Content> history;
    GenerativeModel gm;
    TextView suggestion1,suggestion2,suggestion3,suggestion4,suggestion5;
    EditText messageEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatting);

        TextView Texthello = findViewById(R.id.textViewhello);
        TextView Textanything = findViewById(R.id.anything);
        suggestion1 = findViewById(R.id.suggestion1);
        suggestion2 = findViewById(R.id.suggestion2);
        suggestion3 = findViewById(R.id.suggestion3);
        suggestion4 = findViewById(R.id.suggestion4);
        suggestion5 = findViewById(R.id.suggestion5);
        TextView gradientTextView = findViewById(R.id.anything);
        Shader textShader = new LinearGradient(0, 0, 0, gradientTextView.getTextSize(),
                new int[]{
                        Color.parseColor("#3A59C7"),
                        Color.parseColor("#FFFFFF")
                }, null, Shader.TileMode.CLAMP);
        gradientTextView.getPaint().setShader(textShader);
        gradientTextView.invalidate();


        //chatting view components
        chatsView = findViewById(R.id.chatsRecyclerView);
        chatsView.setLayoutManager(new LinearLayoutManager(this));
        ScrollView scrollView = findViewById(R.id.scroll);
        messageEdit = findViewById(R.id.editTextMessage);
        messageEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        ImageView sendBtn = findViewById(R.id.sendBtn);
        ImageView backBtn = findViewById(R.id.back);

        //chatting components
        gm = new GenerativeModel("gemini-1.5-flash",AppConfig.geminiAPIkey);
        sender = new ArrayList<>();
        receiver = new ArrayList<>();
        itemAdapter = new ChatItemAdapter(sender, receiver);
        chatsView.setAdapter(itemAdapter);

        //send button
        sendBtn.setOnClickListener(v -> {
            String message = messageEdit.getText().toString().trim();
            sendMessageToGemini(message,scrollView,Texthello,Textanything);
        });

        backBtn.setOnClickListener(v -> finish());


        //all suggestion
        suggestion1.setOnClickListener(v->{
            String s = suggestion1.getText().toString().replace("●   ", "");
            sendMessageToGemini(s,scrollView,Texthello,Textanything);
        });
        suggestion2.setOnClickListener(v->{
            String s = suggestion2.getText().toString().replace("●   ","");
            sendMessageToGemini(s,scrollView,Texthello,Textanything);
        });
        suggestion3.setOnClickListener(v->{
            String s = suggestion3.getText().toString().replace("●   ","");
            sendMessageToGemini(s,scrollView,Texthello,Textanything);
        });
        suggestion4.setOnClickListener(v->{
            String s = suggestion4.getText().toString().replace("●   ","");
            sendMessageToGemini(s,scrollView,Texthello,Textanything);
        });
        suggestion5.setOnClickListener(v->{
            String s = suggestion5.getText().toString().replace("●   ","");
            sendMessageToGemini(s,scrollView,Texthello,Textanything);
        });
    }

    private void sendMessageToGemini(String message, ScrollView scrollView,TextView textHello, TextView textAnything) {

        if (message.isEmpty()) {
            return;
        }

        // content for generative model
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
        textHello.setVisibility(View.GONE);
        textAnything.setVisibility(View.GONE);
    }

}

