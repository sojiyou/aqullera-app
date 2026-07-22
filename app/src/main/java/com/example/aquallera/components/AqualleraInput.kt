package com.example.aquallera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aquallera.core.theme.Gray600
import com.example.aquallera.core.theme.InputBg
import com.example.aquallera.core.theme.MidnightBlue

@Composable
fun AqualleraInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    type: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    isPassword: Boolean = false,
    required: Boolean = false,
    error: String? = null,
) {
    var isFocused by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    val isActive = isFocused || value.isNotEmpty()

    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(InputBg)
                .onFocusChanged { isFocused = it.isFocused },
            contentAlignment = Alignment.CenterStart,
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = if (isPassword) 48.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (isActive) {
                        Text(
                            text = "$label${if (required) " *" else ""}",
                            color = Gray600,
                            fontSize = 12.sp,
                        )
                    }
                    val basicTextField = androidx.compose.foundation.text.BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = MidnightBlue,
                            fontSize = if (isActive) 16.sp else 16.sp,
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = type,
                            imeAction = imeAction,
                        ),
                        keyboardActions = KeyboardActions(onDone = { onImeAction() }),
                        visualTransformation = if (isPassword && !showPassword) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = if (isActive) 16.dp else 0.dp),
                    )
                    if (!isActive) {
                        Text(
                            text = "$label${if (required) " *" else ""}",
                            color = Gray600,
                            fontSize = 16.sp,
                            modifier = Modifier.align(Alignment.CenterStart),
                        )
                    }
                    basicTextField
                }
            }

            if (isPassword && value.isNotEmpty()) {
                IconButton(
                    onClick = { showPassword = !showPassword },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .size(40.dp),
                ) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showPassword) "Hide password" else "Show password",
                        tint = Gray600,
                    )
                }
            }
        }

        if (error != null) {
            Text(
                text = error,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 56.dp, start = 16.dp),
            )
        }
    }
}
