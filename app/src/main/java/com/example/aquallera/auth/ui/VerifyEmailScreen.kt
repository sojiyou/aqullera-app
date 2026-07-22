package com.example.aquallera.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aquallera.R
import com.example.aquallera.components.PrimaryButton
import com.example.aquallera.core.theme.AppBg
import com.example.aquallera.core.theme.MidnightBlue

@Composable
fun VerifyEmailScreen(
    viewModel: AuthViewModel,
    onVerified: () -> Unit,
    onBackToLogin: () -> Unit,
) {
    val state by viewModel.verifyState.collectAsState()

    LaunchedEffect(state.verified) {
        if (state.verified) onVerified()
    }

    LaunchedEffect(Unit) {
        viewModel.startVerificationPolling()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Aquallera Logo",
            modifier = Modifier.size(120.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Verify Your Email",
            color = MidnightBlue,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "A verification link has been sent to:",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = state.email ?: "",
            color = MidnightBlue,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Click the link in the email to activate your account.",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                color = MidnightBlue,
                strokeWidth = 2.dp,
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = "Waiting for email verification...",
                color = Color.Gray.copy(alpha = 0.6f),
                fontSize = 12.sp,
            )
        }

        if (state.statusMsg != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = state.statusMsg!!,
                color = Color.DarkGray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .let { it },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PrimaryButton(
                text = if (state.sending) "Sending..." else "Resend verification email",
                onClick = { viewModel.resendVerification() },
                loading = state.sending,
                buttonWidth = 280,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { viewModel.logout(); onBackToLogin() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Gray.copy(alpha = 0.7f),
                ),
            ) {
                Text(text = "Back to Login", fontSize = 12.sp)
            }
        }
    }
}
