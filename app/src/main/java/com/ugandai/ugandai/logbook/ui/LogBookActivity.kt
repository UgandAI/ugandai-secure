package com.ugandai.ugandai.logbook.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ugandai.ugandai.ui.ChatGptBotAppTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class LogBookActivity : ComponentActivity() {

    private val viewModel: LogBookViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefillDraft = intent?.let { parsePrefillDraft(it) }
        setContent {
            ChatGptBotAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LogBookScreen(
                        viewModel = viewModel,
                        prefillDraft = prefillDraft
                    )
                }
            }
        }
    }

    private fun parsePrefillDraft(intent: android.content.Intent): ActivityDraft? {
        val typeRaw = intent.getStringExtra(EXTRA_ACTIVITY_TYPE) ?: return null
        val activityType = runCatching { enumValueOf<com.ugandai.ugandai.logbook.domain.model.ActivityType>(typeRaw) }
            .getOrNull() ?: return null
        val date = intent.getStringExtra(EXTRA_DATE) ?: return null
        return ActivityDraft(
            activityType = activityType,
            date = date,
            crop = intent.getStringExtra(EXTRA_CROP).orEmpty(),
            field = intent.getStringExtra(EXTRA_FIELD).orEmpty(),
            note = intent.getStringExtra(EXTRA_NOTE).orEmpty()
        )
    }

    companion object {
        const val EXTRA_ACTIVITY_TYPE = "logbook_activity_type"
        const val EXTRA_DATE = "logbook_date"
        const val EXTRA_CROP = "logbook_crop"
        const val EXTRA_FIELD = "logbook_field"
        const val EXTRA_NOTE = "logbook_note"
    }
}
