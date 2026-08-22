package com.ugandai.ugandai.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ugandai.ugandai.data.api.SignupRequest
import com.ugandai.ugandai.data.api.UgandAIApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val error: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState
    
    var token: String? = null

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = UgandAIApiClient.api.login(email, pass)
                token = response.access_token
                _authState.value = AuthState.Success("Login successful")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun signup(username: String, pass: String, email: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                UgandAIApiClient.api.signup(SignupRequest(username, pass, email))
                _authState.value = AuthState.Success("Signup successful. Please login.")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Signup failed")
            }
        }
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
