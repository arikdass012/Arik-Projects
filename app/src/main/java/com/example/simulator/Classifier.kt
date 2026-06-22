package com.example.simulator

import com.example.model.FaultEvent
import com.example.model.FaultType
import com.example.model.SignalPacket
import java.util.UUID

class Classifier {

    private val nominalCurrentRms = 10.0 // 10A nominal RMS
    private val nominalVoltageRms = 120.0 // 120V nominal RMS

    /**
     * Reads a signal packet and classifies the state of the power grid,
     * computing confidence scores and generating appropriate fault events.
     */
    fun classify(packet: SignalPacket): FaultEvent {
        val curA = packet.phases.A.current
        val curB = packet.phases.B.current
        val curC = packet.phases.C.current

        val volA = packet.phases.A.voltage
        val volB = packet.phases.B.voltage
        val volC = packet.phases.C.voltage

        val currents = listOf(curA, curB, curC)
        val voltages = listOf(volA, volB, volC)

        val maxCurrent = currents.maxOrNull() ?: 0.0
        val minCurrent = currents.minOrNull() ?: 0.0
        val avgCurrent = currents.average()

        val maxVoltage = voltages.maxOrNull() ?: 0.0
        val minVoltage = voltages.minOrNull() ?: 120.0
        val avgVoltage = voltages.average()

        // Calculate phase standard deviations / imbalance ratios
        val currentImbalanceRatio = if (avgCurrent > 1.0) {
            (maxCurrent - minCurrent) / avgCurrent
        } else {
            0.0
        }

        val voltageImbalanceRatio = if (avgVoltage > 1.0) {
            (maxVoltage - minVoltage) / avgVoltage
        } else {
            0.0
        }

        // 1. Check for SHORT CIRCUIT: Extreme current on any phase (>25A RMS) + voltage sag
        val isShortCircuit = currents.any { it > 25.0 } && voltages.any { it < 60.0 }

        // 2. Check for OVERLOAD: All or average current is elevated (>15A RMS), but no extreme short-circuit spike
        val isOverload = avgCurrent > 14.5 && !isShortCircuit

        // 3. Check for PHASE IMBALANCE: Deviation between phases is prominent (>20%) while average values are not extreme
        val isImbalance = (currentImbalanceRatio > 0.25 || voltageImbalanceRatio > 0.20) && !isShortCircuit && !isOverload

        val faultType: FaultType
        val affectedPhase: String
        val confidenceScore: Double
        val action: String

        when {
            isShortCircuit -> {
                faultType = FaultType.SHORT_CIRCUIT
                // Determine affected phase (one with current > 25A)
                affectedPhase = when {
                    curA > 25.0 && curB <= 25.0 && curC <= 25.0 -> "A"
                    curB > 25.0 && curA <= 25.0 && curC <= 25.0 -> "B"
                    curC > 25.0 && curA <= 25.0 && curB <= 25.0 -> "C"
                    else -> "ALL"
                }
                // Confidence weighted calculation
                // Weight current spike magnitude
                val currentWeight = ((maxCurrent / 40.0) * 50.0).coerceAtMost(50.0)
                // Weight voltage sag depth
                val sagDepth = (nominalVoltageRms - minVoltage) / nominalVoltageRms // e.g. (120 - 30) / 120 = 0.75
                val voltageWeight = (sagDepth * 30.0).coerceAtMost(30.0)
                // Weight THD spike
                val thdWeight = ((packet.thd / 25.0) * 20.0).coerceAtMost(20.0)

                confidenceScore = (currentWeight + voltageWeight + thdWeight).coerceIn(70.0, 100.0)
                action = "TRIP CIRCUIT BREAKER IMMEDIATELY. Isolate Phase $affectedPhase, inspect for short circuit / ground fault path."
            }
            isOverload -> {
                faultType = FaultType.OVERLOAD
                affectedPhase = "ALL"
                // Confidence weighted calculation
                // Weight current magnitude
                val currentWeight = (((avgCurrent - 10.0) / 10.0) * 60.0).coerceAtMost(60.0)
                // Weight THD and PF sag
                val pfSag = (1.0 - packet.powerFactor) * 40.0
                confidenceScore = (currentWeight + pfSag).coerceIn(60.0, 100.0)
                action = "SHED LOAD PROACTIVELY. Alert operators to shift redundant loads or trigger cooling fans to prevent transformer overheating."
            }
            isImbalance -> {
                faultType = FaultType.PHASE_IMBALANCE
                // Affected phase is the one that sags / deviates most from mean
                val devA = Math.abs(curA - avgCurrent)
                val devB = Math.abs(curB - avgCurrent)
                val devC = Math.abs(curC - avgCurrent)
                affectedPhase = when {
                    devA > devB && devA > devC -> "A"
                    devB > devA && devB > devC -> "B"
                    else -> "C"
                }
                // Confidence weighted calculation
                val imbalanceWeight = (currentImbalanceRatio * 70.0).coerceAtMost(70.0)
                val voltImbalanceWeight = (voltageImbalanceRatio * 30.0).coerceAtMost(30.0)
                confidenceScore = (imbalanceWeight + voltImbalanceWeight).coerceIn(55.0, 100.0)
                action = "REBALANCE PHASE LOADS. Inspect secondary distributions on Phase $affectedPhase to resolve neutral current escalation."
            }
            else -> {
                faultType = FaultType.NORMAL
                affectedPhase = "NONE"
                confidenceScore = (100.0 - (packet.thd * 2.0) - (Math.abs(0.95 - packet.powerFactor) * 100.0)).coerceIn(85.0, 100.0)
                action = "SYSTEM HEALTHY. Waveform within nominal parameters."
            }
        }

        return FaultEvent(
            id = UUID.randomUUID().toString(),
            timestamp = packet.timestamp,
            faultType = faultType,
            affectedPhase = affectedPhase,
            confidenceScore = Math.round(confidenceScore * 10.0) / 10.0,
            peakCurrent = maxCurrent,
            peakVoltage = maxVoltage,
            recommendedAction = action,
            resolved = faultType == FaultType.NORMAL
        )
    }
}
