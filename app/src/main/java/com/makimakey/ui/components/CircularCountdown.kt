package com.makimakey.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.makimakey.ui.theme.DarkGray

@Composable
fun CircularCountdown(
    remainingSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier
) {
    val progress = remainingSeconds.toFloat() / totalSeconds.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300),
        label = "countdown_progress"
    )

    Box(
        modifier = modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(48.dp)) {
            val strokeWidth = 4.dp.toPx()

            drawCircle(
                color = DarkGray,
                style = Stroke(width = strokeWidth)
            )

            drawArc(
                color = if (remainingSeconds <= 5) {
                    androidx.compose.ui.graphics.Color(0xFFCF6679)
                } else {
                    androidx.compose.ui.graphics.Color.White
                },
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Text(
            text = remainingSeconds.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = if (remainingSeconds <= 5) {
                androidx.compose.ui.graphics.Color(0xFFCF6679)
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
    }
}
