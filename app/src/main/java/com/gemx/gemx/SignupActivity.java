package com.gemx.gemx;

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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    ProgressDialog progressDialog;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_signup);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        TextView signinBtn = findViewById(R.id.signin);
        Button sendOTP = findViewById(R.id.send_btn);
        ImageView backBtn = findViewById(R.id.back);

        backBtn.setOnClickListener(v-> finish());

        signinBtn.setOnClickListener(v-> onBackPressed());

        progressDialog = new ProgressDialog(SignupActivity.this, R.style.MyAlertDialogStyle);

        EditText userName = findViewById(R.id.et_username);
        EditText userId = findViewById(R.id.et_phone_email);
        EditText userPass = findViewById(R.id.et_password);

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

        userId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // No need
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (isValidEmail(charSequence.toString())) {
                    userPass.setVisibility(View.VISIBLE);
                    sendOTP.setText("Continue");
                } else {
                    userPass.setVisibility(View.GONE);
                    sendOTP.setText("Send OTP");
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                // No need
            }
        });

        sendOTP.setOnClickListener(v->{
//            Intent i = new Intent(this,OTPActivity.class);
//            startActivity(i);

            String name = userName.getText().toString();
            String id = userId.getText().toString();
            String pass = userPass.getText().toString();

            if(TextUtils.isEmpty(name)){
                Toast.makeText(this, "Enter your Name", Toast.LENGTH_SHORT).show();
                return;
            }

            if(!isValidEmail(id) && !isValidPhoneNumber(id)){
                Toast.makeText(this, "Enter valid Email or Number", Toast.LENGTH_SHORT).show();
                return;
            }

            if(isValidEmail(id) && pass.length() < 8){
                Toast.makeText(this, "Atleast 8 characters are required for Password", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isValidEmail(id)) {

                progressDialog.setTitle("Please Wait");
                progressDialog.setMessage("Creating your Account");
                progressDialog.setCancelable(false);
                progressDialog.show();

                registerUser(name,id,pass);

            }

            if (isValidPhoneNumber(id)) {

                progressDialog.setTitle("Please Wait");
                progressDialog.setMessage("Sending OTP to your Registered Phone Number");
                progressDialog.setCancelable(false);
                progressDialog.show();

                sendOTP(name,id);
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
                                    Intent i = new Intent(SignupActivity.this, StartConversationActivity.class);
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

    private void sendOTP(String name, String id) {
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
                        Toast.makeText(SignupActivity.this, "Sending OTP Failed!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token) {
                        // Handle the code sent event
                        Intent intent = new Intent(SignupActivity.this, OTPActivity.class);
                        intent.putExtra("verificationId", verificationId);
                        intent.putExtra("name",name);
                        intent.putExtra("id",id);
                        startActivity(intent);
                        progressDialog.dismiss();
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void registerUser(String name, String id, String pass) {
        mAuth.createUserWithEmailAndPassword(id, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userUID = user.getUid();
                            String email = id;

                            // Create a map to store user data
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("userID", userUID);
                            userData.put("email_phone", email);
                            userData.put("name",name);

                            // Save the user data to Firestore
                            db.collection("users").document(userUID)
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d("Save Status","User Details Saved");

                                        // Log in the user
                                        mAuth.signInWithEmailAndPassword(id, pass)
                                                .addOnCompleteListener(loginTask -> {
                                                    if (loginTask.isSuccessful()) {
                                                        if(mAuth.getCurrentUser().isEmailVerified()){
                                                            System.out.println("Email Verified");
                                                            Intent intent = new Intent(SignupActivity.this, StartConversationActivity.class);
                                                            startActivity(intent);
                                                            finish();
                                                        }else{
                                                            System.out.println("Email Not Verified");
                                                            Intent intent = new Intent(SignupActivity.this, EmailVerificationActivity.class);
                                                            intent.putExtra("email",email);
                                                            startActivity(intent);
                                                            progressDialog.dismiss();
                                                        }
//                                                        Intent intent = new Intent(this, StartConversationActivity.class);
//                                                        startActivity(intent);
//                                                        finishAffinity(); // Close all activities
                                                    } else {
                                                        Toast.makeText(this, "Login failed: " + loginTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        progressDialog.dismiss();
                                        Log.d("Save Status","User Details Not Saved");
                                    });
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Registration successful, but unable to retrieve user UID.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Email Already Registered\nPlease Login", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }


    public static boolean isValidPhoneNumber(String phoneNumber) {
        String PHONE_NUMBER_PATTERN = "^\\+[0-9]+$";
        Pattern pattern = Pattern.compile(PHONE_NUMBER_PATTERN);
        Matcher matcher = pattern.matcher(phoneNumber);
        return matcher.matches();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
