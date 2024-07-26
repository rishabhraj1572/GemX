package com.gemx.gemx;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class VideoActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{

    private static final String TAG = "VideoActivity";
    private static final int MY_CAMERA_REQUEST_CODE = 100;
    private static final int REQUEST_MICROPHONE = 1;
    private Camera mCamera;
    private CameraPreview mPreview;
    private FrameLayout preview;
    private int currentCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private SpeechRecognizer speechRecognizer;
    private AudioManager audioManager;
    private TextToSpeech textToSpeech;
    List<Content> history;
    GenerativeModel gm;
    private Bitmap CapImage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);


        // Initialize SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new VideoActivity.SpeechRecognitionListener());
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        switchToCallProfile();

        // Initialize TextToSpeech
        textToSpeech = new TextToSpeech(this, this);

        gm = new GenerativeModel("gemini-1.5-flash",AppConfig.geminiAPIkey);

        preview = findViewById(R.id.cameraView);
        ImageView back =findViewById(R.id.back);
        back.setOnClickListener(v->onBackPressed());
        ImageView disconnect =findViewById(R.id.disconnect);
        disconnect.setOnClickListener(v->onBackPressed());

        ImageView switchCameraButton = findViewById(R.id.btnSwitchCamera);
        switchCameraButton.setOnClickListener(v -> switchCamera());

        ImageView toggleCameraButton = findViewById(R.id.btnToggleCamera);
        toggleCameraButton.setOnClickListener(v -> toggleCamera());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
        } else {
            if(setupCamera()){
                //start capturing audio
                if (checkMicrophonePermission()) {
                    if (textToSpeech != null) {
                        textToSpeech.stop();
                    }
                    startListening();
                } else {
                    requestMicrophonePermission();
                }

            }else{
                //show error roast
            }
        }
    }

    private boolean checkMicrophonePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMicrophonePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

            // Check if the image is already under 50 KB
            if (data.length > 50 * 1024) {
                // Resize and compress the bitmap if it's too large
                bitmap = resizeBitmapTo200KB(bitmap);
            }

            CapImage = bitmap;
            mCamera.startPreview();
        }
    };

    private void capturePicture() {
        if (mCamera != null) {
            mCamera.enableShutterSound(false);
            Camera.Parameters params = mCamera.getParameters();
            // Set picture size to a reasonable resolution (e.g., 1024x768)
//            params.setPictureSize(1024, 768);
            // Set JPEG quality to a value that balances quality and size (e.g., 85)
            params.setJpegQuality(60);
            mCamera.setParameters(params);

            mCamera.takePicture(null, null, mPicture);
        }
    }

    private Bitmap resizeBitmapTo200KB(Bitmap bitmap) {
        final int MAX_SIZE = 50 * 1024; // 50 KB
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // Initial compression quality
        int quality = 60;

        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        while (baos.toByteArray().length > MAX_SIZE && quality > 5) {
            baos.reset();
            quality -= 5;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        }

        byte[] bitmapData = baos.toByteArray();
        return BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.length);
    }


    private void startListening() {
        runOnUiThread(() -> {
//            saveCurrentVolumeLevels();
//            muteAllSounds();
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            speechRecognizer.startListening(intent);
        });
    }

    private void switchToCallProfile() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true);
    }

    void restoreVol(){
        audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false);
    }


    private Boolean setupCamera() {
        if (checkCameraHardware(this)) {
            mCamera = getCameraInstance(currentCameraId);
            if (mCamera != null) {
                mPreview = new CameraPreview(this, mCamera);
                preview.addView(mPreview);
                return true;
            } else {
                Log.e(TAG, "Camera instance is null.");
                return false;
            }
        } else {
            Log.e(TAG, "No camera hardware found.");
            return false;
        }
    }

    private boolean checkCameraHardware(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
    }

    public static Camera getCameraInstance(int cameraId) {
        Camera c = null;
        try {
            c = Camera.open(cameraId);
        } catch (Exception e) {
            Log.e(TAG, "Camera is not available (in use or does not exist): " + e.getMessage());
        }
        return c;
    }

    private void switchCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        currentCameraId = (currentCameraId == Camera.CameraInfo.CAMERA_FACING_BACK)
                ? Camera.CameraInfo.CAMERA_FACING_FRONT
                : Camera.CameraInfo.CAMERA_FACING_BACK;

        mCamera = getCameraInstance(currentCameraId);
        if (mCamera != null) {
            mPreview = new CameraPreview(this, mCamera);
            preview.removeAllViews();
            preview.addView(mPreview);
        } else {
            Log.e(TAG, "Failed to switch camera.");
        }
    }

    private void toggleCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            preview.removeAllViews();
        } else {
            setupCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera();
            } else {
                Log.e(TAG, "Camera permission denied.");
            }
        }

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
        mCamera.release();
        speechRecognizer.destroy();
        textToSpeech.stop();
        restoreVol();
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        restoreVol();
    }

    @Override
    protected void onResume() {
        super.onResume();
        switchToCallProfile();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported");
            } else {
                Log.d(TAG, "Text-to-Speech engine initialized successfully");
                textToSpeech.setOnUtteranceProgressListener(utteranceProgressListener);
            }
        } else {
            Log.e(TAG, "TTS initialization failed");
        }
    }

    private UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {
        }

        @Override
        public void onDone(String utteranceId) {
            System.out.println("Speech Ended");
            startListening();
        }

        @Override
        public void onError(String utteranceId) {
        }
    };

    private class SpeechRecognitionListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "Ready for speech");
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
//            restoreVolumeLevelsDelayed(null);

            //click picture here
            capturePicture();
        }

        @Override
        public void onError(int error) {
            Log.e(TAG, "Error: " + error);

            if(error == 7){
                startListening();
            }
//            restoreVolumeLevelsDelayed("500");
        }

        @Override
        public void onResults(Bundle results) {
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty()) {
                String recognizedText = matches.get(0);
                Log.d(TAG, "Recognized text: " + recognizedText);
//                restoreVolumeLevelsDelayed(null);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (CapImage == null) {
                            // Wait for CapImage to be not null
                            try {
                                Thread.sleep(100); // Sleep for 100ms before checking again
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        // CapImage is not null, proceed with sending the message
                        sendMessageToGemini(recognizedText);
                    }
                }).start();


            }

        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            ArrayList<String> partialMatches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (partialMatches != null && !partialMatches.isEmpty()) {
                String partialText = partialMatches.get(0);
                Log.d(TAG, "Partial text: " + partialText);
            }
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
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
        userMessageBuilder.addText("don't use 'image shows' in your sentences");
        userMessageBuilder.addText(message);
        userMessageBuilder.addImage(CapImage);
        CapImage = null;
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

                   /* Map<String, Object> userData = new HashMap<>();
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
                            });*/

                    Map<String, Object> messageData = new HashMap<>();
                    messageData.put("message_sent", message);
                    messageData.put("message_received",resultText1);
                    long epochTime1 = System.currentTimeMillis();

                   /* db.collection("chats").document(userUID+"_"+epochTime)
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
            public void onFailure(Throwable t) {
                t.printStackTrace();
            }
        }, executor);

    }
}
