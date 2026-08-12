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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

/** Edit Profile form with the delete-account flow and its active-order guard. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onGoToOrders: () -> Unit = {},
    onAccountDeleted: () -> Unit = {},
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saved) {
        if (state.saved) onSaved()
    }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onAccountDeleted()
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

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Delete Account", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFB71C1C))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Permanently delete your account and all associated data. This cannot be undone.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.onDeleteAccountClicked() },
                                enabled = !state.checkingOrders,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFEF5350),
                                    contentColor = Color.White,
                                ),
                            ) {
                                Text(
                                    text = if (state.checkingOrders) "Checking orders..." else "Delete My Account",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text("Delete Account?", color = Color(0xFFB71C1C), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "This action is permanent and cannot be undone.\n\n" +
                        "• All your personal data will be permanently removed.\n" +
                        "• You will no longer be able to access this account.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAccount() }, enabled = !state.deleting) {
                    Text(
                        text = if (state.deleting) "Deleting..." else "Delete Account",
                        color = Color(0xFFEF5350),
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }, enabled = !state.deleting) {
                    Text("Keep Account", color = MidnightBlue, fontWeight = FontWeight.Medium)
                }
            },
            containerColor = Color.White,
        )
    }

    if (state.showActiveOrders) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text("Active Orders Found", color = Color(0xFFB71C1C), fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Your account still has orders that are not yet completed or cancelled. " +
                        "Please complete or cancel all your orders before deleting your account.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDialogs(); onGoToOrders() }) {
                    Text("Go to My Orders", color = MidnightBlue, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text("Close", color = Color.Gray)
                }
            },
            containerColor = Color.White,
        )
    }
}
