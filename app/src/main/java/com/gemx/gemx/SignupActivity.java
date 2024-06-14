package com.gemx.gemx;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SignupActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    ProgressDialog progressDialog;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_signup);
        mAuth = FirebaseAuth.getInstance();

        TextView signinBtn = findViewById(R.id.signin);
        Button sendOTP = findViewById(R.id.send_btn);
        ImageView backBtn = findViewById(R.id.back);

        backBtn.setOnClickListener(v-> finish());

        signinBtn.setOnClickListener(v-> onBackPressed());

        progressDialog = new ProgressDialog(SignupActivity.this, R.style.MyAlertDialogStyle);

        EditText userName = findViewById(R.id.et_username);
        EditText userId = findViewById(R.id.et_phone_email);
        EditText userPass = findViewById(R.id.et_password);

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

                mAuth.createUserWithEmailAndPassword(id, pass)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(this, "Registered successfully", Toast.LENGTH_SHORT).show();
                                // Log in the user
                                mAuth.signInWithEmailAndPassword(id, pass)
                                        .addOnCompleteListener(loginTask -> {
                                            if (loginTask.isSuccessful()) {
                                                // Login success,
                                                Intent intent = new Intent(this, StartConversationActivity.class);
                                                startActivity(intent);
                                                finish(); // Close the signup activity
                                            } else {
                                                // Login failed, show error message
                                                Toast.makeText(this, "Login failed: " + loginTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            } else {
                                // Registration failed
                                Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            if (isValidPhoneNumber(id)) {

                progressDialog.setTitle("Please Wait");
                progressDialog.setMessage("Sending OTP to your Registered Phone Number");
                progressDialog.setCancelable(false);
                progressDialog.show();
                // Do OTP work
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
                                startActivity(intent);
                                progressDialog.dismiss();
                            }
                        })
                        .build();
                PhoneAuthProvider.verifyPhoneNumber(options);
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
