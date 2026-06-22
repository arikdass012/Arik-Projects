package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.model.FaultType
import com.example.ui.components.*
import com.example.viewmodel.GridGuardViewModel

@Composable
fun DashboardScreen(
    viewModel: GridGuardViewModel,
    isWideScreen: Boolean,
    modifier: Modifier = Modifier
) {
    val isSimulating by viewModel.isSimulating.collectAsState()
    val speed by viewModel.speedMultiplier.collectAsState()
    val injectionSelection by viewModel.injectionSelection.collectAsState()
    val injectedPhase by viewModel.injectedPhase.collectAsState()

    val currentPacket by viewModel.currentPacket.collectAsState()
    val waveforms by viewModel.currentWaveforms.collectAsState()
    val faultEvents by viewModel.faultEvents.collectAsState()

    val totalFaults = faultEvents.size
    val activeFaultLabel = if (totalFaults > 0) faultEvents.first().faultType.name else "NORMAL"

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top status card
        SystemStatusBar(
            isSimulating = isSimulating,
            uptimePercent = viewModel.getUptimePercentage(),
            uptimeSeconds = viewModel.getUptimeSeconds(),
            faultCount = faultEvents.count { !it.resolved },
            activeFaultLabel = activeFaultLabel
        )

        if (isWideScreen) {
            // Adaptive Grid/Row layout for spacious screens
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Raw signals and spectrum
                Column(
                    modifier = Modifier.weight(1.3f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    WaveformPanel(points = waveforms, modifier = Modifier.fillMaxWidth())
                    FftSpectrumPanel(fftData = currentPacket?.fftData, modifier = Modifier.fillMaxWidth())
                }

                // Right Column: Health circular dials and engine controls
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GaugePanel(packet = currentPacket, modifier = Modifier.fillMaxWidth())
                    ControlPanel(
                        isSimulating = isSimulating,
                        speed = speed,
                        injection = injectionSelection,
                        injectedPhase = injectedPhase,
                        onToggleSim = { viewModel.toggleSimulation(it) },
                        onSetSpeed = { viewModel.setSpeedMultiplier(it) },
                        onSetInjection = { viewModel.setInjectionSelection(it) },
                        onInjectNow = { viewModel.injectFault() },
                        onExportHistory = { viewModel.exportHistoryToCsv(viewModel.getApplication()) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    FaultFeedPanel(events = faultEvents, modifier = Modifier.fillMaxWidth())
                }
            }
        } else {
            // Sequential block layout for handheld portrait devices
            WaveformPanel(points = waveforms, modifier = Modifier.fillMaxWidth())
            FftSpectrumPanel(fftData = currentPacket?.fftData, modifier = Modifier.fillMaxWidth())
            GaugePanel(packet = currentPacket, modifier = Modifier.fillMaxWidth())
            ControlPanel(
                isSimulating = isSimulating,
                speed = speed,
                injection = injectionSelection,
                injectedPhase = injectedPhase,
                onToggleSim = { viewModel.toggleSimulation(it) },
                onSetSpeed = { viewModel.setSpeedMultiplier(it) },
                onSetInjection = { viewModel.setInjectionSelection(it) },
                onInjectNow = { viewModel.injectFault() },
                onExportHistory = { viewModel.exportHistoryToCsv(viewModel.getApplication()) },
                modifier = Modifier.fillMaxWidth()
            )
            FaultFeedPanel(events = faultEvents, modifier = Modifier.fillMaxWidth())
        }
    }
}
