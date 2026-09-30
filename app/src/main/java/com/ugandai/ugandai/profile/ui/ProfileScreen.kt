@file:OptIn(ExperimentalMaterial3Api::class)
package com.ugandai.ugandai.profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ugandai.ugandai.auth.ui.AuthField
import com.ugandai.ugandai.auth.ui.BrandGreen
import com.ugandai.ugandai.auth.ui.ScreenBackground
import com.ugandai.ugandai.auth.ui.SubtitleColor
import com.ugandai.ugandai.auth.ui.TitleColor

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
            .background(ScreenBackground)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Setup Farm Profile",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TitleColor
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tell us about your farm to get started",
            fontSize = 16.sp,
            color = SubtitleColor
        )
        Spacer(modifier = Modifier.height(40.dp))

        AuthField(
            value = farmName,
            onValueChange = { farmName = it },
            placeholder = "Farm Name",
            icon = Icons.Default.Home
        )
        Spacer(modifier = Modifier.height(16.dp))

        AuthField(
            value = district,
            onValueChange = { district = it },
            placeholder = "District",
            icon = Icons.Default.LocationOn
        )
        Spacer(modifier = Modifier.height(16.dp))

        AuthField(
            value = crops,
            onValueChange = { crops = it },
            placeholder = "Crops (comma separated)",
            icon = Icons.Default.Eco
        )
        Spacer(modifier = Modifier.height(16.dp))

        AuthField(
            value = farmSizeStr,
            onValueChange = { farmSizeStr = it },
            placeholder = "Farm Size (acres)",
            icon = Icons.Default.Straighten
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val size = farmSizeStr.toDoubleOrNull()
                viewModel.saveFarmProfile(farmName, district, crops, size)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BrandGreen),
            enabled = profileState !is ProfileState.Loading
        ) {
            if (profileState is ProfileState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text(
                    text = "SAVE PROFILE",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
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
