package com.example.aquallera.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aquallera.R
import com.example.aquallera.components.AqualleraInput
import com.example.aquallera.components.PrimaryButton
import com.example.aquallera.components.WaveBackground
import com.example.aquallera.core.theme.AppBg
import com.example.aquallera.core.theme.LightYellow
import com.example.aquallera.core.theme.MidnightBlue

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToSignup: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.loginState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        WaveBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.logo_no_name),
                contentDescription = "Aquallera Logo",
                modifier = Modifier.size(200.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    text = "←",
                    color = LightYellow,
                    fontSize = 24.sp,
                    modifier = Modifier.clickable { onNavigateBack() },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Log In",
                color = LightYellow,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "Welcome! Log in to continue.",
                color = Color.LightGray,
                fontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppBg)
                    .padding(32.dp),
            ) {
                Column {
                    if (state.error != null) {
                        Text(
                            text = state.error!!,
                            color = Color.Red,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }

                    AqualleraInput(
                        label = "Email",
                        value = state.email,
                        onValueChange = { viewModel.onLoginEmailChanged(it) },
                        type = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        required = true,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    AqualleraInput(
                        label = "Password",
                        value = state.password,
                        onValueChange = { viewModel.onLoginPasswordChanged(it) },
                        isPassword = true,
                        imeAction = ImeAction.Done,
                        required = true,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Don't have an account?",
                                color = Color.Gray,
                                fontSize = 14.sp,
                            )
                            Text(
                                text = "Sign Up",
                                color = MidnightBlue,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { onNavigateToSignup() },
                            )
                        }

                        PrimaryButton(
                            text = "Log In",
                            onClick = { viewModel.login() },
                            loading = state.loading,
                            buttonWidth = 120,
                        )
                    }
                }
            }
        }
    }
}
