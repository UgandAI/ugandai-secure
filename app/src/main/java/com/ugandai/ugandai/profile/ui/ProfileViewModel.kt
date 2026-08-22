package com.ugandai.ugandai.profile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ugandai.ugandai.data.api.FarmProfileRequest
import com.ugandai.ugandai.data.api.UgandAIApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    object Success : ProfileState()
    data class Error(val error: String) : ProfileState()
}

class ProfileViewModel : ViewModel() {
    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState

    fun saveFarmProfile(name: String, district: String, crops: String, size: Double?) {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            try {
                val profiles = UgandAIApiClient.api.getFarmProfiles()
                val request = FarmProfileRequest(name, district, crops, size)
                
                if (profiles.isNotEmpty()) {
                    UgandAIApiClient.api.updateFarmProfile(profiles.first().id, request)
                } else {
                    UgandAIApiClient.api.createFarmProfile(request)
                }
                
                _profileState.value = ProfileState.Success
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to save profile")
            }
        }
    }
}
