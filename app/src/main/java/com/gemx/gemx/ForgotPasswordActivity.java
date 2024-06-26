package com.gemx.gemx;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ForgotPasswordActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot);
        mAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(ForgotPasswordActivity.this, R.style.MyAlertDialogStyle);

        TextView signinBtn = findViewById(R.id.signin);
        TextView back_to_login = findViewById(R.id.back_to_login);
        ImageView backBtn = findViewById(R.id.back);
        Button sendOTP = findViewById(R.id.send_btn);
        EditText et_email = findViewById(R.id.et_email);

        ImageView gLogin = findViewById(R.id.gmail);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        gLogin.setOnClickListener(v->{
            progressDialog.setTitle("Please Wait");
            progressDialog.setMessage("Logging In...");
            progressDialog.show();
            signIn();
        });

        backBtn.setOnClickListener(v-> onBackPressed());
        back_to_login.setOnClickListener(v->onBackPressed());
        signinBtn.setOnClickListener(v-> onBackPressed());

        sendOTP.setOnClickListener(v->{
//            Intent i = new Intent(this,VerificationActivity.class);
//            startActivity(i);
            progressDialog.setTitle("Please Wait");
            progressDialog.setMessage("Sending Verification Link");
            progressDialog.setCancelable(false);
            progressDialog.show();
            String email = et_email.getText().toString();
            forgotPassword(email);
        });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                progressDialog.dismiss();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("glogin", "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("userID", user.getUid());
                        userData.put("email_phone", user.getEmail());
                        userData.put("name",user.getDisplayName());

                        // Save the user data to Firestore
                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        db.collection("users").document(user.getUid())
                                .set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d("Save Status","User Details Saved");
                                    Intent i = new Intent(ForgotPasswordActivity.this, StartConversationActivity.class);
                                    startActivity(i);
                                    finishAffinity();
                                })
                                .addOnFailureListener(e -> {
                                    Log.d("Save Status","User Details Not Saved");
                                });
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("glogin", "signInWithCredential:failure", task.getException());
                        progressDialog.dismiss();
                    }
                });
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void forgotPassword(String email){
        if(!isValidEmail(email)){
            Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        }else{
            FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password reset link sent to your email", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        }else {
                            Toast.makeText(this, "Error in sending reset link", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        }
                    });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
