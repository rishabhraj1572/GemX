package com.gemx.gemx;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

public class EmailVerificationActivity extends AppCompatActivity {

    private String oobcode,email;
    FirebaseAuth mAuth;
    ProgressDialog progressDialog;
    private TextView emailVerificationSt;
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_verification);

        progressDialog = new ProgressDialog(EmailVerificationActivity.this,R.style.MyAlertDialogStyle);
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        Intent i = getIntent();
        email = i.getStringExtra("email");
        oobcode = i.getStringExtra("oobcode");

        emailVerificationSt = findViewById(R.id.email_verification_string);
        Button verifyBtn = findViewById(R.id.verify_btn);
        ImageView back = findViewById(R.id.back);

        try{
            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append("Your Email ID ");
            SpannableString spannableEmail = new SpannableString(email);
            spannableEmail.setSpan(new ForegroundColorSpan(Color.WHITE), 0, email.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannableEmail.setSpan(new StyleSpan(Typeface.BOLD), 0, email.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.append(spannableEmail);
            builder.append(" is not yet verified. Please verify the Email ID to proceed the next step.");
            emailVerificationSt.setText(builder);
        }catch (Exception ignore){}

        back.setOnClickListener(v-> onBackPressed());

        verifyBtn.setOnClickListener(v->{

            if(user!=null){
                progressDialog.setTitle("Please Wait");
                progressDialog.setMessage("Sending Email...");
                progressDialog.setCancelable(false);
                progressDialog.show();
                if(!user.isEmailVerified()){
                    //email not verified
                    Toast.makeText(this, "Verification mail sent to Email", Toast.LENGTH_SHORT).show();
                    user.sendEmailVerification();
                    progressDialog.dismiss();
                }else {
                    //email verified
                    Intent intent = new Intent(EmailVerificationActivity.this, StartConversationActivity.class);
                    startActivity(intent);
                    finish();
                }

            }
        });

        if(user == null){
            finish();
            Toast.makeText(this, "Invalid Verification Link", Toast.LENGTH_SHORT).show();
        }else {
            if (oobcode != null) {
                verifyEmail();
            }
        }

        //glogin
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


        //signin button
        TextView signinBtn = findViewById(R.id.signin);
        signinBtn.setOnClickListener(v-> {
            startActivity(new Intent(this, LoginActivity.class));
            finishAffinity();
        });

    }

    private void verifyEmail() {
        progressDialog.setTitle("Please Wait");
        progressDialog.setMessage("Verifying...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        mAuth.applyActionCode(oobcode).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (!task.isSuccessful()) {
                    try{
                        SpannableStringBuilder builder = new SpannableStringBuilder();
                        builder.append("Your Email ID ");
                        SpannableString spannableEmail = new SpannableString(email);
                        spannableEmail.setSpan(new ForegroundColorSpan(Color.WHITE), 0, email.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        spannableEmail.setSpan(new StyleSpan(Typeface.BOLD), 0, email.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        builder.append(spannableEmail);
                        builder.append(" is not yet verified. Please verify the Email ID to proceed the next step.");
                        emailVerificationSt.setText(builder);
                    }catch (Exception ignore){}

                    Toast.makeText(EmailVerificationActivity.this, "Invalid or expired reset code", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();

                }else{
                    progressDialog.dismiss();
                    signInUser();
                }
            }
        });
    }

    private void signInUser() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    progressDialog.dismiss();
                    if (user.isEmailVerified()) {
                        startActivity(new Intent(EmailVerificationActivity.this, EmailVerified.class));
                        finish();
                    } else {
                        Toast.makeText(EmailVerificationActivity.this, "Email verification failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            progressDialog.dismiss();
            Toast.makeText(this, "Email verification failed", Toast.LENGTH_SHORT).show();
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
                                    Intent i = new Intent(EmailVerificationActivity.this, StartConversationActivity.class);
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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

}
