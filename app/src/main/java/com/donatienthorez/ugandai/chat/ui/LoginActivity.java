package com.donatienthorez.ugandai.chat.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ugandai.ugandai.databinding.ActivityLoginBinding;

import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import android.content.SharedPreferences;

public class LoginActivity extends AppCompatActivity {

    private static final String PREFERENCES_FILE = "secure_prefs";  // Define the preferences file name
    private static final String TOKEN_KEY = "user_token";  // Define the key for storing the token

    private ActivityLoginBinding binding;

    public CompletableFuture<String> performNetworkOperationUserLoginAsync(String userName, String password) {
        return CompletableFuture.supplyAsync(() -> userLogin(userName, password));
    }

    private String userLogin(String userName, String password) {
        String successfulLogin = "failed";
        try {
            String inputLine;

            URL tokenUrl = new URL("http://ec2-54-85-226-52.compute-1.amazonaws.com:8000/api/token");
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
                String token = extractTokenFromResponse(tokenContent.toString());
                saveTokenToEncryptedPreferences(token);
            } else {
                successfulLogin = tokenContent.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return successfulLogin;
    }

    private String extractTokenFromResponse(String response) {
        String[] parts = response.split(",");
        String tokenPart = parts[0];
        return tokenPart.split(":")[1].replaceAll("\"", "").trim();
    }

    private String getTokenFromEncryptedPreferences() {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    PREFERENCES_FILE,
                    masterKeyAlias,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            return sharedPreferences.getString(TOKEN_KEY, null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveTokenToEncryptedPreferences(String token) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);

            SharedPreferences sharedPreferences = EncryptedSharedPreferences.create(
                    PREFERENCES_FILE,
                    masterKeyAlias,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(TOKEN_KEY, token);
            // Also save user email as user ID for questionnaire
            String userEmail = binding.loginEmail.getText().toString();
            editor.putString("user_id", userEmail);
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = binding.loginEmail.getText().toString();
                String password = binding.loginPassword.getText().toString();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "All fields are mandatory", Toast.LENGTH_SHORT).show();
                } else {
                    CompletableFuture<String> future = performNetworkOperationUserLoginAsync(email, password);

                    future.thenAccept(loginStatus -> {
                        runOnUiThread(() -> {
                            if (loginStatus.equals("Success")) {
                                Toast.makeText(LoginActivity.this, "Login Successfully!", Toast.LENGTH_SHORT).show();
                                // For now, always go to questionnaire (simple 2-question flow)
                                Intent intent = new Intent(getApplicationContext(), com.donatienthorez.ugandai.questionnaire.QuestionnaireActivity.class);
                                startActivity(intent);
                            } else {
                                Toast.makeText(LoginActivity.this, "Invalid Credentials", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }).exceptionally(ex -> {
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
