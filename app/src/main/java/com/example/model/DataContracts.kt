package com.example.model

data class PhaseValue(
    val voltage: Double,
    val current: Double
)

data class Phases(
    val A: PhaseValue,
    val B: PhaseValue,
    val C: PhaseValue
)

data class FftData(
    val frequencies: List<Int>, // [50, 150, 250, 350] Hz
    val magnitudes: List<Double> // Amplitude at each frequency
)

data class SignalPacket(
    val timestamp: Long,
    val phases: Phases,
    val fftData: FftData,
    val powerFactor: Double,
    val thd: Double              // Total Harmonic Distortion %
)

enum class FaultType {
    NORMAL,
    SHORT_CIRCUIT,
    OVERLOAD,
    PHASE_IMBALANCE
}

data class FaultEvent(
    val id: String,
    val timestamp: Long,
    val faultType: FaultType,
    val affectedPhase: String,  // "A", "B", "C", "ALL"
    val confidenceScore: Double,  // 0–100
    val peakCurrent: Double,
    val peakVoltage: Double,
    val recommendedAction: String,
    val resolved: Boolean
)

data class WaveWavePoint(
    val t: Double,
    val vA: Double, val vB: Double, val vC: Double,
    val iA: Double, val iB: Double, val iC: Double
)

typealias WavePoint = WaveWavePoint

