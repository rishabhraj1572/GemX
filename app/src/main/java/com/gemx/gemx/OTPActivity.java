package com.gemx.gemx;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.gemx.gemx.Watchers.OtpTextWatcher;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OTPActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private String verificationId,id,name;
    private ProgressDialog progressDialog;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp);

        mAuth = FirebaseAuth.getInstance();
        verificationId = getIntent().getStringExtra("verificationId");
        id = getIntent().getStringExtra("id");
        name = getIntent().getStringExtra("name");

        db = FirebaseFirestore.getInstance();


        TextView signinBtn = findViewById(R.id.signin);
        TextView resendBtn = findViewById(R.id.resend);
        Button cancelBtn = findViewById(R.id.btn_cancel);
        Button submitBtn = findViewById(R.id.btn_submit);
        EditText otp1 = findViewById(R.id.otp_1);
        EditText otp2 = findViewById(R.id.otp_2);
        EditText otp3 = findViewById(R.id.otp_3);
        EditText otp4 = findViewById(R.id.otp_4);
        EditText otp5 = findViewById(R.id.otp_5);
        EditText otp6 = findViewById(R.id.otp_6);
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

        backBtn.setOnClickListener(v -> finish());

        progressDialog = new ProgressDialog(this, R.style.MyAlertDialogStyle);

        otpHandle(otp1, otp2, otp3, otp4,otp5,otp6);
        submitBtn.setOnClickListener(v -> {
            try {
                progressDialog.setTitle("Please Wait");
                progressDialog.setMessage("Verifying OTP");
                progressDialog.setCancelable(false);
                progressDialog.show();
                String otp = otp1.getText().toString() + otp2.getText().toString() + otp3.getText().toString() + otp4.getText().toString()+ otp5.getText().toString() + otp6.getText().toString();
                if (otp.length() == 6) {
                    verifyCode(otp);
                } else {
                    Toast.makeText(this, "Please enter a valid 6-digit OTP", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        });

        cancelBtn.setOnClickListener(v -> finish());

        signinBtn.setOnClickListener(v -> {
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
            finishAffinity();
        });

        resendBtn.setOnClickListener(v -> {

            progressDialog.setTitle("Please Wait");
            progressDialog.setMessage("Sending OTP to your Registered Phone Number");
            progressDialog.setCancelable(false);
            progressDialog.show();

            resendVerificationCode(); // Resend the OTP

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
                                    Intent i = new Intent(OTPActivity.this, StartConversationActivity.class);
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

    private void verifyCode(String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithCredential(credential);
    }

    private void signInWithCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userUID = user.getUid();
                            String email = id;

                            Map<String, Object> userData = new HashMap<>();
                            userData.put("userID", userUID);
                            userData.put("email_phone", email);

                            //signup
                            if(!TextUtils.isEmpty(name)){
                                userData.put("name",name);
                                db.collection("users").document(userUID)
                                        .set(userData)
                                        .addOnSuccessListener(aVoid -> {
                                            // Sign in success, update UI with the signed-in user's information
                                            Intent intent = new Intent(OTPActivity.this, StartConversationActivity.class);
                                            startActivity(intent);
                                            progressDialog.dismiss();
                                            finishAffinity();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.d("Save Status","User Details Not Saved");
                                            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                                        });
                            }
                            //login
                            else{
                                db.collection("users").document(userUID).get()
                                        .addOnSuccessListener(aVoid -> {
                                            String name = aVoid.getString("name");
                                            if(name == null){
                                                //new user is trying to login
                                                Toast.makeText(this, "User Not Registered", Toast.LENGTH_SHORT).show();
                                                mAuth.signOut();
                                                finish();
                                            }
                                            else{
                                                //old user
                                                // Sign in success, update UI with the signed-in user's information
                                                Intent intent = new Intent(OTPActivity.this, StartConversationActivity.class);
                                                startActivity(intent);
                                                progressDialog.dismiss();
                                                finishAffinity();
                                            }

                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        }

                    } else {
                        // Sign in failed
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(OTPActivity.this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        }
                    }
                });
    }

    private void resendVerificationCode() {
        String phoneNumber = getIntent().getStringExtra("phoneNumber");
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber) // Phone number to verify
                .setTimeout(60L, TimeUnit.SECONDS) // Timeout duration
                .setActivity(this) // Activity (for callback binding)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(PhoneAuthCredential credential) {
                        // Auto-retrieval or instant verification succeeded
                    }

                    @Override
                    public void onVerificationFailed(FirebaseException e) {
                        // Verification failed
                        Toast.makeText(OTPActivity.this, "Verification failed. Please try again.", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }

                    @Override
                    public void onCodeSent(String newVerificationId, PhoneAuthProvider.ForceResendingToken token) {
                        // Code sent, update verification ID
                        verificationId = newVerificationId;
                        Toast.makeText(OTPActivity.this, "OTP Resent.", Toast.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                })
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void otpHandle(EditText otp1, EditText otp2, EditText otp3, EditText otp4, EditText otp5, EditText otp6) {
        otp1.addTextChangedListener(new OtpTextWatcher(otp1, otp2, null));
        otp2.addTextChangedListener(new OtpTextWatcher(otp2, otp3, otp1));
        otp3.addTextChangedListener(new OtpTextWatcher(otp3, otp4, otp2));
        otp4.addTextChangedListener(new OtpTextWatcher(otp4, otp5, otp3));
        otp5.addTextChangedListener(new OtpTextWatcher(otp5, otp6, otp4));
        otp6.addTextChangedListener(new OtpTextWatcher(otp6, null, otp5));

        otp1.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN && otp1.getText().length() == 0) {
                if (otp1.getSelectionStart() == 0) {
                    otp1.clearFocus();
                    otp1.requestFocus();
                }
            }
            return false;
        });

        otp2.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN && otp2.getText().length() == 0) {
                if (otp2.getSelectionStart() == 0) {
                    otp2.clearFocus();
                    otp1.requestFocus();
                }
            }
            return false;
        });

        otp3.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN && otp3.getText().length() == 0) {
                if (otp3.getSelectionStart() == 0) {
                    otp3.clearFocus();
                    otp2.requestFocus();
                }
            }
            return false;
        });

        otp4.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN && otp4.getText().length() == 0) {
                if (otp4.getSelectionStart() == 0) {
                    otp4.clearFocus();
                    otp3.requestFocus();
                }
            }
            return false;
        });
        otp5.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN && otp5.getText().length() == 0) {
                if (otp5.getSelectionStart() == 0) {
                    otp5.clearFocus();
                    otp4.requestFocus();
                }
            }
            return false;
        });

        otp6.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN && otp6.getText().length() == 0) {
                if (otp6.getSelectionStart() == 0) {
                    otp6.clearFocus();
                    otp5.requestFocus();
                }
            }
            return false;
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
