package com.example.aquallera.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aquallera.R
import com.example.aquallera.core.theme.AppBg
import com.example.aquallera.core.theme.MidnightBlue
import kotlinx.coroutines.delay

/** Holds the splash while auth state resolves, then routes to landing, map or verification. */
@Composable
fun SplashScreen(
    onNavigateToLanding: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToVerifyEmail: () -> Unit,
    isLoggedIn: Boolean,
    /** null while the verification check is still in flight — stay on the splash until it resolves. */
    isEmailVerified: Boolean?,
) {
    LaunchedEffect(isLoggedIn, isEmailVerified) {
        delay(1500)
        when {
            !isLoggedIn -> onNavigateToLanding()
            isEmailVerified == true -> onNavigateToMap()
            isEmailVerified == false -> onNavigateToVerifyEmail()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_no_name),
            contentDescription = "Aquallera Logo",
            modifier = Modifier.size(180.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "\"Clean Water, Anytime, Anywhere.\"",
            color = MidnightBlue.copy(alpha = 0.5f),
            fontSize = 16.sp,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(modifier = Modifier.height(40.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(32.dp),
            color = MidnightBlue,
            strokeWidth = 3.dp,
        )
    }
}
