package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.EnergyViewModel

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_HISTORY = "history"
const val ROUTE_SETTINGS = "settings"

class MainActivity : ComponentActivity() {
    private val viewModel: EnergyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                ScadaAppContent(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScadaAppContent(viewModel: EnergyViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_DASHBOARD

    // Watch alarm states to color-code toolbar
    val hiVolt by viewModel.hasHighVoltageAlarm.collectAsState()
    val loVolt by viewModel.hasLowVoltageAlarm.collectAsState()
    val overload by viewModel.hasOverloadAlarm.collectAsState()
    val poorPf by viewModel.hasPoorPfAlarm.collectAsState()
    val hasAlarms = hiVolt || loVolt || overload || poorPf

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "ENERGY MONITOR PRO",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.2.sp
                                    )
                                )
                                Text(
                                    "INTELLIGENT SCADA POWER GRID",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (hasAlarms) Color(0xFFEF4444).copy(alpha = 0.12f) else MaterialTheme.colorScheme.background
                    ),
                    actions = {
                        val isSyncing by viewModel.isSyncing.collectAsState()
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(22.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else if (hasAlarms) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Active Alarm",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = "Connected",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    }
                )
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)
            }
        },
        bottomBar = {
            Column {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                NavigationBarItem(
                    selected = currentRoute == ROUTE_DASHBOARD,
                    onClick = {
                        if (currentRoute != ROUTE_DASHBOARD) {
                            navController.navigate(ROUTE_DASHBOARD) {
                                popUpTo(ROUTE_DASHBOARD) { inclusive = true }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )

                NavigationBarItem(
                    selected = currentRoute == ROUTE_HISTORY,
                    onClick = {
                        if (currentRoute != ROUTE_HISTORY) {
                            navController.navigate(ROUTE_HISTORY) {
                                popUpTo(ROUTE_DASHBOARD)
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )

                NavigationBarItem(
                    selected = currentRoute == ROUTE_SETTINGS,
                    onClick = {
                        if (currentRoute != ROUTE_SETTINGS) {
                            navController.navigate(ROUTE_SETTINGS) {
                                popUpTo(ROUTE_DASHBOARD)
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }
        }
    }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ROUTE_DASHBOARD,
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            composable(ROUTE_DASHBOARD) {
                DashboardScreen(viewModel = viewModel)
            }
            composable(ROUTE_HISTORY) {
                HistoryScreen(viewModel = viewModel)
            }
            composable(ROUTE_SETTINGS) {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
