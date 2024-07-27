package com.gemx.gemx;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
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
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
        String userId = user.getUid();
        getUserName(userId);


        ImageView menu = findViewById(R.id.menu);
        menu.setOnClickListener(v-> showLogoutDialog());

        // Fetch history data
        retrieveHistory();

    }

    private void getUserName(String userId) {
        TextView userName = findViewById(R.id.user_name);
        applyGradientToTextView(userName);
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

    private void applyGradientToTextView(TextView textView) {
        Shader textShader = new LinearGradient(0, 0, textView.getWidth(), textView.getHeight(),
                new int[]{
                        0xFFFFFFFF, // #FFFFFF
                        0xFF8580C4, // #8580C4
                        0xFF5951BC, // #5951BC
                        0xFF4840A6, // #4840A6
                        0xFF6D67B8  // #6D67B8
                },
                new float[]{
                        0, 0.09f, 0.40f, 0.56f, 0.64f
                }, Shader.TileMode.CLAMP);

        textView.getPaint().setShader(textShader);
    }

    private void showLogoutDialog() {

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Dialog dialog = new Dialog(HomeActivity.this);
        dialog.setContentView(R.layout.logout_dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button logoutYes = dialog.findViewById(R.id.logout_yes);
        Button logoutNo = dialog.findViewById(R.id.logout_no);

        logoutYes.setOnClickListener(v -> {

            //logout Intent
            mAuth.signOut();
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                if (task.isSuccessful()){
                    FirebaseAuth.getInstance().signOut(); 
                }
            });

            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finishAffinity();

        });

        logoutNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void initializeUI() {
        CardView chatWithGemx = findViewById(R.id.chatwithGemx);
        CardView talkWithGemx = findViewById(R.id.talkWithGemX);
        LinearLayout videoCall = findViewById(R.id.videoCall);
        ImageView profile = findViewById(R.id.profile);
        profile.setOnClickListener(v-> startActivity(new Intent(HomeActivity.this,ProfileActivity.class)));
        chatWithGemx.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ChattingActivity.class)));
        talkWithGemx.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, TalkActivity.class)));
        videoCall.setOnClickListener(v->startActivity(new Intent(HomeActivity.this,VideoActivity.class)));
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
