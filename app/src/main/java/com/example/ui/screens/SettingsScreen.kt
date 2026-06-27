package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.EnergyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: EnergyViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Observe setting states
    val webAppUrl by viewModel.googleAppsScriptUrl.collectAsState()
    val tariff by viewModel.electricityTariff.collectAsState()
    val interval by viewModel.refreshIntervalSec.collectAsState()
    val vMin by viewModel.voltageMin.collectAsState()
    val vMax by viewModel.voltageMax.collectAsState()
    val cMax by viewModel.currentMax.collectAsState()
    val pMax by viewModel.powerMax.collectAsState()
    val pfMin by viewModel.pfMin.collectAsState()
    val theme by viewModel.themeMode.collectAsState()

    // Temporary input buffers
    var urlInput by remember(webAppUrl) { mutableStateOf(webAppUrl) }
    var tariffInput by remember(tariff) { mutableStateOf(tariff.toString()) }
    var vMinInput by remember(vMin) { mutableStateOf(vMin.toString()) }
    var vMaxInput by remember(vMax) { mutableStateOf(vMax.toString()) }
    var cMaxInput by remember(cMax) { mutableStateOf(cMax.toString()) }
    var pMaxInput by remember(pMax) { mutableStateOf(pMax.toString()) }
    var pfMinInput by remember(pfMin) { mutableStateOf(pfMin.toString()) }

    var isEditingLimit by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Google Sheet API Connectivity
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudSync, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GAS CLOUD CONFIGURATION", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                }

                Text(
                    text = "Insert your Google Sheets URL (Shared as 'Anyone with link can view') or Google Apps Script Web App URL to stream real electrical variables.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                )

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Google Sheets / Web App URL") },
                    placeholder = { Text("https://docs.google.com/spreadsheets/d/...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.setGoogleAppsScriptUrl(urlInput.trim())
                            Toast.makeText(context, "Cloud URL Saved Successfully", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("SAVE & CONNECT", fontWeight = FontWeight.Bold)
                    }

                    if (urlInput.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                urlInput = ""
                                viewModel.setGoogleAppsScriptUrl("")
                                Toast.makeText(context, "Disconnected", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("DISCONNECT", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: System Update Intervals
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TELEMETRY FETCH RATE", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                }

                Text(
                    text = "Select how frequently the SCADA system polls Google Sheet / NodeMCU buffers.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(2, 5, 10, 30, 60).forEach { sec ->
                        val isSelected = interval == sec
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    viewModel.setRefreshIntervalSec(sec)
                                    Toast.makeText(context, "Refresh interval set to ${sec}s", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        ) {
                            Text(
                                text = "${sec}s",
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Section: Thresholds & Alarms Config
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = Color(0xFFEF4444), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SCADA ALARM THRESHOLDS", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                }

                Text(
                    text = "Adjust boundaries for triggering high/low alarm telemetry flags.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = vMinInput,
                            onValueChange = { vMinInput = it },
                            label = { Text("Min Voltage (V)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = vMaxInput,
                            onValueChange = { vMaxInput = it },
                            label = { Text("Max Voltage (V)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = cMaxInput,
                            onValueChange = { cMaxInput = it },
                            label = { Text("Max Current (A)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = pMaxInput,
                            onValueChange = { pMaxInput = it },
                            label = { Text("Max Power (W)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = pfMinInput,
                        onValueChange = { pfMinInput = it },
                        label = { Text("Minimum Power Factor (cos φ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val vm = vMinInput.toFloatOrNull() ?: 210f
                            val vx = vMaxInput.toFloatOrNull() ?: 250f
                            val cm = cMaxInput.toFloatOrNull() ?: 15.0f
                            val pm = pMaxInput.toFloatOrNull() ?: 3000f
                            val pfm = pfMinInput.toFloatOrNull() ?: 0.85f

                            viewModel.setVoltageLimits(vm, vx)
                            viewModel.setCurrentMax(cm)
                            viewModel.setPowerMax(pm)
                            viewModel.setPfMin(pfm)

                            Toast.makeText(context, "Alarm Thresholds Updated", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("APPLY LIMITS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section: Electricity Tariff
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AttachMoney, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ELECTRICITY TARIFF", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                }

                OutlinedTextField(
                    value = tariffInput,
                    onValueChange = { tariffInput = it },
                    label = { Text("Rate per kWh ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val tf = tariffInput.toFloatOrNull() ?: 0.15f
                        viewModel.setElectricityTariff(tf)
                        Toast.makeText(context, "Tariff Updated to $$tf/kWh", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("UPDATE TARIFF", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Instructions deployment help card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("DEPLOYMENT TUTORIAL", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary))
                Text(
                    text = "1. Copy the full script code from the project's root google_apps_script.js.\n" +
                            "2. In Google Sheet, click Extensions -> Apps Script.\n" +
                            "3. Paste the code, then deploy as a Web App accessible to 'Anyone'.\n" +
                            "4. Copy your Web App URL and paste it in GAS Cloud Configuration above.",
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp)
                )
                Button(
                    onClick = {
                        // Normally we copy the script, but since we are inside the client we can copy a reference link or trigger copying info
                        clipboardManager.setText(AnnotatedString("Google Apps Script code is placed in google_apps_script.js at the project root folder. Open and copy!"))
                        Toast.makeText(context, "Instructions copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("COPY SCRIPT DIRECTORY INFO", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Cache utilities & Theme config
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    viewModel.clearDatabase()
                    Toast.makeText(context, "Local DB Cache Cleared", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.DeleteForever, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("CLEAR CACHE", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    val mode = if (theme == "DARK") "LIGHT" else "DARK"
                    viewModel.setThemeMode(mode)
                    Toast.makeText(context, "Theme toggled to $mode", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Contrast, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("TOGGLE THEME", fontWeight = FontWeight.Bold)
            }
        }

        // About the expert architecture
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ENERGY MONITOR PRO v1.0",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                )
                Text(
                    text = "Designed for Schneider, Siemens & ABB Industrial Operations",
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), textAlign = TextAlign.Center)
                )
            }
        }
    }
}
