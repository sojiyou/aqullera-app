package com.example.aquallera.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.aquallera.core.theme.Blue
import com.example.aquallera.core.theme.LightYellow
import com.example.aquallera.core.theme.MidnightBlue

@Composable
fun WaveBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBlue),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawWaves()
        }
    }
}

private fun DrawScope.drawWaves() {
    val w = size.width
    val h = size.height

    // Gold wave 1 (top)
    drawWave(
        color = LightYellow.copy(alpha = 0.1f),
        points = listOf(
            Offset(0f, h * 0.22f),
            Offset(w * 0.1f, h * 0.25f),
            Offset(w * 0.2f, h * 0.28f),
            Offset(w * 0.3f, h * 0.27f),
            Offset(w * 0.4f, h * 0.24f),
            Offset(w * 0.5f, h * 0.23f),
            Offset(w * 0.6f, h * 0.26f),
            Offset(w * 0.7f, h * 0.27f),
            Offset(w * 0.8f, h * 0.25f),
            Offset(w * 0.9f, h * 0.22f),
            Offset(w, h * 0.20f),
        ),
    )

    // Gold wave 2 (middle)
    drawWave(
        color = LightYellow.copy(alpha = 0.1f),
        points = listOf(
            Offset(0f, h * 0.39f),
            Offset(w * 0.1f, h * 0.37f),
            Offset(w * 0.2f, h * 0.35f),
            Offset(w * 0.3f, h * 0.36f),
            Offset(w * 0.4f, h * 0.40f),
            Offset(w * 0.5f, h * 0.42f),
            Offset(w * 0.6f, h * 0.40f),
            Offset(w * 0.7f, h * 0.37f),
            Offset(w * 0.8f, h * 0.38f),
            Offset(w * 0.9f, h * 0.40f),
            Offset(w, h * 0.41f),
        ),
    )

    // White wave (bottom)
    drawWave(
        color = Color.White.copy(alpha = 0.1f),
        points = listOf(
            Offset(0f, h * 0.58f),
            Offset(w * 0.1f, h * 0.60f),
            Offset(w * 0.2f, h * 0.64f),
            Offset(w * 0.3f, h * 0.63f),
            Offset(w * 0.4f, h * 0.59f),
            Offset(w * 0.5f, h * 0.57f),
            Offset(w * 0.6f, h * 0.60f),
            Offset(w * 0.7f, h * 0.62f),
            Offset(w * 0.8f, h * 0.61f),
            Offset(w * 0.9f, h * 0.58f),
            Offset(w, h * 0.56f),
        ),
    )
}

private fun DrawScope.drawWave(color: Color, points: List<Offset>) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
        lineTo(points.last().x, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(path, color = color)
}
