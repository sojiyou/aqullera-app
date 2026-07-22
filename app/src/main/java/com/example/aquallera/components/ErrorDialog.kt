package com.example.aquallera.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.aquallera.core.theme.MidnightBlue

@Composable
fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit,
    title: String = "Error",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = MidnightBlue)
            }
        },
    )
}
