package com.ugandai.ugandai.logbook.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ugandai.ugandai.logbook.domain.model.ActivityType
import com.ugandai.ugandai.logbook.domain.model.FarmActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogBookScreen(
    viewModel: LogBookViewModel,
    prefillActivity: FarmActivity? = null,
    prefillDraft: ActivityDraft? = null
) {
    val activities by viewModel.activities.collectAsState()
    val showDialog by viewModel.showDialog.collectAsState()
    val editingActivity by viewModel.editingActivity.collectAsState()
    val draftActivity by viewModel.prefillDraft.collectAsState()
    val context = LocalContext.current
    var handledPrefill by remember { mutableStateOf(false) }
    val effectivePrefill = prefillDraft ?: prefillActivity?.let { ActivityDraft.fromActivity(it) }

    LaunchedEffect(effectivePrefill) {
        if (effectivePrefill != null && !handledPrefill) {
            viewModel.showAddDialog(effectivePrefill)
            handledPrefill = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Book") },
                navigationIcon = {
                    IconButton(onClick = { (context as? LogBookActivity)?.finish() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add activity")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Activity History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Newest entries appear first",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (activities.isEmpty()) {
                EmptyLogBookState(onAddClick = { viewModel.showAddDialog() })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activities) { activity ->
                        FarmActivityCard(
                            activity = activity,
                            onEdit = { viewModel.showEditDialog(it) },
                            onDelete = { viewModel.deleteActivity(it) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        ActivityDialog(
            activity = editingActivity,
            draft = draftActivity,
            onDismiss = { viewModel.hideDialog() },
            onSave = { type, date, crop, field, note ->
                viewModel.saveActivity(type, date, crop, field, note)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmActivityCard(
    activity: FarmActivity,
    onEdit: (FarmActivity) -> Unit,
    onDelete: (FarmActivity) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatDate(activity.date),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activity.activityType.displayName(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (activity.crop.isNotEmpty() || activity.field.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (activity.crop.isNotEmpty()) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(activity.crop) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Info, contentDescription = null)
                                    }
                                )
                            }
                            if (activity.field.isNotEmpty()) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(activity.field) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Info, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }

                    if (activity.note.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = activity.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column {
                    IconButton(onClick = { onEdit(activity) }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Activity") },
            text = { Text("Are you sure you want to delete this activity?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(activity)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDialog(
    activity: FarmActivity?,
    draft: ActivityDraft?,
    onDismiss: () -> Unit,
    onSave: (ActivityType, String, String, String, String) -> Unit
) {
    val initialType = activity?.activityType ?: draft?.activityType ?: ActivityType.PLANTED
    val initialDate = activity?.date ?: draft?.date ?: LocalDate.now().toString()
    val initialCrop = activity?.crop ?: draft?.crop.orEmpty()
    val initialField = activity?.field ?: draft?.field.orEmpty()
    val initialNote = activity?.note ?: draft?.note.orEmpty()

    var selectedType by remember { mutableStateOf(initialType) }
    var date by remember { mutableStateOf(initialDate) }
    var crop by remember { mutableStateOf(initialCrop) }
    var field by remember { mutableStateOf(initialField) }
    var note by remember { mutableStateOf(initialNote) }
    var expandedTypeMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (activity == null) "Add Activity" else "Edit Activity") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Activity Type Dropdown
                Box {
                    OutlinedTextField(
                        value = selectedType.displayName(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Activity Type") },
                        trailingIcon = {
                            IconButton(onClick = { expandedTypeMenu = !expandedTypeMenu }) {
                                Icon(
                                    imageVector = if (expandedTypeMenu)
                                        Icons.Default.ArrowDropDown
                                    else
                                        Icons.Default.ArrowDropDown,
                                    contentDescription = "Select"
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedTypeMenu = !expandedTypeMenu }
                    )
                    DropdownMenu(
                        expanded = expandedTypeMenu,
                        onDismissRequest = { expandedTypeMenu = false }
                    ) {
                        ActivityType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName()) },
                                onClick = {
                                    selectedType = type
                                    expandedTypeMenu = false
                                }
                            )
                        }
                    }
                }

                // Date field
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Crop field
                OutlinedTextField(
                    value = crop,
                    onValueChange = { crop = it },
                    label = { Text("Crop (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Field field
                OutlinedTextField(
                    value = field,
                    onValueChange = { field = it },
                    label = { Text("Field (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Note field
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(selectedType, date, crop, field, note)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(dateString: String): String {
    return try {
        val date = LocalDate.parse(dateString)
        date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    } catch (e: Exception) {
        dateString
    }
}

@Composable
private fun EmptyLogBookState(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No entries yet",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start by adding your first farm activity.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAddClick) {
            Text("Add first entry")
        }
    }
}
