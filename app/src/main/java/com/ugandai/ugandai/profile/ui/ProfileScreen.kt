@file:OptIn(ExperimentalMaterial3Api::class)
package com.ugandai.ugandai.profile.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ProfileScreen(
    onProfileSaved: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    var farmName by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var crops by remember { mutableStateOf("") }
    var farmSizeStr by remember { mutableStateOf("") }

    val profileState by viewModel.profileState.collectAsState()

    LaunchedEffect(profileState) {
        if (profileState is ProfileState.Success) {
            onProfileSaved()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Setup Farm Profile",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = farmName,
            onValueChange = { farmName = it },
            label = { Text("Farm Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = district,
            onValueChange = { district = it },
            label = { Text("District") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = crops,
            onValueChange = { crops = it },
            label = { Text("Crops (comma separated)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = farmSizeStr,
            onValueChange = { farmSizeStr = it },
            label = { Text("Farm Size (acres)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val size = farmSizeStr.toDoubleOrNull()
                viewModel.saveFarmProfile(farmName, district, crops, size)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = profileState !is ProfileState.Loading
        ) {
            if (profileState is ProfileState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Text("SAVE PROFILE")
            }
        }

        if (profileState is ProfileState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (profileState as ProfileState.Error).error,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
