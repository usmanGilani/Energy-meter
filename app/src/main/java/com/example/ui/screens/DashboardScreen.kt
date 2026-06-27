package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.ui.components.ScadaGauge
import com.example.ui.components.ScadaChart
import com.example.ui.components.ChartType
import com.example.viewmodel.EnergyViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: EnergyViewModel,
    modifier: Modifier = Modifier
) {
    val latest by viewModel.latestRecord.collectAsState()
    val isDemo by viewModel.isDemoMode.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val connError by viewModel.connectionError.collectAsState()

    // Alarm statuses
    val hiVolt by viewModel.hasHighVoltageAlarm.collectAsState()
    val loVolt by viewModel.hasLowVoltageAlarm.collectAsState()
    val overload by viewModel.hasOverloadAlarm.collectAsState()
    val poorPf by viewModel.hasPoorPfAlarm.collectAsState()

    // Aggregate stats
    val todayEnergy by viewModel.todayEnergyKWh.collectAsState()
    val monthlyEnergy by viewModel.monthlyEnergyKWh.collectAsState()
    val runningCost by viewModel.runningCostVal.collectAsState()
    val co2Val by viewModel.co2EmissionsVal.collectAsState()
    val maxDemand by viewModel.maxPowerDemand.collectAsState()

    // Bar chart points
    val dailyPoints by viewModel.dailyEnergy.collectAsState()
    val monthlyPoints by viewModel.monthlyEnergy.collectAsState()

    // Settings values for alarms
    val vMin by viewModel.voltageMin.collectAsState()
    val vMax by viewModel.voltageMax.collectAsState()
    val pMax by viewModel.powerMax.collectAsState()
    val pfMin by viewModel.pfMin.collectAsState()

    val scrollState = rememberScrollState()

    // Tab state to toggle between KPI Dashboard and Gauges
    var activeTab by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // System Health & Connection Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    if (hiVolt || loVolt || overload || poorPf) Color(0xFFEF4444) else Color(0xFF10B981),
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hiVolt || loVolt || overload || poorPf) "SYSTEM ALARM ACTIVE" else "SYSTEM STABLE",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (hiVolt || loVolt || overload || poorPf) Color(0xFFEF4444) else Color(0xFF10B981)
                            )
                        )
                    }

                    // Demo / Real Status Indicator
                    Box(
                        modifier = Modifier
                            .background(
                                if (isDemo) Color(0xFFFF9100).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                                RoundedCornerShape(4.dp)
                            )
                            .border(
                                0.5.dp,
                                if (isDemo) Color(0xFFFF9100) else Color(0xFF10B981),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isDemo) "DEMO MODE" else "GOOGLE SHEET LIVE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = if (isDemo) Color(0xFFFF9100) else Color(0xFF10B981)
                            )
                        )
                    }
                }

                // Show active alarm warnings
                val alarmText = mutableListOf<String>()
                if (hiVolt) alarmText.add("Overvoltage detected (> $vMax V)")
                if (loVolt) alarmText.add("Undervoltage detected (< $vMin V)")
                if (overload) alarmText.add("System overload warning (> $pMax W)")
                if (poorPf) alarmText.add("Low displacement power factor (< $pfMin)")

                if (alarmText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEF4444).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, Color(0xFFEF4444), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        alarmText.forEach { text ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alarm",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = text,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFFEF4444),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                if (connError != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFEF4444).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CloudOff, "Sync Error", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "COMMUNICATION FAILURE: $connError",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LAST UPDATE",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (latest != null) {
                                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(latest!!.timestamp))
                            } else "Syncing...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    IconButton(
                        onClick = { viewModel.triggerManualRefresh() },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Manual Refresh", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }

        // Tab Selector (KPI vs Gauges)
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = Color.Transparent,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = { Text("KPI MONITOR", fontWeight = FontWeight.Black) }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = { Text("ANALOG DIALS", fontWeight = FontWeight.Black) }
            )
        }

        if (activeTab == 0) {
            // KPI Grid
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Primary Electric parameters row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Voltage",
                        value = "${String.format("%.1f", latest?.voltage ?: 230f)} V",
                        status = if (hiVolt) "HIGH" else if (loVolt) "LOW" else "NORMAL",
                        statusColor = if (hiVolt || loVolt) Color(0xFFEF4444) else Color(0xFF10B981),
                        icon = Icons.Default.Bolt,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Current",
                        value = "${String.format("%.2f", latest?.current ?: 4.2f)} A",
                        status = "NORMAL",
                        statusColor = Color(0xFF10B981),
                        icon = Icons.Default.ElectricMeter,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Active Power",
                        value = "${String.format("%.1f", latest?.power ?: 966f)} W",
                        status = if (overload) "OVERLOAD" else "NORMAL",
                        statusColor = if (overload) Color(0xFFEF4444) else Color(0xFF10B981),
                        icon = Icons.Default.OfflineBolt,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Total Energy",
                        value = "${String.format("%.2f", latest?.energy ?: 124.5f)} kWh",
                        status = "CUMULATIVE",
                        statusColor = MaterialTheme.colorScheme.primary,
                        icon = Icons.Default.EnergySavingsLeaf,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Frequency",
                        value = "${String.format("%.2f", latest?.frequency ?: 50.0f)} Hz",
                        status = "STABLE",
                        statusColor = Color(0xFF10B981),
                        icon = Icons.Default.GraphicEq,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Power Factor",
                        value = String.format("%.2f", latest?.powerFactor ?: 0.92f),
                        status = if (poorPf) "POOR" else "IDEAL",
                        statusColor = if (poorPf) Color(0xFFF59E0B) else Color(0xFF10B981),
                        icon = Icons.Default.Power,
                        modifier = Modifier.weight(1f)
                    )
                }



                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Today's Energy",
                        value = "${String.format("%.2f", todayEnergy)} kWh",
                        status = "DAILY COUNT",
                        statusColor = MaterialTheme.colorScheme.primary,
                        icon = Icons.Default.DateRange,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Maximum Demand",
                        value = "${String.format("%.1f", maxDemand)} W",
                        status = "PEAK RECORD",
                        statusColor = Color(0xFFFF9100),
                        icon = Icons.Default.ShowChart,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    KpiCard(
                        title = "Monthly Cost",
                        value = "$${String.format("%.2f", runningCost)}",
                        status = "ESTIMATED",
                        statusColor = Color(0xFF10B981),
                        icon = Icons.Default.AttachMoney,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "CO₂ Emissions",
                        value = "${String.format("%.1f", co2Val)} kg",
                        status = "CARBON FOOTPRINT",
                        statusColor = Color(0xFF81C784),
                        icon = Icons.Default.Co2,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            // Analog Gauges tab
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScadaGauge(
                        label = "Voltage",
                        value = latest?.voltage ?: 230f,
                        min = 180f,
                        max = 280f,
                        unit = "V",
                        alarmLow = vMin,
                        alarmHigh = vMax,
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.weight(1f)
                    )
                    ScadaGauge(
                        label = "Current",
                        value = latest?.current ?: 4.2f,
                        min = 0f,
                        max = 20f,
                        unit = "A",
                        alarmHigh = 15f,
                        color = Color(0xFFFF9100),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScadaGauge(
                        label = "Active Power",
                        value = latest?.power ?: 966f,
                        min = 0f,
                        max = 4000f,
                        unit = "W",
                        alarmHigh = pMax,
                        color = Color(0xFFE040FB),
                        modifier = Modifier.weight(1f)
                    )
                    ScadaGauge(
                        label = "Frequency",
                        value = latest?.frequency ?: 50.0f,
                        min = 45f,
                        max = 55f,
                        unit = "Hz",
                        alarmLow = 49.5f,
                        alarmHigh = 50.5f,
                        color = Color(0xFF00E676),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ScadaGauge(
                        label = "Power Factor",
                        value = latest?.powerFactor ?: 0.92f,
                        min = 0.5f,
                        max = 1.0f,
                        unit = "PF",
                        alarmLow = pfMin,
                        color = Color(0xFF2979FF),
                        modifier = Modifier.weight(1f)
                    )
                    ScadaGauge(
                        label = "Daily Energy",
                        value = todayEnergy,
                        min = 0f,
                        max = 50f,
                        unit = "kWh",
                        color = Color(0xFFFFD600),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Consumption aggregate bar charts
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "ACCUMULATED ENERGY DEMAND",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            )

            ScadaChart(
                title = "Daily Energy Consumption",
                dataPoints = dailyPoints,
                unit = "kWh",
                chartType = ChartType.BAR,
                lineColor = MaterialTheme.colorScheme.primary
            )

            ScadaChart(
                title = "Monthly Energy Consumption",
                dataPoints = monthlyPoints,
                unit = "kWh",
                chartType = ChartType.BAR,
                lineColor = Color(0xFF0D9488)
            )
        }
    }
}

@Composable
fun KpiCard(
    title: String,
    value: String,
    status: String,
    statusColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val cardColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Card(
        modifier = modifier
            .border(1.dp, outlineColor, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = statusColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = status.uppercase(),
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }
    }
}
