package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

enum class ChartType {
    LINE, BAR
}

@Composable
fun ScadaChart(
    title: String,
    dataPoints: List<Pair<Long, Float>>, // Pair(Timestamp, Value)
    unit: String,
    modifier: Modifier = Modifier,
    chartType: ChartType = ChartType.LINE,
    lineColor: Color = Color(0xFF00E5FF),
    showRollingAverage: Boolean = false,
    rollingWindowSize: Int = 5
) {
    var scaleX by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var selectedIndex by remember { mutableStateOf(-1) }

    val panelBg = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
    val textStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold
    )

    // Calculate rolling average
    val rollingAveragePoints = remember(dataPoints, showRollingAverage, rollingWindowSize) {
        if (!showRollingAverage || dataPoints.size < rollingWindowSize) emptyList()
        else {
            val list = mutableListOf<Pair<Long, Float>>()
            for (i in dataPoints.indices) {
                if (i < rollingWindowSize - 1) {
                    list.add(dataPoints[i])
                } else {
                    var sum = 0f
                    for (j in 0 until rollingWindowSize) {
                        sum += dataPoints[i - j].second
                    }
                    list.add(Pair(dataPoints[i].first, sum / rollingWindowSize))
                }
            }
            list
        }
    }

    Column(
        modifier = modifier
            .background(panelBg, RoundedCornerShape(24.dp))
            .border(1.dp, outlineColor, RoundedCornerShape(24.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )
                if (dataPoints.isNotEmpty()) {
                    val maxVal = dataPoints.maxOf { it.second }
                    val minVal = dataPoints.minOf { it.second }
                    Text(
                        text = "Range: ${String.format("%.1f", minVal)} - ${String.format("%.1f", maxVal)} $unit",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            // Controls display
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "ZOOM: ${String.format("%.1fx", scaleX)} (Drag/Pinch)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (dataPoints.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO TELEMETRY DATA RECORDED",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(dataPoints) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scaleX = (scaleX * zoom).coerceIn(1f, 10f)
                                offsetX = (offsetX + pan.x).coerceIn(-size.width * (scaleX - 1f), 0f)
                            }
                        }
                        .pointerInput(dataPoints) {
                            detectTapGestures { tapOffset ->
                                val chartWidth = size.width - 100f
                                val clickX = tapOffset.x - 70f
                                if (clickX in 0f..chartWidth) {
                                    val percent = (clickX - offsetX) / (chartWidth * scaleX)
                                    val index = (percent * (dataPoints.size - 1))
                                        .coerceIn(0f, (dataPoints.size - 1).toFloat())
                                        .toInt()
                                    selectedIndex = index
                                }
                            }
                        }
                ) {
                    val paddingLeft = 70f
                    val paddingRight = 30f
                    val paddingTop = 20f
                    val paddingBottom = 40f

                    val chartWidth = size.width - paddingLeft - paddingRight
                    val chartHeight = size.height - paddingTop - paddingBottom

                    // Value ranges
                    val minVal = dataPoints.minOf { it.second }
                    val maxVal = dataPoints.maxOf { it.second }
                    val valDiff = maxVal - minVal
                    val valueRangeMin = if (valDiff == 0f) minVal - 1f else minVal - valDiff * 0.1f
                    val valueRangeMax = if (valDiff == 0f) maxVal + 1f else maxVal + valDiff * 0.1f
                    val valueSpan = valueRangeMax - valueRangeMin

                    // Draw grid lines (horizontal / value divisions)
                    val horizontalGridLines = 4
                    for (i in 0..horizontalGridLines) {
                        val fraction = i.toFloat() / horizontalGridLines
                        val y = paddingTop + chartHeight - (fraction * chartHeight)
                        val gridValue = valueRangeMin + (fraction * valueSpan)

                        // Line
                        drawLine(
                            color = gridColor,
                            start = Offset(paddingLeft, y),
                            end = Offset(size.width - paddingRight, y),
                            strokeWidth = 1f
                        )

                        // Label
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format("%.1f", gridValue),
                            10f,
                            y + 4f,
                            android.graphics.Paint().apply {
                                color = textStyle.color.toArgb()
                                textSize = 24f
                                typeface = android.graphics.Typeface.MONOSPACE
                                isFakeBoldText = true
                            }
                        )
                    }

                    // Clip to Chart Area to support proper scrolling with zoom
                    clipRect(
                        left = paddingLeft,
                        top = paddingTop - 10f,
                        right = size.width - paddingRight,
                        bottom = size.height - paddingBottom + 5f
                    ) {
                        val numPoints = dataPoints.size
                        val stepX = (chartWidth * scaleX) / (numPoints - 1).coerceAtLeast(1)

                        if (chartType == ChartType.LINE) {
                            val path = Path()
                            val fillPath = Path()

                            for (i in dataPoints.indices) {
                                val point = dataPoints[i]
                                val x = paddingLeft + offsetX + (i * stepX)
                                val y = paddingTop + chartHeight - ((point.second - valueRangeMin) / valueSpan * chartHeight)

                                if (i == 0) {
                                    path.moveTo(x, y)
                                    fillPath.moveTo(x, paddingTop + chartHeight)
                                    fillPath.lineTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                    fillPath.lineTo(x, y)
                                }

                                if (i == dataPoints.size - 1) {
                                    fillPath.lineTo(x, paddingTop + chartHeight)
                                    fillPath.close()
                                }
                            }

                            // Draw gradient under line
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        lineColor.copy(alpha = 0.35f),
                                        Color.Transparent
                                    )
                                )
                            )

                            // Draw line
                            drawPath(
                                path = path,
                                color = lineColor,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Draw rolling average if requested
                            if (showRollingAverage && rollingAveragePoints.isNotEmpty()) {
                                val rollPath = Path()
                                for (i in rollingAveragePoints.indices) {
                                    val point = rollingAveragePoints[i]
                                    val x = paddingLeft + offsetX + (i * stepX)
                                    val y = paddingTop + chartHeight - ((point.second - valueRangeMin) / valueSpan * chartHeight)
                                    if (i == 0) rollPath.moveTo(x, y) else rollPath.lineTo(x, y)
                                }
                                drawPath(
                                    path = rollPath,
                                    color = Color(0xFFFF9100), // Amber
                                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                                )
                            }

                        } else {
                            // Bar chart (e.g. daily/monthly aggregates)
                            val barWidth = (stepX * 0.7f).coerceIn(4f, 40f)
                            for (i in dataPoints.indices) {
                                val point = dataPoints[i]
                                val x = paddingLeft + offsetX + (i * stepX) - (barWidth / 2f)
                                val y = paddingTop + chartHeight - ((point.second - valueRangeMin) / valueSpan * chartHeight)

                                drawRect(
                                    color = lineColor,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, (paddingTop + chartHeight) - y),
                                    style = Fill
                                )
                            }
                        }

                        // Selected point indicator (vertical crosshair)
                        if (selectedIndex in dataPoints.indices) {
                            val selPoint = dataPoints[selectedIndex]
                            val x = paddingLeft + offsetX + (selectedIndex * stepX)
                            val y = paddingTop + chartHeight - ((selPoint.second - valueRangeMin) / valueSpan * chartHeight)

                            drawLine(
                                color = Color.White.copy(alpha = 0.6f),
                                start = Offset(x, paddingTop),
                                end = Offset(x, paddingTop + chartHeight),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )

                            drawCircle(
                                color = Color.White,
                                radius = 6.dp.toPx(),
                                center = Offset(x, y)
                            )

                            drawCircle(
                                color = lineColor,
                                radius = 3.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }

                    // Draw X Axis labels
                    val dateSdf = SimpleDateFormat("HH:mm:ss", Locale.US)
                    val labelCount = 3
                    val stepIndex = (dataPoints.size / labelCount).coerceAtLeast(1)
                    for (i in dataPoints.indices step stepIndex) {
                        val numPoints = dataPoints.size
                        val stepX = (chartWidth * scaleX) / (numPoints - 1).coerceAtLeast(1)
                        val x = paddingLeft + offsetX + (i * stepX)

                        if (x in paddingLeft..(size.width - paddingRight)) {
                            val timeStr = try {
                                dateSdf.format(Date(dataPoints[i].first))
                            } catch (e: Exception) {
                                "N/A"
                            }
                            drawContext.canvas.nativeCanvas.drawText(
                                timeStr,
                                x - 40f,
                                size.height - 10f,
                                android.graphics.Paint().apply {
                                    color = textStyle.color.toArgb()
                                    textSize = 22f
                                    typeface = android.graphics.Typeface.MONOSPACE
                                }
                             )
                        }
                    }
                }

                // Interactive Tooltip overlay inside Box scope
                if (selectedIndex in dataPoints.indices) {
                    val point = dataPoints[selectedIndex]
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    val timeStr = sdf.format(Date(point.first))

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp)
                            .background(MaterialTheme.colorScheme.inverseSurface, RoundedCornerShape(8.dp))
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${String.format("%.2f", point.second)} $unit",
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                            Text(
                                text = timeStr,
                                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
