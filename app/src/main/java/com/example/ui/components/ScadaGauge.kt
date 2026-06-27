package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ScadaGauge(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    unit: String,
    modifier: Modifier = Modifier,
    alarmLow: Float? = null,
    alarmHigh: Float? = null,
    color: Color = Color(0xFF00E5FF)
) {
    val animatedValue by animateFloatAsState(
        targetValue = value.coerceIn(min, max),
        animationSpec = tween(durationMillis = 800),
        label = "GaugeValue"
    )

    // Check alarm status
    val isAlarm = (alarmLow != null && value < alarmLow) || (alarmHigh != null && value > alarmHigh)
    val gaugeColor = if (isAlarm) Color(0xFFFF3D00) else color

    // Background panel color matching Sophisticated style
    val panelBg = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface

    Column(
        modifier = modifier
            .background(panelBg, RoundedCornerShape(24.dp))
            .border(1.dp, if (isAlarm) Color(0xFFFF3D00).copy(alpha = 0.4f) else outlineColor, RoundedCornerShape(24.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = if (isAlarm) Color(0xFFFF3D00) else MaterialTheme.colorScheme.onSurfaceVariant
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .size(110.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 10.dp.toPx()
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.width - strokeWidth) / 2f

                // Base grey track arc from 135 to 45 degrees (270 degree sweep)
                drawArc(
                    color = Color.Gray.copy(alpha = 0.2f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Colored active arc based on ratio
                val sweepAngle = ((animatedValue - min) / (max - min)) * 270f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            gaugeColor.copy(alpha = 0.6f),
                            gaugeColor
                        )
                    ),
                    startAngle = 135f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Needle indicator
                val angleRad = Math.toRadians((135f + sweepAngle).toDouble())
                val needleLength = radius - 15f
                val endX = center.x + needleLength * cos(angleRad).toFloat()
                val endY = center.y + needleLength * sin(angleRad).toFloat()

                // Draw needle line
                drawLine(
                    color = if (isAlarm) Color(0xFFFF3D00) else onSurface,
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Needle center cap
                drawCircle(
                    color = if (isAlarm) Color(0xFFFF3D00) else onSurface,
                    radius = 6.dp.toPx()
                )
                drawCircle(
                    color = surfaceColor,
                    radius = 2.dp.toPx()
                )
            }

            // Digital readout under cap inside dial
            Column(
                modifier = Modifier.offset(y = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val formattedVal = String.format("%.1f", value)
                Text(
                    text = formattedVal,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = if (isAlarm) Color(0xFFFF3D00) else MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        }

        if (isAlarm) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .background(Color(0xFFFF3D00).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .border(0.5.dp, Color(0xFFFF3D00), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (alarmHigh != null && value > alarmHigh) "HIGH ALARM" else "LOW ALARM",
                    color = Color(0xFFFF3D00),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                )
            }
        } else {
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
