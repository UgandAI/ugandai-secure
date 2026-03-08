package com.ugandai.ugandai.logbook.ui

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.ugandai.ugandai.logbook.data.LogBookRepository
import com.ugandai.ugandai.logbook.domain.model.ActivityType
import com.ugandai.ugandai.logbook.domain.model.FarmActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LogBookViewModel(
    private val repository: LogBookRepository,
    private val context: Context
) : ViewModel() {

    val activities: StateFlow<List<FarmActivity>> = repository.activities

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _editingActivity = MutableStateFlow<FarmActivity?>(null)
    val editingActivity: StateFlow<FarmActivity?> = _editingActivity.asStateFlow()

    private val _prefillDraft = MutableStateFlow<ActivityDraft?>(null)
    val prefillDraft: StateFlow<ActivityDraft?> = _prefillDraft.asStateFlow()

    init {
        loadActivities()
    }

    fun loadActivities() {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            repository.loadActivities(userId)
        }
    }

    fun showAddDialog(prefill: ActivityDraft? = null) {
        _editingActivity.value = null
        _prefillDraft.value = prefill
        _showDialog.value = true
    }

    fun showEditDialog(activity: FarmActivity) {
        _editingActivity.value = activity
        _prefillDraft.value = null
        _showDialog.value = true
    }

    fun hideDialog() {
        _showDialog.value = false
        _editingActivity.value = null
        _prefillDraft.value = null
    }

    fun saveActivity(
        activityType: ActivityType,
        date: String,
        crop: String,
        field: String,
        note: String
    ) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            Log.d("LogBookViewModel", "saveActivity called, userId: $userId")
            if (userId.isEmpty()) {
                Log.e("LogBookViewModel", "userId is empty, cannot save")
                return@launch
            }
            
            val activity = _editingActivity.value

            if (activity != null) {
                // Update existing activity
                Log.d("LogBookViewModel", "Updating existing activity: ${activity.id}")
                val updated = activity.copy(
                    activityType = activityType,
                    date = date,
                    crop = crop,
                    field = field,
                    note = note
                )
                val result = repository.updateActivity(updated)
                Log.d("LogBookViewModel", "Update result: ${result.isSuccess}")
            } else {
                // Create new activity
                Log.d("LogBookViewModel", "Creating new activity")
                val newActivity = FarmActivity(
                    userId = userId,
                    activityType = activityType,
                    date = date,
                    crop = crop,
                    field = field,
                    note = note
                )
                Log.d("LogBookViewModel", "Calling repository.saveActivity...")
                val result = repository.saveActivity(newActivity)
                Log.d("LogBookViewModel", "Save result: success=${result.isSuccess}, failure=${result.isFailure}")
                result.onSuccess { id ->
                    Log.d("LogBookViewModel", "Save succeeded with id: $id")
                }
                result.onFailure { error ->
                    Log.e("LogBookViewModel", "Save failed with error: ${error.message}", error)
                }
            }
            Log.d("LogBookViewModel", "Activity operation completed, hiding dialog")
            hideDialog()
        }
    }

    fun deleteActivity(activity: FarmActivity) {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            repository.deleteActivity(activity.id, userId)
        }
    }

    private fun getCurrentUserId(): String {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPreferences = EncryptedSharedPreferences.create(
                "secure_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPreferences.getString("username", "") ?: ""
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }
}
