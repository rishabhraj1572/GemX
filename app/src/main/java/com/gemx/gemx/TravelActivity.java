package com.gemx.gemx;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.ChatFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TravelActivity extends AppCompatActivity {

    private AutoCompleteTextView autoCompleteTextView;
    private OkHttpClient client;
    private ArrayAdapter<String> adapter;
    List<Content> history;
    GenerativeModel gm;
    ProgressDialog progressDialog;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel);

        progressDialog = new ProgressDialog(this, R.style.MyAlertDialogStyle);


        ImageView back = findViewById(R.id.back);
        back.setOnClickListener(v->onBackPressed());

        EditText duration_e = findViewById(R.id.duration);
        EditText budget_e = findViewById(R.id.budget);
        EditText no_p_e = findViewById(R.id.nop);
        Button submitBtn = findViewById(R.id.submitBtn);

        client = new OkHttpClient();
        gm = new GenerativeModel("gemini-1.5-flash",AppConfig.geminiAPIkey);

        autoCompleteTextView = findViewById(R.id.autoCompleteTextView);
        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                String input = s.toString();

                int lineCount = autoCompleteTextView.getLineCount();
                if (lineCount > 3) {
                    autoCompleteTextView.setText(input.substring(0, autoCompleteTextView.getSelectionStart() - 1));
                    autoCompleteTextView.setSelection(autoCompleteTextView.getText().length());
                }
                if (input.length() >= 3) {
                    requestCitySuggestions(input);

                }
            }
        });

        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCity = adapter.getItem(position);

            if (selectedCity != null) {
                autoCompleteTextView.setText(selectedCity);
            }
        });

        submitBtn.setOnClickListener(v->{
            String city = autoCompleteTextView.getText().toString();
            String duration = duration_e.getText().toString();
            String budget = budget_e.getText().toString();
            String noOfPer = no_p_e.getText().toString();

            if(TextUtils.isEmpty(city)||TextUtils.isEmpty(duration)){
                Toast.makeText(this, "Enter details", Toast.LENGTH_SHORT).show();
                return;
            }

            if(TextUtils.isEmpty(budget)){
                String prompt = "Make itinerary for "+city+" for "+duration+" days for "+noOfPer+" persons";
                String heading = "Itinerary for "+city+" for "+duration+" days for "+noOfPer+" persons";
                sendMessageToGemini(prompt,heading,city);
                return;
            }

            if(TextUtils.isEmpty(noOfPer)){
                String prompt = "Make itinerary for "+city+" for "+duration+" days in around "+budget+" rupees (INR)";
                String heading = "Itinerary for "+city+" for "+duration+" days in around Rs."+budget;
                sendMessageToGemini(prompt,heading,city);
                return;
            }

            if(TextUtils.isEmpty(noOfPer) && TextUtils.isEmpty(budget)){
                String prompt = "Make itinerary for "+city+" for "+duration+" days";
                String heading = "Itinerary for "+city+" for "+duration+" days";
                sendMessageToGemini(prompt,heading,city);
                return;
            }

            String prompt = "Make itinerary for "+city+" for "+duration+" days for "+noOfPer+" persons in around "+budget+" rupees (INR)";
            String heading = "Itinerary for "+city+" for "+duration+" days for "+noOfPer+" persons in around Rs."+budget;
            //sending prompt to gemini
            sendMessageToGemini(prompt,heading,city);

        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void requestCitySuggestions(String input) {
        String url = "http://api.geonames.org/searchJSON?name_startsWith="+input+"&maxRows=5&username="+AppConfig.GeonamesUsername;

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    List<String> suggestions = parseCitySuggestions(responseData);
                    System.out.println(responseData);
                    runOnUiThread(() -> {
                        adapter = new ArrayAdapter<>(TravelActivity.this,
                                android.R.layout.simple_dropdown_item_1line, suggestions);
                        autoCompleteTextView.setAdapter(adapter);
                        autoCompleteTextView.showDropDown();
                    });
                }
            }
        });
    }

    private List<String> parseCitySuggestions(String responseData) {
        List<String> suggestions = new ArrayList<>();
        try {
            JSONObject jsonResponse = new JSONObject(responseData);
            JSONArray geonamesArray = jsonResponse.getJSONArray("geonames");
            for (int i = 0; i < geonamesArray.length(); i++) {
                JSONObject cityObject = geonamesArray.getJSONObject(i);
                String cityName = cityObject.getString("name");
                String countryName = cityObject.getString("countryName");
                String suggestion = cityName + ", " + countryName;
                suggestions.add(suggestion);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return suggestions;
    }

    private void sendMessageToGemini(String message,String heading,String city) {

        if (message.isEmpty()) {
            return;
        }

        progressDialog.setTitle("Please Wait");
        progressDialog.setMessage("Generating itinerary");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Content.Builder userMessageBuilder = new Content.Builder();
        userMessageBuilder.setRole("user");
        userMessageBuilder.addText(message);
        Content userMessage = userMessageBuilder.build();

        // Start a chat session with history
        if (history == null) {
            history = new ArrayList<>();
        }
        history.add(userMessage);
        ChatFutures chat = ChatFutures.from(gm.startChat(history));

        Executor executor = Executors.newSingleThreadExecutor();
        ListenableFuture<GenerateContentResponse> response = chat.sendMessage(userMessage);

        Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse result) {
                String resultText = result.getText();
                String resultText1 = resultText.replace("**","").replace("*","●").replace("##","");
                Log.d("Gemini Response", resultText);
                runOnUiThread(() -> {
                    Content.Builder modelResponse = new Content.Builder();
                    modelResponse.setRole("model");
                    modelResponse.addText(resultText1);
                    history.add(modelResponse.build());

                    //resultText1 is the generated content for travel and we will move to next page with this response
                    Intent i = new Intent(TravelActivity.this,DisplayTravelDetails.class);
                    i.putExtra("heading",heading);
                    i.putExtra("description",resultText1);
                    i.putExtra("city",city);
                    startActivity(i);
                    progressDialog.dismiss();

                });
            }

            @Override
            public void onFailure(Throwable t) {
                t.printStackTrace();
                progressDialog.dismiss();
            }
        }, executor);

    }
}
