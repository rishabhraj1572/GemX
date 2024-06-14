package com.gemx.gemx;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {


    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(LoginActivity.this, R.style.MyAlertDialogStyle);


        Button loginBtn = findViewById(R.id.btn_login);
        TextView createBtn = findViewById(R.id.create);
        EditText et_email=findViewById(R.id.et_username);
        EditText et_pass=findViewById(R.id.et_password);
        TextView forgotBtn = findViewById(R.id.tv_forgot_password);
        ImageView backBtn = findViewById(R.id.back);

        backBtn.setOnClickListener(v-> finish());

        createBtn.setOnClickListener(v->{
            Intent i = new Intent(this,SignupActivity.class);
            startActivity(i);
        });

        forgotBtn.setOnClickListener(v->{
            Intent i = new Intent(this,ForgotPasswordActivity.class);
            startActivity(i);
        });

        loginBtn.setOnClickListener(v -> {
            String email = et_email.getText().toString().trim();
            String password = et_pass.getText().toString().trim();

            if (!isValidEmail(email)) {
                Toast.makeText(this, "Enter a valid email", Toast.LENGTH_SHORT).show();
                return;
            }

            if(TextUtils.isEmpty(password)){
                Toast.makeText(this, "Enter Password", Toast.LENGTH_SHORT).show();
                return;
            }


            loginUser(email, password);
        });
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
                        // Login successful
                        Intent intent = new Intent(LoginActivity.this, StartConversationActivity.class);
                        startActivity(intent);
                        finish(); // Close the login activity
                    } else {
                        // Login failed, show error message
                        Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}