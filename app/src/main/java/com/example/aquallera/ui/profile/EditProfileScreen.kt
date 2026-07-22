package com.example.aquallera.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aquallera.components.AqualleraInput
import com.example.aquallera.components.LoadingIndicator
import com.example.aquallera.components.PrimaryButton
import com.example.aquallera.core.theme.AppBg
import com.example.aquallera.core.theme.MidnightBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", color = AppBg) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppBg)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MidnightBlue),
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingIndicator()
            else -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .background(AppBg)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Name", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MidnightBlue)
                            Spacer(modifier = Modifier.height(4.dp))
                            AqualleraInput(
                                label = "Full Name",
                                value = state.name,
                                onValueChange = { viewModel.onNameChanged(it) },
                                type = KeyboardType.Text,
                                imeAction = ImeAction.Next,
                                required = true,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Email", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MidnightBlue)
                            Spacer(modifier = Modifier.height(4.dp))
                            AqualleraInput(
                                label = "Email Address",
                                value = state.email,
                                onValueChange = { viewModel.onEmailChanged(it) },
                                type = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                                required = true,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Phone Number", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MidnightBlue)
                            Spacer(modifier = Modifier.height(4.dp))
                            AqualleraInput(
                                label = "Phone Number",
                                value = state.number,
                                onValueChange = { viewModel.onNumberChanged(it) },
                                type = KeyboardType.Phone,
                                imeAction = ImeAction.Done,
                            )
                        }
                    }

                    if (state.error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.error ?: "",
                            color = Color.Red,
                            fontSize = 14.sp,
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    PrimaryButton(
                        text = if (state.saving) "Saving..." else "Save Changes",
                        onClick = { viewModel.save() },
                        loading = state.saving,
                        modifier = Modifier.fillMaxWidth(),
                        buttonWidth = 350,
                    )
                }
            }
        }
    }
}
