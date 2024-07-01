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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EmailVerificationActivity extends AppCompatActivity {

    private String oobcode,email;
    FirebaseAuth mAuth;
    ProgressDialog progressDialog;
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

        TextView emailVerificationSt = findViewById(R.id.email_verification_string);
        Button verifyBtn = findViewById(R.id.verify_btn);
        ImageView back = findViewById(R.id.back);

        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append("Your Email ID ");
        SpannableString spannableEmail = new SpannableString(email);
        spannableEmail.setSpan(new ForegroundColorSpan(Color.WHITE), 0, email.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableEmail.setSpan(new StyleSpan(Typeface.BOLD), 0, email.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.append(spannableEmail);
        builder.append(" is not yet verified. Please verify the Email ID to proceed the next step.");
        emailVerificationSt.setText(builder);

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

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

}
