package com.gemx.gemx;

import static com.gemx.gemx.SignupActivity.isValidPhoneNumber;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.identity.SignInCredential;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {


    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(LoginActivity.this, R.style.MyAlertDialogStyle);

        Button loginBtn = findViewById(R.id.btn_login);
        TextView createBtn = findViewById(R.id.create);
        EditText et_email=findViewById(R.id.et_username);
        EditText et_pass=findViewById(R.id.et_password);
        TextView forgotBtn = findViewById(R.id.tv_forgot_password);
        ImageView backBtn = findViewById(R.id.back);
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

        backBtn.setOnClickListener(v-> finish());

        createBtn.setOnClickListener(v->{
            Intent i = new Intent(this,SignupActivity.class);
            startActivity(i);
        });

        forgotBtn.setOnClickListener(v->{
            Intent i = new Intent(this,ForgotPasswordActivity.class);
            startActivity(i);
        });

        et_email.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // No need
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (isValidEmail(charSequence.toString())) {
                    et_pass.setVisibility(View.VISIBLE);
                } else {
                    et_pass.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // No need
            }
        });

        loginBtn.setOnClickListener(v -> {
            String email = et_email.getText().toString().trim();
            String password = et_pass.getText().toString().trim();

            if (!isValidEmail(email) && !isValidPhoneNumber(email)) {
                Toast.makeText(this, "Enter a valid input", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isValidEmail(email)) {
                if(TextUtils.isEmpty(password)){
                    Toast.makeText(this, "Enter Password", Toast.LENGTH_SHORT).show();
                    return;
                }
                loginUser(email, password);
            }
            if (isValidPhoneNumber(email)) {
                progressDialog.setTitle("Please Wait");
                progressDialog.setMessage("Sending OTP to your Registered Phone Number");
                progressDialog.setCancelable(false);
                progressDialog.show();
                sendOTP(email);
            }

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
                                    Intent i = new Intent(LoginActivity.this, StartConversationActivity.class);
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

    private void sendOTP(String id) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(id)              // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout duration
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        progressDialog.dismiss();
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this, "Sending OTP Failed!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                        // Handle the code sent event
                        Intent intent = new Intent(LoginActivity.this, OTPActivity.class);
                        intent.putExtra("verificationId", verificationId);
                        intent.putExtra("name","");
                        intent.putExtra("id",id);
                        startActivity(intent);
                        progressDialog.dismiss();
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void redirectToAppropriateActivity() {
        boolean hasSeenStartConversation = getSharedPreferences("app_prefs", MODE_PRIVATE)
                .getBoolean("has_seen_start_conversation", false);

        Intent intent;
        if (hasSeenStartConversation) {
            intent = new Intent(this, HomeActivity.class);
        } else {
            intent = new Intent(this, StartConversationActivity.class);
        }
        startActivity(intent);
    }

    private void loginUser(String email, String password) {
        progressDialog.setTitle("Please Wait");
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        saveHasSeenStartConversation(false); // Reset start conversation status
                        // Login successful
                        if(mAuth.getCurrentUser().isEmailVerified()){
                            System.out.println("Email Verified");
                            Intent intent = new Intent(LoginActivity.this, StartConversationActivity.class);
                            startActivity(intent);
                            finish();
                        }else{
                            System.out.println("Email Not Verified");
                            Intent intent = new Intent(LoginActivity.this, EmailVerificationActivity.class);
                            intent.putExtra("email",email);
                            startActivity(intent);
                        }

//                        finish();
                    } else {
                        // Login failed
                        Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveHasSeenStartConversation(boolean hasSeen) {
        getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit()
                .putBoolean("has_seen_start_conversation", hasSeen)
                .apply();
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            String phoneNumber = currentUser.getPhoneNumber();
            if(currentUser.isEmailVerified() || !TextUtils.isEmpty(phoneNumber)){
                System.out.println("Email or number verified");
                redirectToAppropriateActivity();
            }
        }
    }
}