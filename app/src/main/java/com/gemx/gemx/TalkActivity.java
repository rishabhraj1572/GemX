package com.gemx.gemx;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TalkActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final String TAG = "TalkActivity";
    private static final int REQUEST_MICROPHONE = 1;
    private SpeechRecognizer speechRecognizer;
    TextView msgText;
    private AudioManager audioManager;
    private final Handler handler = new Handler();
    private int originalSystemVolume;
    private int originalNotificationVolume,originalRingVolume;
    private TextToSpeech textToSpeech;
    List<Content> history;
    GenerativeModel gm;
    LottieAnimationView micAnim;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_talk);

        ImageView backBtn = findViewById(R.id.back);
        ImageView micBtn = findViewById(R.id.mic_btn);
        msgText = findViewById(R.id.msgText);
        micAnim = findViewById(R.id.animationView1);

        backBtn.setOnClickListener(v -> finish());
        micBtn.setOnClickListener(v -> {
            if (checkMicrophonePermission()) {
                if (textToSpeech != null) {
                    textToSpeech.stop();
                }
                startListening();
            } else {
                requestMicrophonePermission();
            }
        });

        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new SpeechRecognitionListener());
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, this);

        gm = new GenerativeModel("gemini-1.5-flash",AppConfig.geminiAPIkey);

    }

    private boolean checkMicrophonePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMicrophonePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);
    }

    private void startListening() {
        runOnUiThread(() -> {
            saveCurrentVolumeLevels();
            muteAllSounds();
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            speechRecognizer.startListening(intent);
        });
    }


    private void saveCurrentVolumeLevels() {
        if (audioManager != null) {
            originalSystemVolume = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
            originalNotificationVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
            originalRingVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
        }
    }

    private void muteAllSounds() {
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, 0, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_RING, 0, 0);
        }
    }

    private void restoreVolumeLevelsDelayed(String delay) {
        if(delay != null){
            handler.postDelayed(() -> restoreVolumeLevels(), Integer.parseInt(delay));
        }else{
            handler.postDelayed(() -> restoreVolumeLevels(), 4000);
        }

    }

    private void restoreVolumeLevels() {
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_SYSTEM, originalSystemVolume, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, originalNotificationVolume, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_RING, originalRingVolume, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MICROPHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startListening();
            } else {
                Toast.makeText(this, "Microphone permission is required to use this feature", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        restoreVolumeLevelsDelayed("1000");
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    private class SpeechRecognitionListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "Ready for speech");
            micAnim.setVisibility(View.VISIBLE);
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "Speech started");
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "Speech ended");
            restoreVolumeLevelsDelayed(null);
        }

        @Override
        public void onError(int error) {
            Log.e(TAG, "Error: " + error);
            micAnim.setVisibility(View.GONE);
            restoreVolumeLevelsDelayed("500");
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String recognizedText = matches.get(0);
                Log.d(TAG, "Recognized text: " + recognizedText);
                restoreVolumeLevelsDelayed(null);
                msgText.setText(recognizedText);
                sendMessageToGemini(recognizedText);
            }

        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> partialMatches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (partialMatches != null && !partialMatches.isEmpty()) {
                String partialText = partialMatches.get(0);
                Log.d(TAG, "Partial text: " + partialText);
                msgText.setText(partialText);
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Setting language
            int result = textToSpeech.setLanguage(Locale.US);
            // Language data is missing or the language is not supported
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported");
            } else {
                Log.d(TAG, "Text-to-Speech engine initialized successfully");
                // Set UtteranceProgressListener after successful initialization
                textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
            }
        } else {
            Log.e(TAG, "TTS initialization failed");
        }
    }

    private void speakRecognizedText(String text) {
        System.out.println("TTS started");

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UniqueUtteranceId");
    }

    private void sendMessageToGemini(String message) {

        if (message.isEmpty()) {
            return;
        }

        Content.Builder userMessageBuilder = new Content.Builder();
        userMessageBuilder.setRole("user");
        userMessageBuilder.addText("Answer me in 20-30 words. And don't include anything in your sentences about this, this line.");
        userMessageBuilder.addText(message);
        Content userMessage = userMessageBuilder.build();

        // Start a chat session with history
        if (history == null) {
            history = new ArrayList<>();
        }
        history.add(userMessage);
        ChatFutures chat = ChatFutures.from(gm.startChat(history));

        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = chat.sendMessage(userMessage);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                String resultText1 = resultText.replace("**","").replace("*","●").replace("##","");
                Log.d("Gemini Response", resultText);
                runOnUiThread(() -> {
                    Content.Builder modelResponse = new Content.Builder();
                    modelResponse.setRole("model");
                    modelResponse.addText(resultText1);
                    history.add(modelResponse.build());
                    speakRecognizedText(resultText1);

//                    Map<String, Object> userData = new HashMap<>();
//                    userData.put("chat_name", sender.get(0));
//                    userData.put("last_update",epochTime);


//                    String userUID = user.getUid();
//                    db.collection("chats").document(userUID+"_"+epochTime)
//                            .set(userData)
//                            .addOnSuccessListener(aVoid -> {
//                                Log.d("Save Status","Details Saved");
//
//                            })
//                            .addOnFailureListener(e -> {
//                                Log.d("Save Status","Not Saved");
//                            });

                    Map<String, Object> messageData = new HashMap<>();
                    messageData.put("message_sent", message);
                    messageData.put("message_received",resultText1);
                    long epochTime1 = System.currentTimeMillis();

//                    db.collection("chats").document(userUID+"_"+epochTime)
//                            .collection("messsages").document(String.valueOf(epochTime1))
//                            .set(messageData)
//                            .addOnSuccessListener(aVoid -> {
//                                Log.d("Save Status","Saved");
//                            })
//                            .addOnFailureListener(e -> {
//                                Log.d("Save Status","Not Saved");
//                            });


                });
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        }, executor);

    }
    private UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
            // Speech synthesis started
            micAnim.setVisibility(View.GONE);
        }

        @Override
        public void onDone(String utteranceId) {
            // Speech synthesis completed
            // Call your function here
            System.out.println("Speech Ended");
            startListening();
        }

        @Override
        public void onError(String utteranceId) {
            // Speech synthesis error
            micAnim.setVisibility(View.VISIBLE);
        }
    };



}
