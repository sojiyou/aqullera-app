package com.example.aquallera.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.aquallera.components.LoadingIndicator
import com.example.aquallera.core.theme.AppBg
import com.example.aquallera.core.theme.MidnightBlue

@Composable
fun ProfileScreen(
    navController: NavHostController? = null,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    fun navigateToTab(route: String) {
        navController?.navigate(route) {
            popUpTo("map") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val state by viewModel.uiState.collectAsState()

    when {
        state.loading -> LoadingIndicator()
        state.displayName.isBlank() && state.error != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = state.error ?: "", color = Color.Red, fontSize = 16.sp)
            }
        }
        else -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBg)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                AvatarCircle(name = state.displayName)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = state.displayName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MidnightBlue,
                    textAlign = TextAlign.Center,
                )
                if (state.email.isNotBlank()) {
                    Text(
                        text = state.email,
                        fontSize = 14.sp,
                        color = Color.Gray,
                    )
                }
                if (state.email.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    VerificationBadge(
                        isVerified = state.isEmailVerified,
                        sending = state.sendingVerification,
                        sent = state.verificationSent,
                        onResend = { viewModel.resendVerification() },
                        onCheckStatus = { viewModel.checkVerificationStatus() },
                    )
                }
                if (state.phone.isNotBlank()) {
                    Text(
                        text = state.phone,
                        fontSize = 14.sp,
                        color = Color.Gray,
                    )
                }
                if (state.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.error ?: "",
                        color = Color.Red,
                        fontSize = 13.sp,
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column {
                        ProfileActionRow(
                            icon = Icons.Default.Person,
                            label = "Edit Profile",
                            onClick = { navigateToTab("edit_profile") },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileActionRow(
                            icon = Icons.Default.List,
                            label = "My Orders",
                            onClick = { navigateToTab("orders") },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileActionRow(
                            icon = Icons.Default.Info,
                            label = "About",
                            onClick = { navigateToTab("about") },
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileActionRow(
                            icon = Icons.Default.Logout,
                            label = "Sign Out",
                            labelColor = Color.Red,
                            onClick = {
                                viewModel.signOut()
                                navController?.navigate("landing") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarCircle(name: String) {
    val initials = name.split(" ").joinToString("") { it.firstOrNull()?.toString() ?: "" }.take(2)
        .ifBlank { "?" }
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(MidnightBlue),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials.uppercase(),
            color = AppBg,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun VerificationBadge(
    isVerified: Boolean,
    sending: Boolean,
    sent: Boolean,
    onResend: () -> Unit,
    onCheckStatus: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (isVerified) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Verified",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF2E7D32),
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFE65100),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Unverified",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE65100),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onResend, enabled = !sending) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (sent) "Sent!" else if (sending) "Sending..." else "Resend Verification")
                }
                TextButton(onClick = onCheckStatus) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Check Status")
                }
            }
        }
    }
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    labelColor: Color = MidnightBlue,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = labelColor, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            color = labelColor,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(16.dp),
        )
    }
}
