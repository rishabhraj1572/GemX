package com.gemx.gemx;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText newPasswordEditText;
    private EditText confirmPasswordEditText;
    private Button resetPasswordButton;
    private FirebaseAuth mAuth;
    private String oobCode;

    private ProgressDialog progressDialog;

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

        Intent intent = getIntent();
        Uri data = intent.getData();
        if (data != null) {
            String mode = data.getQueryParameter("mode");
            oobCode = data.getQueryParameter("oobCode");

            if ("resetPassword".equals(mode) && oobCode != null) {
                validateResetCode();
            } else {
                Toast.makeText(this, "Invalid reset password link", Toast.LENGTH_SHORT).show();
                finish();
            }

        } else {
            Toast.makeText(this, "Invalid reset password link", Toast.LENGTH_SHORT).show();
            finish();
        }
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
