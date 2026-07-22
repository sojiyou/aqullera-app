package com.example.aquallera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aquallera.core.theme.AppBg
import com.example.aquallera.core.theme.MidnightBlue

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    buttonWidth: Int = 200,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Button(
            onClick = onClick,
            enabled = enabled && !loading,
            modifier = Modifier
                .width(buttonWidth.dp)
                .height(48.dp),
            shape = RoundedCornerShape(25.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MidnightBlue,
                contentColor = AppBg,
                disabledContainerColor = MidnightBlue.copy(alpha = 0.5f),
            ),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.height(20.dp),
                    color = AppBg,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(
                    text = text,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun LinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = Color.Gray.copy(alpha = 0.7f),
        fontSize = 12.sp,
        modifier = modifier.padding(8.dp),
        fontWeight = FontWeight.Normal,
    )
    Box(modifier = Modifier.height(0.dp))
}
