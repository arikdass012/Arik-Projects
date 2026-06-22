package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.SlateBackground
import com.example.viewmodel.GridGuardViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

@Composable
fun MainAppContainer(
    viewModel: GridGuardViewModel = viewModel()
) {
    var activeTab by remember { mutableStateOf("LIVE_TELEMETRY") } // "LIVE_TELEMETRY" or "HISTORY"

    val isSimulating by viewModel.isSimulating.collectAsState()
    val speedMultiplier by viewModel.speedMultiplier.collectAsState()
    val faultEvents by viewModel.faultEvents.collectAsState()

    var uptimeSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(isSimulating) {
        while (isSimulating) {
            kotlinx.coroutines.delay(1000)
            uptimeSeconds++
        }
    }

    val formattedUptime = remember(uptimeSeconds) {
        val h = uptimeSeconds / 3600
        val m = (uptimeSeconds % 3600) / 60
        val s = uptimeSeconds % 60
        String.format("%02d:%02d:%02d", h, m, s)
    }

    val activeFaultCount = remember(faultEvents) {
        faultEvents.count { !it.resolved }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(SlateBackground)) {
        val isWide = maxWidth > 620.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = SlateBackground,
            topBar = {
                // Header Panel
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Row(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left status column
                        Column {
                            Text(
                                text = "GRIDGUARD PRO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Pulsing/Glowing green circle
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = if (isSimulating) NeonGreen else Color.Gray,
                                            shape = androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                                Text(
                                    text = if (isSimulating) "SIMULATING @ ${String.format("%.1f", speedMultiplier)}X" else "SIMULATION PAUSED",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = if (isSimulating) NeonGreen else Color.Gray,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Right state column
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "UPTIME: $formattedUptime",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "FAULTS: " + String.format("%02d", activeFaultCount),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                fontWeight = FontWeight.Bold,
                                color = if (activeFaultCount > 0) NeonRed else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            },
            bottomBar = {
                if (!isWide) {
                    // Modern Navigation Bar for standard mobile vertical dimensions
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = activeTab == "LIVE_TELEMETRY",
                            onClick = { activeTab = "LIVE_TELEMETRY" },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                            label = { Text("Live Telemetry") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonGreen,
                                selectedTextColor = NeonGreen,
                                indicatorColor = Color(0x3310B981),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                        NavigationBarItem(
                            selected = activeTab == "HISTORY",
                            onClick = { activeTab = "HISTORY" },
                            icon = { Icon(Icons.Default.History, contentDescription = null) },
                            label = { Text("Log Historian") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = NeonGreen,
                                selectedTextColor = NeonGreen,
                                indicatorColor = Color(0x3310B981),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWide) {
                    // Navigation Rail / Sidebar for wide aspect ratios (Tablet / Foldable / Landscape Emulator)
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        header = {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "CONSOLE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                        }
                    ) {
                        NavigationRailItem(
                            selected = activeTab == "LIVE_TELEMETRY",
                            onClick = { activeTab = "LIVE_TELEMETRY" },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                            label = { Text("Live Grid") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = NeonGreen,
                                indicatorColor = Color(0x3310B981),
                                unselectedIconColor = Color.Gray
                            )
                        )
                        NavigationRailItem(
                            selected = activeTab == "HISTORY",
                            onClick = { activeTab = "HISTORY" },
                            icon = { Icon(Icons.Default.History, contentDescription = null) },
                            label = { Text("Archive") },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = NeonGreen,
                                indicatorColor = Color(0x3310B981),
                                unselectedIconColor = Color.Gray
                            )
                        )
                    }
                }

                // Main screen routing switch
                Box(modifier = Modifier.weight(1f)) {
                    if (activeTab == "LIVE_TELEMETRY") {
                        DashboardScreen(viewModel = viewModel, isWideScreen = isWide)
                    } else {
                        HistoryScreen(viewModel = viewModel, isWideScreen = isWide)
                    }
                }
            }
        }
    }
}
