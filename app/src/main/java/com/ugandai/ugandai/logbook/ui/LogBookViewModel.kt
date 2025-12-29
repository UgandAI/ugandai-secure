package com.ugandai.ugandai.logbook.ui

import android.content.Context
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

    init {
        loadActivities()
    }

    fun loadActivities() {
        viewModelScope.launch {
            val userId = getCurrentUserId()
            repository.loadActivities(userId)
        }
    }

    fun showAddDialog() {
        _editingActivity.value = null
        _showDialog.value = true
    }

    fun showEditDialog(activity: FarmActivity) {
        _editingActivity.value = activity
        _showDialog.value = true
    }

    fun hideDialog() {
        _showDialog.value = false
        _editingActivity.value = null
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
            val activity = _editingActivity.value

            if (activity != null) {
                // Update existing activity
                val updated = activity.copy(
                    activityType = activityType,
                    date = date,
                    crop = crop,
                    field = field,
                    note = note
                )
                repository.updateActivity(updated)
            } else {
                // Create new activity
                val newActivity = FarmActivity(
                    userId = userId,
                    activityType = activityType,
                    date = date,
                    crop = crop,
                    field = field,
                    note = note
                )
                repository.saveActivity(newActivity)
            }
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
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val sharedPreferences = EncryptedSharedPreferences.create(
            "secure_prefs",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        return sharedPreferences.getString("username", "") ?: ""
    }
}
