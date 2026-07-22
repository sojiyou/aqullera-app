package com.example.aquallera.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aquallera.R
import com.example.aquallera.components.Footer
import com.example.aquallera.components.PrimaryButton
import com.example.aquallera.core.theme.AppBg
import com.example.aquallera.core.theme.MidnightBlue

@Composable
fun LandingScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSignup: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Aquallera Logo",
            modifier = Modifier.size(260.dp),
        )

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            text = "Log In",
            onClick = onNavigateToLogin,
            modifier = Modifier.width(200.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "or",
            color = MidnightBlue.copy(alpha = 0.5f),
            fontSize = 18.sp,
            fontStyle = FontStyle.Italic,
        )

        Spacer(modifier = Modifier.height(12.dp))

        PrimaryButton(
            text = "Sign Up",
            onClick = onNavigateToSignup,
            modifier = Modifier.width(200.dp),
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "\"Clean Water, Anytime, Anywhere.\"",
            color = MidnightBlue.copy(alpha = 0.5f),
            fontSize = 16.sp,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )

        Box(
            modifier = Modifier
                .width(200.dp)
                .height(2.dp)
                .padding(top = 4.dp)
                .background(MidnightBlue),
        )

        Spacer(modifier = Modifier.height(24.dp))
        Footer()
    }
}
