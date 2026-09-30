@file:OptIn(ExperimentalMaterial3Api::class)
package com.ugandai.ugandai.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ugandai.ugandai.R

val BrandGreen = Color(0xFF446F5D)
val ScreenBackground = Color(0xFFF5F5F5)
val FieldBorder = Color(0xFFCCCCCC)
val TitleColor = Color(0xFF1E1E1E)
val SubtitleColor = Color(0xFF666666)

@Composable
fun AuthScreen(
    onLoginSuccess: (String) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var isLoginMode by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            val successState = authState as AuthState.Success
            if (isLoginMode && viewModel.token != null) {
                onLoginSuccess(viewModel.token!!)
            } else if (!isLoginMode) {
                // Switch back to login mode after signup
                isLoginMode = true
                viewModel.resetState()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isLoginMode) "Login" else "Create an Account",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TitleColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isLoginMode) "Welcome back to UgandAI" else "Sign up to get started",
            fontSize = 16.sp,
            color = SubtitleColor
        )
        Spacer(modifier = Modifier.height(48.dp))

        if (!isLoginMode) {
            AuthField(
                value = username,
                onValueChange = { username = it },
                placeholder = "Username",
                iconRes = R.drawable.baseline_lock_24
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        AuthField(
            value = email,
            onValueChange = { email = it },
            placeholder = "Email",
            iconRes = R.drawable.baseline_email_24,
            keyboardType = KeyboardType.Email
        )
        Spacer(modifier = Modifier.height(16.dp))

        AuthField(
            value = password,
            onValueChange = { password = it },
            placeholder = "Password",
            iconRes = R.drawable.baseline_lock_24,
            isPassword = true
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (isLoginMode) {
                    viewModel.login(email, password)
                } else {
                    viewModel.signup(username, password, email)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            enabled = authState !is AuthState.Loading
        ) {
            if (authState is AuthState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text(
                    text = if (isLoginMode) "LOGIN" else "SIGN UP",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = {
            isLoginMode = !isLoginMode
            viewModel.resetState()
        }) {
            Text(
                text = if (isLoginMode) "Not yet registered? Sign up" else "Already have an account? Login",
                color = BrandGreen,
                fontSize = 15.sp
            )
        }

        if (authState is AuthState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (authState as AuthState.Error).error,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (authState is AuthState.Success && !isLoginMode) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (authState as AuthState.Success).message,
                color = BrandGreen
            )
        }
    }
}

@Composable
fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    iconRes: Int? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Color(0xFF999999)) },
        leadingIcon = {
            when {
                icon != null -> Icon(imageVector = icon, contentDescription = null, tint = BrandGreen)
                iconRes != null -> Icon(painter = painterResource(iconRes), contentDescription = null, tint = BrandGreen)
            }
        },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = BrandGreen,
            unfocusedBorderColor = FieldBorder,
            focusedTextColor = TitleColor,
            unfocusedTextColor = TitleColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    )
}
