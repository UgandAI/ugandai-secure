package com.donatienthorez.chatgptbot.chat.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import java.io.DataOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

import androidx.appcompat.app.AppCompatActivity;

import com.ugandai.chatgptbot.databinding.ActivitySignupBinding;

public class SignupActivity extends AppCompatActivity {

    ActivitySignupBinding binding;
    DatabaseHelper databaseHelper;

    // Simulate an asynchronous network operation
    public static CompletableFuture<String> performNetworkOperationUserRegisterAsync(String userName, String password) {
        return CompletableFuture.supplyAsync(() -> {
            // Perform network operation here
            return userRegister(userName, password);
        });
    }

    private static String userRegister(String userName, String password) {
        String userCreationSuccess = "Failed";
        try {
            // Step 1: Create a new user by sending a POST request to the /users/ endpoint
            URL userUrl = new URL("http://ec2-54-85-226-52.compute-1.amazonaws.com:8000/users/register/");
            HttpURLConnection conUser = (HttpURLConnection) userUrl.openConnection();

            conUser.setRequestMethod("POST");
            conUser.setRequestProperty("Content-Type", "application/json; utf-8");
            conUser.setRequestProperty("Accept", "application/json");
            conUser.setDoOutput(true);

            String jsonUserInputString = String.format("{\"username\": \"%s\", \"password\": \"%s\"}", userName, password);

            try (DataOutputStream out = new DataOutputStream(conUser.getOutputStream())) {
                out.writeBytes(jsonUserInputString);
                out.flush();
            }

            int userStatus = conUser.getResponseCode();
            System.out.println("User Creation Response Code: " + userStatus);

            if (userStatus == 307 || userStatus == HttpURLConnection.HTTP_MOVED_TEMP) {
                // Handle 307 redirect
                String newUrl = conUser.getHeaderField("Location");
                System.out.println("Redirecting to: " + newUrl);
                conUser.disconnect();

                // Resend the request to the new URL
                userUrl = new URL(newUrl);
                conUser = (HttpURLConnection) userUrl.openConnection();
                conUser.setRequestMethod("POST");
                conUser.setRequestProperty("Content-Type", "application/json; utf-8");
                conUser.setRequestProperty("Accept", "application/json");
                conUser.setDoOutput(true);

                try (DataOutputStream out = new DataOutputStream(conUser.getOutputStream())) {
                    out.writeBytes(jsonUserInputString);
                    out.flush();
                }

                userStatus = conUser.getResponseCode();
            }

            BufferedReader inUser;
            if (userStatus >= 200 && userStatus < 300) {
                inUser = new BufferedReader(new InputStreamReader(conUser.getInputStream(), StandardCharsets.UTF_8));
                userCreationSuccess = "Success";
            } else {
                inUser = new BufferedReader(new InputStreamReader(conUser.getErrorStream(), StandardCharsets.UTF_8));
            }

            String inputLine;
            StringBuilder userContent = new StringBuilder();
            while ((inputLine = inUser.readLine()) != null) {
                userContent.append(inputLine);
            }

            inUser.close();
            conUser.disconnect();

            if (!userCreationSuccess.equals("Success")) {
                userCreationSuccess = userContent.toString();
            }
            System.out.println("User Creation Response: " + userContent.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return userCreationSuccess;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        databaseHelper = new DatabaseHelper(this);

        binding.signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = binding.signupEmail.getText().toString();
                String password = binding.signupPassword.getText().toString();
                String confirmPassword = binding.signupConfirm.getText().toString();

                if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "All fields are mandatory", Toast.LENGTH_SHORT).show();
                } else {
                    if (password.equals(confirmPassword)) {
                        // Perform the async network operation
                        CompletableFuture<String> future = performNetworkOperationUserRegisterAsync(email, password);

                        future.thenAccept(registerStatus -> {
                            // This block is executed after the network operation is complete
                            runOnUiThread(() -> {
                                if (registerStatus.equals("Success")) {
                                    Toast.makeText(SignupActivity.this, "Signup Successfully!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                                    startActivity(intent);
                                } else if (registerStatus.contains("Username which is already in use")) {
                                    Toast.makeText(SignupActivity.this, "User already exists! Please login", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(SignupActivity.this, "Signup Failed!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }).exceptionally(ex -> {
                            // This block is executed if an exception occurs
                            runOnUiThread(() -> {
                                Toast.makeText(SignupActivity.this, "Network operation failed: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                            return null;
                        });
                    } else {
                        //TODO this needs to be within the runOnUiThread.
                        Toast.makeText(SignupActivity.this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        binding.loginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });
    }

}