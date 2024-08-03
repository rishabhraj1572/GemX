package com.gemx.gemx;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseUser user;
    private FirebaseAuth mAuth;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        CardView logoutBtn = findViewById(R.id.logout);
        logoutBtn.setOnClickListener(v->showLogoutDialog());
        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(v->finish());
        ImageView edit_name = findViewById(R.id.edit_name);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        user = mAuth.getCurrentUser();
        String userId = user.getUid();
        getUserDetails(userId);
        edit_name.setOnClickListener(v->showNameChangeDialog(userId));

        CardView privacyPolicy = findViewById(R.id.privacyPolicy);
        CardView help = findViewById(R.id.help);
        CardView aboutUs = findViewById(R.id.aboutUs);

        privacyPolicy.setOnClickListener(v->visitWebsite("https://geminiai-kz9x.onrender.com/privacypolicy"));
        help.setOnClickListener(v->visitWebsite("https://geminiai-kz9x.onrender.com/help"));
        aboutUs.setOnClickListener(v->visitWebsite("https://geminiai-kz9x.onrender.com/aboutus"));

    }

    private void getUserDetails(String userId) {
        TextView userName = findViewById(R.id.name);
        TextView emailView = findViewById(R.id.email);
        db.collection("users").document(userId).get().addOnCompleteListener(task->{
            String name = task.getResult().getString("name");
            String email = task.getResult().getString("email_phone");
            userName.setText(name);
            emailView.setText(email);
        });
    }

    private void showLogoutDialog() {

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        GoogleSignInClient mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Dialog dialog = new Dialog(ProfileActivity.this);
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

            startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
            finishAffinity();

        });

        logoutNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showNameChangeDialog(String userId) {

        Dialog dialog = new Dialog(ProfileActivity.this);
        dialog.setContentView(R.layout.name_change_dialog);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        Button cancel = dialog.findViewById(R.id.cancel);
        Button save = dialog.findViewById(R.id.save);
        EditText nameEdit = dialog.findViewById(R.id.nameEdit);

        save.setOnClickListener(v -> {

            String name = nameEdit.getText().toString();
            if(TextUtils.isEmpty(name)){
                Toast.makeText(this, "Enter a valid input", Toast.LENGTH_SHORT).show();
            }else{
                db.collection("users").document(userId)
                        .update("name", name)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Username updated successfully", Toast.LENGTH_SHORT).show();
                                getUserDetails(userId);
                                dialog.dismiss();
                            } else {
                                Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }
                        });
            }

        });

        cancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void visitWebsite(String url){
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
