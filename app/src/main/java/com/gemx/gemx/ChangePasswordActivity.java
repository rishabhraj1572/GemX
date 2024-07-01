package com.gemx.gemx;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText newPasswordEditText;
    private EditText confirmPasswordEditText;
    private Button resetPasswordButton;
    private FirebaseAuth mAuth;
    private String oobCode;

    private ProgressDialog progressDialog;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_pass);
        mAuth = FirebaseAuth.getInstance();

        ImageView backBtn = findViewById(R.id.back);
        TextView signinBtn = findViewById(R.id.signin);
        newPasswordEditText = findViewById(R.id.newPass);
        confirmPasswordEditText = findViewById(R.id.newPassConf);
        resetPasswordButton = findViewById(R.id.submitBtn);
        progressDialog = new ProgressDialog(ChangePasswordActivity.this,R.style.MyAlertDialogStyle);
        progressDialog.setTitle("Please Wait");
        progressDialog.setMessage("Verifying...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        backBtn.setOnClickListener(v -> finish());
        signinBtn.setOnClickListener(v -> {
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
            finishAffinity();
        });

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

        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            String mode = data.getQueryParameter("mode");
            oobCode = data.getQueryParameter("oobCode");

            if ("resetPassword".equals(mode) && oobCode != null) {
                validateResetCode();
            } else if ("verifyEmail".equals(mode) && oobCode!= null) {
                Intent i = new Intent(this,EmailVerificationActivity.class);
                i.putExtra("oobcode",oobCode);
                startActivity(i);
                finishAffinity();
            } else {
                Toast.makeText(this, "Invalid reset password link", Toast.LENGTH_SHORT).show();
                finish();
            }

        } else {
            Toast.makeText(this, "Invalid reset password link", Toast.LENGTH_SHORT).show();
            finish();
        }
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
                                    Intent i = new Intent(ChangePasswordActivity.this, StartConversationActivity.class);
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

    private void validateResetCode() {


        mAuth.verifyPasswordResetCode(oobCode).addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                if (!task.isSuccessful()) {
                    Toast.makeText(ChangePasswordActivity.this, "Invalid or expired reset code", Toast.LENGTH_SHORT).show();
                    finish();
                    progressDialog.dismiss();
                }else{
                    progressDialog.dismiss();
                    resetPasswordButton.setOnClickListener(v -> {
                        progressDialog.show();
                        String newPassword = newPasswordEditText.getText().toString().trim();
                        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

                        if (TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
                            progressDialog.dismiss();
                            Toast.makeText(ChangePasswordActivity.this, "Enter both password fields", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (newPassword.length() < 8) {
                            progressDialog.dismiss();
                            Toast.makeText(ChangePasswordActivity.this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (!newPassword.equals(confirmPassword)) {
                            progressDialog.dismiss();
                            Toast.makeText(ChangePasswordActivity.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        resetPassword(newPassword);
                    });
                }
            }
        });
    }

    private void resetPassword(String newPassword) {
        mAuth.confirmPasswordReset(oobCode, newPassword).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Toast.makeText(ChangePasswordActivity.this, "Password has been successfully changed", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(ChangePasswordActivity.this, LoginActivity.class);
                    startActivity(i);
                    finishAffinity();
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(ChangePasswordActivity.this, "Error in resetting password", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
