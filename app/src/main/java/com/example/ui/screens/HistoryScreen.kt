package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PowerRecord
import com.example.viewmodel.EnergyViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: EnergyViewModel,
    modifier: Modifier = Modifier
) {
    val historyList by viewModel.filteredHistory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    // Alarm thresholds to highlight records in list
    val vMin by viewModel.voltageMin.collectAsState()
    val vMax by viewModel.voltageMax.collectAsState()
    val pMax by viewModel.powerMax.collectAsState()
    val pfMin by viewModel.pfMin.collectAsState()

    var selectedRecordForDetail by remember { mutableStateOf<PowerRecord?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Control Row: Search & Filters
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.searchQuery.value = it },
            placeholder = { Text("Search by parameter...") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Filter Selector Dropdown
            var showFilterMenu by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { showFilterMenu = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter: $filterType",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Icon(Icons.Default.FilterList, "Filter Menu", modifier = Modifier.size(16.dp))
                    }
                }

                DropdownMenu(
                    expanded = showFilterMenu,
                    onDismissRequest = { showFilterMenu = false }
                ) {
                    listOf("ALL", "ALARM", "VOLTAGE", "OVERLOAD").forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                viewModel.filterType.value = type
                                showFilterMenu = false
                            }
                        )
                    }
                }
            }

            // Sort Toggle Button
            IconButton(
                onClick = {
                    viewModel.sortOrder.value = if (sortOrder == "DESC") "ASC" else "DESC"
                },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = if (sortOrder == "DESC") Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = "Sort order",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Refresh trigger
            IconButton(
                onClick = { viewModel.triggerManualRefresh() },
                enabled = !isSyncing,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .size(40.dp)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, "Refresh Cache", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Header Row for the parameters list
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "TIMESTAMP",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1.5f)
            )
            Text(
                text = "VOLTS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
            Text(
                text = "POWER",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
            Text(
                text = "PF",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(0.8f),
                textAlign = TextAlign.End
            )
        }

        // Scrollable List of Records
        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "NO HISTORICAL DATA LOADED",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    )
                    Text(
                        text = "Pull to fetch or ensure sheet is connected",
                        style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(historyList) { record ->
                    val sdf = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
                    val timeString = sdf.format(Date(record.timestamp))

                    // Determine if record contains alarms
                    val isAlarm = record.voltage < vMin || record.voltage > vMax || record.power > pMax || record.powerFactor < pfMin
                    val itemBg = if (isAlarm) Color(0xFFFF3D00).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    val outlineColor = if (isAlarm) Color(0xFFFF3D00).copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(itemBg, RoundedCornerShape(16.dp))
                            .border(1.dp, outlineColor, RoundedCornerShape(16.dp))
                            .clickable { selectedRecordForDetail = record }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isAlarm) Color(0xFFFF3D00) else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1.5f)
                        )
                        Text(
                            text = String.format("%.1fV", record.voltage),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = if (record.voltage < vMin || record.voltage > vMax) Color(0xFFFF3D00) else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = String.format("%.1fW", record.power),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = if (record.power > pMax) Color(0xFFFF3D00) else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                        Text(
                            text = String.format("%.2f", record.powerFactor),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                color = if (record.powerFactor < pfMin) Color(0xFFFF9100) else MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }

        // Details Modal Dialog
        selectedRecordForDetail?.let { record ->
            val isAlarm = record.voltage < vMin || record.voltage > vMax || record.power > pMax || record.powerFactor < pfMin
            AlertDialog(
                onDismissRequest = { selectedRecordForDetail = null },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isAlarm) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isAlarm) Color(0xFFFF3D00) else Color(0xFF10B981)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Telemetry Diagnostics",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black)
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiagnosticRow("Timestamp", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(record.timestamp)))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        DiagnosticRow("Voltage (RMS)", "${String.format("%.2f", record.voltage)} V")
                        DiagnosticRow("Current", "${String.format("%.3f", record.current)} A")
                        DiagnosticRow("Active Power", "${String.format("%.1f", record.power)} W")
                        DiagnosticRow("Total Energy", "${String.format("%.3f", record.energy)} kWh")
                        DiagnosticRow("Frequency", "${String.format("%.2f", record.frequency)} Hz")
                        DiagnosticRow("Power Factor", String.format("%.3f", record.powerFactor))
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        // Diagnosis message
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isAlarm) Color(0xFFFF3D00).copy(alpha = 0.1f) else Color(0xFF10B981).copy(alpha = 0.1f),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(0.5.dp, if (isAlarm) Color(0xFFFF3D00) else Color(0xFF10B981), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (isAlarm) {
                                    val anomalies = mutableListOf<String>()
                                    if (record.voltage < vMin) anomalies.add("Undervoltage (< $vMin V)")
                                    if (record.voltage > vMax) anomalies.add("Overvoltage (> $vMax V)")
                                    if (record.power > pMax) anomalies.add("Power overload (> $pMax W)")
                                    if (record.powerFactor < pfMin) anomalies.add("Bad power factor (< $pfMin)")
                                    "ANOMALY WARNING: ${anomalies.joinToString(", ")}"
                                } else {
                                    "SYSTEM HEALTH: Nominal. All load parameters comply with regulatory guidelines."
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAlarm) Color(0xFFFF3D00) else Color(0xFF10B981)
                                )
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedRecordForDetail = null }) {
                        Text("CLOSE", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}
