package com.example.aquallera.auth.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.aquallera.R
import com.example.aquallera.components.DataPrivacyDialog
import com.example.aquallera.components.PrimaryButton
import com.example.aquallera.core.theme.Blue
import com.example.aquallera.core.theme.LightYellow
import com.example.aquallera.core.theme.MidnightBlue

/** Six-digit email verification; blocks until verified or the user backs out to signup. */
@Composable
fun VerifyEmailScreen(
    viewModel: AuthViewModel,
    onVerified: () -> Unit,
    onBackToSignup: () -> Unit,
) {
    val state by viewModel.verifyState.collectAsState()

    // A user who already verified on another device should not be stuck here.
    LaunchedEffect(Unit) { viewModel.refreshEmailVerified() }
    val alreadyVerified by viewModel.emailVerified.collectAsState()

    val leaveWithoutVerifying = {
        viewModel.abandonUnverifiedSignup()
        onBackToSignup()
    }

    // System back must behave like the button below, or it would leave an unverified
    // account behind and strand the user on this screen.
    BackHandler(enabled = !state.showPrivacy) { leaveWithoutVerifying() }

    if (state.showPrivacy || alreadyVerified == true) {
        DataPrivacyDialog(onContinue = onVerified)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Blue, MidnightBlue)))
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_no_name),
            contentDescription = "Aquallera",
            modifier = Modifier.size(120.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Verify Your Email",
            color = LightYellow,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter the 6-digit code sent to:",
            color = Color.LightGray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )

        Text(
            text = state.email ?: "",
            color = LightYellow,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        CodeInput(
            code = state.code,
            onCodeChanged = { viewModel.onCodeChanged(it) },
        )

        if (state.error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = state.error!!,
                color = Color(0xFFFFCDD2),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .background(Color(0x4DB71C1C), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryButton(
            text = if (state.verifying) "Verifying..." else "Verify",
            onClick = { viewModel.verifyCode() },
            enabled = state.code.length == 6,
            loading = state.verifying,
            buttonWidth = 280,
        )

        Spacer(modifier = Modifier.height(12.dp))

        val resendLabel = when {
            state.sending -> "Sending..."
            state.cooldown > 0 -> "Resend in ${state.cooldown}s"
            else -> "Resend code"
        }
        val resendEnabled = state.cooldown == 0 && !state.sending
        Text(
            text = resendLabel,
            color = if (resendEnabled) LightYellow else LightYellow.copy(alpha = 0.4f),
            fontSize = 14.sp,
            textDecoration = if (resendEnabled) TextDecoration.Underline else null,
            modifier = Modifier
                .clickable(enabled = resendEnabled) { viewModel.resendVerification() }
                .padding(8.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Back to Signup",
            color = Color.Gray,
            fontSize = 12.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable { leaveWithoutVerifying() }
                .padding(8.dp),
        )
    }
}

@Composable
private fun CodeInput(code: String, onCodeChanged: (String) -> Unit) {
    BasicTextField(
        value = code,
        onValueChange = onCodeChanged,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
        textStyle = TextStyle(
            color = MidnightBlue,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            letterSpacing = 12.sp,
        ),
        modifier = Modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                if (code.isEmpty()) {
                    Text(
                        text = "000000",
                        color = Color.LightGray,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 12.sp,
                    )
                }
                innerTextField()
            }
        },
    )
}
