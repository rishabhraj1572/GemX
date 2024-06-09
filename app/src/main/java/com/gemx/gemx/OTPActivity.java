package com.gemx.gemx;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.gemx.gemx.Watchers.OtpTextWatcher;


public class OTPActivity extends AppCompatActivity {

    ProgressDialog progressDialog;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_otp);

        TextView signinBtn = findViewById(R.id.signin);
        TextView resendBtn = findViewById(R.id.resend);
        Button cancelBtn = findViewById(R.id.btn_cancel);
        Button submitBtn = findViewById(R.id.btn_submit);
        EditText otp1 = findViewById(R.id.otp_1);
        EditText otp2 = findViewById(R.id.otp_2);
        EditText otp3 = findViewById(R.id.otp_3);
        EditText otp4 = findViewById(R.id.otp_4);
        ImageView backBtn = findViewById(R.id.back);

        backBtn.setOnClickListener(v-> finish());

        otpHandle(otp1,otp2,otp3,otp4);
        submitBtn.setOnClickListener(v -> {
            try {
                int otp1Value = Integer.parseInt(otp1.getText().toString());
                int otp2Value = Integer.parseInt(otp2.getText().toString());
                int otp3Value = Integer.parseInt(otp3.getText().toString());
                int otp4Value = Integer.parseInt(otp4.getText().toString());

                String otp = String.valueOf(otp1Value) + otp2Value + otp3Value + otp4Value;
                Toast.makeText(this, "OTP is : " + otp, Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        });

        cancelBtn.setOnClickListener(v-> finish());

        signinBtn.setOnClickListener(v->{
            Intent i = new Intent(this,LoginActivity.class);
            startActivity(i);
            finishAffinity();
        });

        resendBtn.setOnClickListener(v->{
            progressDialog = new ProgressDialog(this,R.style.MyAlertDialogStyle);
            progressDialog.setTitle("Please Wait");
            progressDialog.setMessage("Sending OTP to your Registered Phone Number");
            progressDialog.setCancelable(false);
            progressDialog.show();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();
                }
            }, 3000); // 3 seconds delay

        });

    }

    private void otpHandle(EditText otp1, EditText otp2, EditText otp3, EditText otp4) {
        otp1.addTextChangedListener(new OtpTextWatcher(otp1, otp2, null));
        otp2.addTextChangedListener(new OtpTextWatcher(otp2, otp3, otp1));
        otp3.addTextChangedListener(new OtpTextWatcher(otp3, otp4, otp2));
        otp4.addTextChangedListener(new OtpTextWatcher(otp4, null, otp3));

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
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}
