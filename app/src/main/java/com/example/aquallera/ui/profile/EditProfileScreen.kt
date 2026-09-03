package com.example.aquallera.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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

    if (state.showOtpDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissOtpDialog() },
            title = { Text("Verify New Email", color = MidnightBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Enter the 6-digit code sent to:",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.otpSentTo ?: "",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MidnightBlue,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OtpInput(
                        code = state.otpCode,
                        onCodeChanged = { viewModel.onOtpChanged(it) },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Confirm your password to update email:",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AqualleraInput(
                        label = "Password",
                        value = state.password,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        type = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        isPassword = true,
                        onImeAction = { viewModel.verifyOtp() },
                    )
                    if (state.otpError != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = state.otpError ?: "",
                            color = Color(0xFFB71C1C),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val otpResendLabel = when {
                        state.sendingOtp -> "Sending..."
                        state.otpCooldown > 0 -> "Resend in ${state.otpCooldown}s"
                        else -> "Resend code"
                    }
                    OtpResendText(
                        label = otpResendLabel,
                        enabled = state.otpCooldown == 0 && !state.sendingOtp,
                        onClick = { viewModel.resendOtp() },
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.verifyOtp() },
                    enabled = state.otpCode.length == 6 && state.password.isNotBlank() && !state.verifying,
                ) {
                    Text(
                        text = if (state.verifying) "Verifying..." else "Verify",
                        color = MidnightBlue,
                        fontWeight = FontWeight.Medium,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissOtpDialog() }, enabled = !state.verifying) {
                    Text("Later", color = Color.Gray)
                }
            },
            containerColor = Color.White,
        )
    }
}

@Composable
private fun OtpInput(code: String, onCodeChanged: (String) -> Unit) {
    Box(
        modifier = Modifier.widthIn(max = 320.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = code,
            onValueChange = onCodeChanged,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            textStyle = TextStyle(
                color = MidnightBlue,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 10.sp,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF1F5F9)),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (code.isEmpty()) {
                        Text(
                            text = "000000",
                            color = Color.LightGray,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 10.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun OtpResendText(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (enabled) MidnightBlue else Color.Gray,
        fontSize = 14.sp,
        textDecoration = if (enabled) TextDecoration.Underline else null,
        modifier = Modifier
            .clickable(enabled = enabled) { onClick() }
            .padding(8.dp),
    )
}
