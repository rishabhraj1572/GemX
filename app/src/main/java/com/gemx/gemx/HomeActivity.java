package com.gemx.gemx;


import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gemx.gemx.Adapters.ChatHistoryItemAdapter;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeActivity extends AppCompatActivity implements ChatHistoryItemAdapter.OnItemClickListener {

    private static final int MY_CAMERA_REQUEST_CODE = 1;
    private static final int REQUEST_MICROPHONE = 2;
    private RecyclerView recyclerView;
    private ChatHistoryItemAdapter itemAdapter;
    private List<String> itemList, itemId;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private FirebaseAuth mAuth;
    private boolean isFetchingData = false;
    String userId;
    ProgressBar circularProgress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initializeUI();
        setupRecyclerView();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
        userId = user.getUid();
        getUserName(userId);


//        ImageView menu = findViewById(R.id.menu);
//        menu.setOnClickListener(v-> showLogoutDialog());

        // Fetch history data
        retrieveHistory();
        checkItems();

    }

    private void checkItems() {
        TextView no = findViewById(R.id.noSeraches);
        if(itemList.isEmpty()){
            no.setVisibility(View.VISIBLE);
        }else{
            no.setVisibility(View.GONE);
        }
    }

    private void getUserName(String userId) {
        TextView userName = findViewById(R.id.user_name);
        db.collection("users").document(userId).get().addOnCompleteListener(task->{
            String name = task.getResult().getString("name");
            try{
                String[] words = name.trim().split("\\s+");
                String firstWord = words[0];
                userName.setText(firstWord);
            }catch (Exception e){
                userName.setText(name);
            }
        });
    }

    private void showInfo(String head, String desc) {


        Dialog dialog = new Dialog(HomeActivity.this);
        dialog.setContentView(R.layout.info_i_dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        TextView title = dialog.findViewById(R.id.title);
        TextView description = dialog.findViewById(R.id.description);

        title.setText(head);
        description.setText(desc);


        dialog.show();
    }

    private void initializeUI() {
        CardView chatWithGemx = findViewById(R.id.chatwithGemx);
        CardView talkWithGemx = findViewById(R.id.talkWithGemX);
        LinearLayout videoCall = findViewById(R.id.videoCall);
        LinearLayout translateBtn =  findViewById(R.id.translate);
        LinearLayout travelBtn = findViewById(R.id.travel);
        ImageView profile = findViewById(R.id.profile);
        circularProgress = findViewById(R.id.progress);
        ImageView i_video = findViewById(R.id.i_videoCall);
        ImageView i_translate = findViewById(R.id.i_translate);
        ImageView i_travel = findViewById(R.id.i_travel);

        i_video.setOnClickListener(v->showInfo("Video Connect","Video Connect enables seamless real-time video call to AI"));
        i_translate.setOnClickListener(v->showInfo("Smart Translator","Coming Soon"));
        i_travel.setOnClickListener(v->showInfo("TravelGenie","TravelGenie effortlessly generates personalized travel itineraries for your perfect trip."));
        profile.setOnClickListener(v-> startActivity(new Intent(HomeActivity.this,ProfileActivity.class)));
        translateBtn.setOnClickListener(v-> startActivity(new Intent(HomeActivity.this,TranslateActivity.class)));
        travelBtn.setOnClickListener(v-> startActivity(new Intent(HomeActivity.this,TravelActivity.class)));
        chatWithGemx.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ChattingActivity.class)));
        talkWithGemx.setOnClickListener(v -> {
            if(checkMicrophonePermission()){
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
                } else {
                    startActivity(new Intent(HomeActivity.this, TalkActivity.class));
                }
            }else {
                requestMicrophonePermission();
            }
        });
        videoCall.setOnClickListener(v->
                {
                    if(checkMicrophonePermission()){
                        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                                != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
                        } else {
                            startActivity(new Intent(HomeActivity.this,VideoActivity.class));
                        }
                    }else {
                        requestMicrophonePermission();
                    }
                }
                );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                setupCamera();
            } else {
//                Log.e(TAG, "Camera permission denied.");
                Toast.makeText(this, "Camera permission is required to use this feature", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == REQUEST_MICROPHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_REQUEST_CODE);
                }
            } else {
                Toast.makeText(this, "Microphone permission is required to use this feature", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean checkMicrophonePermission() {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestMicrophonePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MICROPHONE);
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
                                circularProgress.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
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
            System.out.println("All processed");
            checkItems();
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
                        checkItems();
                    } else {
                        startProcessing(index + 1, collectionList);
                        System.out.println(index);
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
            circularProgress.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            getUserName(userId);
    }


    @Override
    public void onItemClick(int position) {
        Intent i = new Intent(this, ChattingActivity.class);
        String historyItemId = itemId.get(position);
        i.putExtra("historyItemId", historyItemId);
        Log.d("Clicked Item", historyItemId);
        startActivity(i);
    }

    //delete each item
    private void deleteItem(int position) {
        String collectionId = itemId.get(position);

        db.collection("chats/"+collectionId+"/messsages")
                        .get().addOnCompleteListener(t->{
                            for (QueryDocumentSnapshot snapshot : t.getResult()){
                                db.collection("chats/"+collectionId+"/messsages").document(snapshot.getId()).delete();
                            }

                    db.collection("chats").document(collectionId)
                            .delete()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    itemList.remove(position);
                                    itemId.remove(position);
                                    checkItems();
                                    retrieveHistory();
                                    deleteFromStorage(collectionId);
                                    Log.d("Firestore", "DocumentSnapshot successfully deleted!");
                                } else {
                                    Log.w("Firestore", "Error deleting document", task.getException());
                                }
                            });

                }).addOnFailureListener(f->{
                    Log.d("F", String.valueOf(f));
                });
    }

    private void deleteFromStorage(String collectionId) {
        String storagePrefix = collectionId + "_";

        StorageReference storageRef = FirebaseStorage.getInstance().getReference();

        storageRef.child("chatImages").listAll()
                .addOnSuccessListener(listResult -> {
                    List<StorageReference> itemsToDelete = new ArrayList<>();

                    for (StorageReference item : listResult.getItems()) {
                        if (item.getName().startsWith(storagePrefix)) {
                            itemsToDelete.add(item);
                        }
                    }

                    List<Task<Void>> deleteTasks = new ArrayList<>();
                    for (StorageReference itemRef : itemsToDelete) {
                        Task<Void> deleteTask = itemRef.delete();
                        deleteTasks.add(deleteTask);
                    }

                    Tasks.whenAll(deleteTasks)
                            .addOnSuccessListener(voidResult -> {
                                Log.d("Firebase Storage", "All items deleted successfully");
                            })
                            .addOnFailureListener(e -> {
                                Log.w("Firebase Storage", "Error deleting items", e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.w("Firebase Storage", "Error listing items", e);
                });
    }

    @Override
    public void onItemDelete(int position) {
        deleteItem(position);
    }
}
