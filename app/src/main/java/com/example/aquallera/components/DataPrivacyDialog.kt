package com.example.aquallera.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aquallera.R
import com.example.aquallera.core.theme.MidnightBlue

/** The six privacy sections, shared with the About screen so the two never drift apart. */
val DATA_PRIVACY_SECTIONS: List<Pair<String, String>> = listOf(
    "1. Information We Collect" to
        "When you create an account and use Aquallera, we collect the following personal information: " +
        "your full name, email address, phone number, delivery address and location data (when you " +
        "place a delivery order), and your order history including water type preferences, quantities, " +
        "and payment method. This information is necessary to provide and fulfill our services to you.",
    "2. How We Use Your Information" to
        "We use your information solely to process and fulfill your water orders, send you order " +
        "confirmations and status updates, communicate with you about account-related matters, " +
        "provide customer support, and improve our services. We do not sell, rent, or share your " +
        "personal information with third parties for marketing purposes. Your data is used only " +
        "within the Aquallera platform to deliver the services you request.",
    "3. Data Storage and Security" to
        "Your data is stored securely in Firebase Realtime Database, which employs encryption " +
        "in transit (HTTPS) and at rest. Access to your data is restricted to authenticated users " +
        "and authorized administrators. We implement reasonable security measures to protect your " +
        "personal information from unauthorized access, alteration, or disclosure.",
    "4. Account Deletion" to
        "When you delete your account through the app, all your personal data including your " +
        "name, email address, phone number, and delivery addresses are permanently removed from " +
        "our database. To delete your account, all your orders must first be completed or " +
        "cancelled. Your Firebase Authentication account is also deleted. This action " +
        "is irreversible and cannot be undone. You will need to create a new account if you wish " +
        "to use Aquallera again.",
    "5. Your Rights" to
        "You have the right to access your personal data at any time through your profile page. " +
        "You may update or correct your information using the edit profile feature. You may " +
        "delete your account and all associated data at any time. If you have any questions or " +
        "concerns about your data, you may contact us using the information below.",
    "6. Contact Us" to
        "If you have any questions about this privacy notice or how we handle your data, please " +
        "contact us at sojodecaran200@gmail.com.",
)

/** Full-screen data privacy notice shown right after email verification. */
@Composable
fun DataPrivacyDialog(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_no_name),
                    contentDescription = "Aquallera",
                    modifier = Modifier.size(64.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Data Privacy Notice",
                    color = MidnightBlue,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            DATA_PRIVACY_SECTIONS.forEach { (heading, body) ->
                Text(
                    text = heading,
                    color = MidnightBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    color = Color.Gray,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        HorizontalDivider(color = Color.LightGray)
        PrimaryButton(
            text = "Continue",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            buttonWidth = 350,
        )
    }
}
