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
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ChattingActivity extends AppCompatActivity {

    RecyclerView chatsView;
    List<String> sender, receiver;
    private ChatItemAdapter itemAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chatting);

        // Gradient text setup
        TextView gradientTextView = findViewById(R.id.anything);
        Shader textShader = new LinearGradient(0, 0, 0, gradientTextView.getTextSize(),
                new int[]{
                        Color.parseColor("#3A59C7"),
                        Color.parseColor("#FFFFFF")
                }, null, Shader.TileMode.CLAMP);
        gradientTextView.getPaint().setShader(textShader);
        gradientTextView.invalidate();

        // Initialize RecyclerView and other views
        chatsView = findViewById(R.id.chatsRecyclerView);
        chatsView.setLayoutManager(new LinearLayoutManager(this));
        ScrollView scrollView = findViewById(R.id.scroll);
        EditText messageEdit = findViewById(R.id.editTextMessage);
        messageEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        ImageView sendBtn = findViewById(R.id.sendBtn);
        ImageView backBtn = findViewById(R.id.back);

        // Initialize data lists and adapter
        sender = new ArrayList<>();
        receiver = new ArrayList<>();
        itemAdapter = new ChatItemAdapter(sender, receiver);
        chatsView.setAdapter(itemAdapter);

        sendBtn.setOnClickListener(v -> {
            sendMessageToGemini(messageEdit,scrollView);
        });

        backBtn.setOnClickListener(v -> finish());
    }

    private void sendMessageToGemini(EditText messageEdit, ScrollView scrollView) {
        // Initialize Generative Model
        GenerativeModel gm = new GenerativeModel(/* modelName */ "gemini-1.5-flash",
                /* apiKey */ AppConfig.geminiAPIkey);
        GenerativeModelFutures model = GenerativeModelFutures.from(gm);

        String message = messageEdit.getText().toString().trim();
        if (message.isEmpty()) {
            return; // Do not send empty messages
        }

        sender.add(message);
        receiver.add("Waiting For Response");
        chatsView.smoothScrollToPosition(receiver.size() - 1);
        itemAdapter.notifyDataSetChanged();

        // Create content for generative model
        Content content = new Content.Builder()
                .addText(message)
                .build();

        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = model.generateContent(content);

        // Handle response from generative model
        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                Log.d("Gemini Response", resultText);
                runOnUiThread(() -> {
                    // Update the last item in receiver list with the result text
                    receiver.set(receiver.size() - 1, resultText);
                    itemAdapter.notifyDataSetChanged();
                    chatsView.smoothScrollToPosition(receiver.size() - 1);
                });
            }


            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        }, executor);

        // Clear the input field after sending
        messageEdit.setText("");

        // Update UI visibility
        scrollView.setVisibility(View.GONE);
        chatsView.setVisibility(View.VISIBLE);
    }
}
