package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.simulator.Classifier
import com.example.simulator.SignalSimulator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

class GridGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val simulator = SignalSimulator()
    private val classifier = Classifier()

    // Configuration / Control States
    private val _isSimulating = MutableStateFlow(true)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _speedMultiplier = MutableStateFlow(1.0)
    val speedMultiplier: StateFlow<Double> = _speedMultiplier.asStateFlow()

    private val _injectionSelection = MutableStateFlow(FaultType.NORMAL)
    val injectionSelection: StateFlow<FaultType> = _injectionSelection.asStateFlow()

    private val _injectedPhase = MutableStateFlow("A")
    val injectedPhase: StateFlow<String> = _injectedPhase.asStateFlow()

    // Telemetry Teleprocessing Output
    private val _currentPacket = MutableStateFlow<SignalPacket?>(null)
    val currentPacket: StateFlow<SignalPacket?> = _currentPacket.asStateFlow()

    private val _currentWaveforms = MutableStateFlow<List<WavePoint>>(emptyList())
    val currentWaveforms: StateFlow<List<WavePoint>> = _currentWaveforms.asStateFlow()

    // History & Alert States
    private val _faultEvents = MutableStateFlow<List<FaultEvent>>(emptyList())
    val faultEvents: StateFlow<List<FaultEvent>> = _faultEvents.asStateFlow()

    private val _systemHealthHistory = MutableStateFlow<List<Int>>(emptyList())
    val systemHealthHistory: StateFlow<List<Int>> = _systemHealthHistory.asStateFlow()

    // Simulation Loop Handle
    private var simulationJob: Job? = null
    private var overloadSecondsElapsed = 0.0
    private var startTimeMillis = System.currentTimeMillis()
    private var totalPacketsGenerated = 0
    private var healthyPacketsGenerated = 0

    // Currently injected fault state
    private var activeFaultType = FaultType.NORMAL
    private var activeFaultPhase = "A"

    init {
        startSimulationLoop()
    }

    private fun startSimulationLoop() {
        simulationJob?.cancel()
        simulationJob = viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                if (_isSimulating.value) {
                    val currentSpeed = _speedMultiplier.value
                    val delayMs = (200.0 / currentSpeed).toLong().coerceAtLeast(50L)

                    // Increment overload counter if overload is running
                    if (activeFaultType == FaultType.OVERLOAD) {
                        overloadSecondsElapsed += 0.2 * currentSpeed
                    } else {
                        overloadSecondsElapsed = 0.0
                    }

                    // Generate waves and get telemetry packet
                    val window = simulator.generateWaveformWindow(
                        faultType = activeFaultType,
                        affectedPhase = activeFaultPhase,
                        overloadSecondsElapsed = overloadSecondsElapsed
                    )

                    // Classify the current telemetry packet
                    val event = classifier.classify(window.packet)

                    // Append event log if there's a real fault detected
                    if (event.faultType != FaultType.NORMAL) {
                        val currentList = _faultEvents.value.toMutableList()
                        // Ensure we do not record duplicate back-to-back incidents for same phase of same fault unless new
                        val lastEvent = currentList.firstOrNull()
                        if (lastEvent == null || lastEvent.faultType != event.faultType || lastEvent.affectedPhase != event.affectedPhase || lastEvent.resolved) {
                            currentList.add(0, event) // Insert at top (newest first)
                            // In memory limit of 500 events
                            if (currentList.size > 500) {
                                currentList.removeAt(currentList.lastIndex)
                            }
                            _faultEvents.value = currentList
                        }
                    } else {
                        // If normal, see if we need to resolve past active events
                        val currentList = _faultEvents.value
                        val hasUnresolved = currentList.any { !it.resolved }
                        if (hasUnresolved) {
                            _faultEvents.value = currentList.map {
                                if (!it.resolved) it.copy(resolved = true) else it
                            }
                        }
                    }

                    // Update rolling packet and waveforms
                    _currentPacket.value = window.packet
                    _currentWaveforms.value = window.points

                    // Track packet statistics
                    totalPacketsGenerated++
                    if (event.faultType == FaultType.NORMAL) {
                        healthyPacketsGenerated++
                    }

                    // Update System Health Score history (last 100 packets)
                    val score = when (event.faultType) {
                        FaultType.NORMAL -> {
                            val thdPenalty = (window.packet.thd * 1.5).coerceAtMost(10.0)
                            val pfPenalty = ((1.0 - window.packet.powerFactor) * 50.0).coerceAtMost(10.0)
                            (100 - thdPenalty - pfPenalty).toInt().coerceIn(90, 100)
                        }
                        FaultType.SHORT_CIRCUIT -> 12
                        FaultType.OVERLOAD -> {
                            val progress = (overloadSecondsElapsed / 5.0).coerceAtMost(1.0)
                            (85 - (55 * progress)).toInt()
                        }
                        FaultType.PHASE_IMBALANCE -> 54
                    }

                    val updatedHealthHistory = _systemHealthHistory.value.toMutableList()
                    updatedHealthHistory.add(score)
                    if (updatedHealthHistory.size > 100) {
                        updatedHealthHistory.removeAt(0)
                    }
                    _systemHealthHistory.value = updatedHealthHistory

                    delay(delayMs)
                } else {
                    delay(200L)
                }
            }
        }
    }

    fun toggleSimulation(enable: Boolean) {
        _isSimulating.value = enable
    }

    fun setSpeedMultiplier(multiplier: Double) {
        _speedMultiplier.value = multiplier
    }

    fun setInjectionSelection(type: FaultType) {
        _injectionSelection.value = type
    }

    fun setInjectedPhase(phase: String) {
        _injectedPhase.value = phase
    }

    fun injectFault() {
        var faultToInject = _injectionSelection.value
        if (faultToInject == FaultType.NORMAL) {
            // "Random" selection choice translates FaultType.NORMAL to an active random fault
            val options = listOf(FaultType.SHORT_CIRCUIT, FaultType.OVERLOAD, FaultType.PHASE_IMBALANCE)
            faultToInject = options.random()
        }

        activeFaultType = faultToInject
        activeFaultPhase = listOf("A", "B", "C").random() // If SHORT_CIRCUIT or IMBALANCE, choose a phase

        // Auto-resolve shortcut: If user injects, it stays active for 4.5 seconds and then returns to Normal
        viewModelScope.launch {
            delay(4500)
            clearActiveFault()
        }
    }

    fun clearActiveFault() {
        activeFaultType = FaultType.NORMAL
        overloadSecondsElapsed = 0.0
    }

    fun getUptimePercentage(): Double {
        if (totalPacketsGenerated == 0) return 100.0
        return (healthyPacketsGenerated.toDouble() / totalPacketsGenerated.toDouble()) * 100.0
    }

    fun getUptimeSeconds(): Long {
        return (System.currentTimeMillis() - startTimeMillis) / 1000
    }

    fun exportHistoryToCsv(context: Context) {
        val events = _faultEvents.value
        val csvBuilder = java.lang.StringBuilder()
        csvBuilder.append("ID,Timestamp,Fault Type,Affected Phase,Confidence Score,Peak Current (A),Peak Voltage (V),Action,Resolved\n")

        for (e in events) {
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(e.timestamp)
            csvBuilder.append("\"${e.id}\",")
            csvBuilder.append("\"$dateStr\",")
            csvBuilder.append("\"${e.faultType}\",")
            csvBuilder.append("\"${e.affectedPhase}\",")
            csvBuilder.append("${e.confidenceScore},")
            csvBuilder.append("${String.format("%.2f", e.peakCurrent)},")
            csvBuilder.append("${String.format("%.2f", e.peakVoltage)},")
            csvBuilder.append("\"${e.recommendedAction}\",")
            csvBuilder.append("${e.resolved}\n")
        }

        val csvString = csvBuilder.toString()

        try {
            // Write to local temporary file
            val filename = "GridGuard_Fault_Log_${System.currentTimeMillis()}.csv"
            val cacheFile = File(context.cacheDir, filename)
            cacheFile.writeText(csvString)

            // Trigger sharing
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "GridGuard Fault Events Export Log")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Fault History CSV"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCleared() {
        super.onCleared()
        simulationJob?.cancel()
    }
}
