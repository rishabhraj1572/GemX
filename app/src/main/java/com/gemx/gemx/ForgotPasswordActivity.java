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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

public class ForgotPasswordActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;
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
