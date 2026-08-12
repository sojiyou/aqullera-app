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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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

/** Signup form; routes to email verification once an account is created. */
@Composable
fun SignupScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToVerifyEmail: () -> Unit,
    onNavigateBack: () -> Unit,
    signupSuccess: Boolean,
) {
    val state by viewModel.signupState.collectAsState()

    // Must be a side effect, not a composition-time call: navigating inline re-fired on every
    // recomposition, so returning here from the verify screen bounced straight back to it.
    LaunchedEffect(signupSuccess) {
        if (signupSuccess) onNavigateToVerifyEmail()
    }

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
                modifier = Modifier.size(135.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))

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
                text = "Sign Up",
                color = LightYellow,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = "Welcome! Create an account to continue.",
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
                    .padding(horizontal = 32.dp, vertical = 36.dp),
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
                        label = "Full Name",
                        value = state.fullName,
                        onValueChange = { viewModel.onSignupFieldChanged("fullName", it) },
                        imeAction = ImeAction.Next,
                        required = true,
                    )

                    AqualleraInput(
                        label = "Email",
                        value = state.email,
                        onValueChange = { viewModel.onSignupFieldChanged("email", it) },
                        type = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        required = true,
                    )

                    AqualleraInput(
                        label = "Number",
                        value = state.number,
                        onValueChange = { viewModel.onSignupFieldChanged("number", it) },
                        type = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                        required = true,
                    )

                    AqualleraInput(
                        label = "Password",
                        value = state.password,
                        onValueChange = { viewModel.onSignupFieldChanged("password", it) },
                        isPassword = true,
                        imeAction = ImeAction.Next,
                        required = true,
                    )

                    AqualleraInput(
                        label = "Confirm Password",
                        value = state.confirmPassword,
                        onValueChange = { viewModel.onSignupFieldChanged("confirmPassword", it) },
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
                                text = "Already have an account?",
                                color = Color.Gray,
                                fontSize = 14.sp,
                            )
                            Text(
                                text = "Log In",
                                color = MidnightBlue,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable { onNavigateToLogin() },
                            )
                        }

                        PrimaryButton(
                            text = "Sign Up",
                            onClick = { viewModel.signup() },
                            loading = state.loading,
                            buttonWidth = 140,
                        )
                    }
                }
            }
        }
    }
}
