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


public class LoginActivity extends AppCompatActivity {

    ActivityLoginBinding binding;
    DatabaseHelper databaseHelper;

    public static CompletableFuture<String> performNetworkOperationUserLoginAsync(String userName, String password) {
        return CompletableFuture.supplyAsync(() -> {
            // Perform network operation here
            return userLogin(userName, password);
        });
    }

    private static String extractTokenFromResponse(String response) {
        // Assuming the token is in the response like {"access_token":"<TOKEN>","token_type":"bearer"}
        String[] parts = response.split(",");
        String tokenPart = parts[0];
        return tokenPart.split(":")[1].replaceAll("\"", "");
    }

    private static String userLogin(String userName, String password){
        String succesfulLogin = "failed";
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
                succesfulLogin = "Success";
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

            if (succesfulLogin.equals("Success")) {
                // Extract the token from the response
                String token = extractTokenFromResponse(tokenContent.toString());
            }
            else {
                succesfulLogin = tokenContent.toString();
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }

        return succesfulLogin;
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