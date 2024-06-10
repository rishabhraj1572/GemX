package com.gemx.gemx.Watchers;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.gemx.gemx.R;

public class OtpTextWatcher implements TextWatcher {
    private final EditText currentEditText;
    private final EditText nextEditText;
    private final EditText prevEditText;

    public OtpTextWatcher(EditText currentEditText, EditText nextEditText, EditText prevEditText) {
        this.currentEditText = currentEditText;
        this.nextEditText = nextEditText;
        this.prevEditText = prevEditText;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        //no need
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        //no need
    }

    @Override
    public void afterTextChanged(Editable s) {
        if (s.length() == 1 && nextEditText != null) {
            nextEditText.requestFocus();
        } else if (s.length() == 0 && prevEditText != null) {
            prevEditText.requestFocus();
        }
        if (s.toString().isEmpty()) {
            currentEditText.setBackgroundResource(R.drawable.otp_bg_without_border);
        } else {
            currentEditText.setBackgroundResource(R.drawable.otp_bg_with_border);
        }
    }
}
