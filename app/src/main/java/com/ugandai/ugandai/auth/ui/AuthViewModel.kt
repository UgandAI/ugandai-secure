package com.ugandai.ugandai.auth.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ugandai.ugandai.data.api.SignupRequest
import com.ugandai.ugandai.data.api.UgandAIApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.ugandai.ugandai.auth.data.AuthTokenStore
import retrofit2.HttpException

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val error: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState
    
    var token: String? = null
    private val tokenStore = AuthTokenStore(application)

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = UgandAIApiClient.api.login(email, pass)
                token = response.access_token
                tokenStore.save(response.access_token, email)
                _authState.value = AuthState.Success("Login successful")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(apiError(e, "Login failed"))
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
                _authState.value = AuthState.Error(apiError(e, "Signup failed"))
            }
        }
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }

    private fun apiError(error: Exception, fallback: String): String = when (error) {
        is HttpException -> when (error.code()) {
            401 -> "Invalid username or password"
            403 -> "You do not have permission to perform this action"
            else -> "$fallback (${error.code()})"
        }
        else -> fallback
    }
}
