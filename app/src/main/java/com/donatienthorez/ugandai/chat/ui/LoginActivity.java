package com.donatienthorez.ugandai.chat.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ugandai.ugandai.databinding.ActivityLoginBinding;

import org.json.JSONObject;

import java.io.OutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;
import android.content.SharedPreferences;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LOGIN_DEBUG";

    private static final String PREFERENCES_FILE = "secure_prefs";
    private static final String TOKEN_KEY = "user_token";
    private static final String USERNAME_KEY = "username";

    private ActivityLoginBinding binding;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "LoginActivity created");

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.loginButton.setOnClickListener(v -> attemptLogin());

        binding.signupRedirectText.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class))
        );
    }

    private void attemptLogin() {
        String email = binding.loginEmail.getText().toString().trim();
        String password = binding.loginPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields are mandatory", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.loginButton.setEnabled(false);
        binding.loginButton.setText("LOADING...");

        executor.execute(() -> {
            Log.d(TAG, "Starting network call");
            String result = performLogin(email, password);
            Log.d(TAG, "Network result: " + result);

            runOnUiThread(() -> {
                binding.loginButton.setEnabled(true);
                binding.loginButton.setText("LOGIN");

                if ("Success".equals(result)) {
                    Log.d(TAG, "Login success → navigating");
                    startActivity(new Intent(
                            LoginActivity.this,
                            com.donatienthorez.ugandai.chat.ui.presets.PresetPromptsActivity.class
                    ));
                } else {
                    Toast.makeText(LoginActivity.this, result, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String performLogin(String username, String password) {
        HttpURLConnection conn = null;
        InputStream stream = null;

        try {
            Log.d(TAG, "Opening connection");

            URL url = new URL("http://ec2-54-85-226-52.compute-1.amazonaws.com:8000/api/token");
            conn = (HttpURLConnection) url.openConnection();

            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            String body = "username=" + username + "&password=" + password;
            Log.d(TAG, "Sending body: " + body);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            Log.d(TAG, "Waiting for response code...");
            int code = conn.getResponseCode();
            Log.d(TAG, "Response code: " + code);

            stream = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

            String response = "";
            if (stream != null) {
                try (Scanner scanner = new Scanner(stream).useDelimiter("\\A")) {
                    response = scanner.hasNext() ? scanner.next() : "";
                }
            }

            Log.d(TAG, "Raw response: " + response);

            if (code >= 200 && code < 300) {
                JSONObject json = new JSONObject(response);

                // ← FIX HERE: use access_token instead of access
                String token = json.getString("access_token");

                saveToken(token);
                saveUsername(username);
                return "Success";
            }

            return response.isEmpty() ? "Invalid credentials" : response;

        } catch (Exception e) {
            Log.e(TAG, "Login exception", e);
            return "Network error";
        } finally {
            if (stream != null) {
                try { stream.close(); } catch (Exception ignored) {}
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private void saveToken(String token) {
        try {
            String masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    PREFERENCES_FILE,
                    masterKey,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            prefs.edit().putString(TOKEN_KEY, token).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving token", e);
        }
    }

    private void saveUsername(String username) {
        try {
            String masterKey = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    PREFERENCES_FILE,
                    masterKey,
                    this,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
            prefs.edit().putString(USERNAME_KEY, username).apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving username", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}