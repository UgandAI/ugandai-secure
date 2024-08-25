package com.donatienthorez.chatgptbot.chat.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ugandai.chatgptbot.databinding.ActivityLoginBinding;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import android.content.Context;
import android.content.SharedPreferences;


public class LoginActivity extends AppCompatActivity {

    ActivityLoginBinding binding;
    DatabaseHelper databaseHelper;

    public CompletableFuture<String> performNetworkOperationUserLoginAsync(String userName, String password) {
        return CompletableFuture.supplyAsync(() -> {
            // Perform network operation here
            return userLogin(userName, password, LoginActivity.this);
        });
    }

    private static String extractTokenFromResponse(String response) {
        // Assuming the token is in the response like {"access_token":"<TOKEN>","token_type":"bearer"}
        String[] parts = response.split(",");
        String tokenPart = parts[0];
        return tokenPart.split(":")[1].replaceAll("\"", "");
    }

    private static final String PREFERENCES_FILE = "secure_prefs";
    private static final String TOKEN_KEY = "user_token";

    public String userLogin(String userName, String password, Context context) {
        String successfulLogin = "failed";
        try {
            String inputLine;

            URL tokenUrl = new URL("http://10.0.2.2:8000/api/token");
            HttpURLConnection conToken = (HttpURLConnection) tokenUrl.openConnection();

            conToken.setRequestMethod("POST");
            conToken.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; utf-8");
            conToken.setRequestProperty("Accept", "application/json");
            conToken.setDoOutput(true);

            String formInputString = String.format("username=%s&password=%s", userName, password);

            try (DataOutputStream out = new DataOutputStream(conToken.getOutputStream())) {
                out.writeBytes(formInputString);
                out.flush();
            }

            int tokenStatus = conToken.getResponseCode();
            System.out.println("Token Generation Response Code: " + tokenStatus);

            BufferedReader inToken;
            if (tokenStatus >= 200 && tokenStatus < 300) {
                inToken = new BufferedReader(new InputStreamReader(conToken.getInputStream(), StandardCharsets.UTF_8));
                successfulLogin = "Success";
            } else {
                inToken = new BufferedReader(new InputStreamReader(conToken.getErrorStream(), StandardCharsets.UTF_8));
            }

            StringBuilder tokenContent = new StringBuilder();
            while ((inputLine = inToken.readLine()) != null) {
                tokenContent.append(inputLine);
            }

            inToken.close();
            conToken.disconnect();

            System.out.println("Token Generation Response: " + tokenContent.toString());

            if (successfulLogin.equals("Success")) {
                // Extract the token from the response
                String token = extractTokenFromResponse(tokenContent.toString());

                // Save the token securely
                saveTokenToEncryptedPreferences(context, token);

                //                token = getTokenFromEncryptedPreferences(context);
                //                System.out.println(token);
            } else {
                successfulLogin = tokenContent.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return successfulLogin;
    }

    private String getTokenFromEncryptedPreferences(Context context) throws Exception {
        // Get the master key for encryption
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

        // Create (or retrieve) EncryptedSharedPreferences object
        SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                "secure_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        // Retrieve the token securely
        return sharedPreferences.getString("user_token", null);
    }

    private static void saveTokenToEncryptedPreferences(Context context, String token) throws Exception {
        // Get the master key for encryption
        String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

        // Create (or retrieve) EncryptedSharedPreferences object
        SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                PREFERENCES_FILE,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );

        // Save the token securely
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TOKEN_KEY, token);
        editor.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        databaseHelper = new DatabaseHelper(this);

        binding.loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = binding.loginEmail.getText().toString();
                String password = binding.loginPassword.getText().toString();

                if(email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "All fields are mandatory", Toast.LENGTH_SHORT).show();
                }
                else{
                    CompletableFuture<String> future = performNetworkOperationUserLoginAsync(email, password);

                    future.thenAccept(loginStatus -> {
                        // This block is executed after the network operation is complete
                        runOnUiThread(() -> {
                            if (loginStatus.equals("Success")) {
                                Toast.makeText(LoginActivity.this, "Login Successfully!", Toast.LENGTH_SHORT).show();
                                Intent intent  = new Intent(getApplicationContext(), ChatActivity.class);
                                startActivity(intent);
                            }
                            else {
                                Toast.makeText(LoginActivity.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).exceptionally(ex -> {
                        // This block is executed if an exception occurs
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "Network operation failed: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                        return null;
                    });
                }
            }
        });

        binding.signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                startActivity(intent);
            }
        });
    }
}