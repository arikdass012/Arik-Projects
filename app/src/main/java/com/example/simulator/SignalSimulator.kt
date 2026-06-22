package com.example.simulator

import com.example.model.*
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class SignalSimulator {

    private val fftProcessor = FftProcessor()
    private var timeOffset = 0.0

    // Nominal peak values
    private val nominalVoltagePeak = 170.0 // ~120V RMS
    private val nominalCurrentPeak = 14.14 // ~10V RMS

    // Multipliers/Scalers for different active faults
    private var overloadProgress = 0.0 // Goes from 0.0 to 1.0 during overload

    /**
     * Generates a waveform window (sampling rate 1000Hz, duration 100ms: 100 samples).
     * Injecting appropriate distortions based on the active fault type.
     */
    fun generateWaveformWindow(
        faultType: FaultType,
        affectedPhase: String,
        overloadSecondsElapsed: Double,
        noiseLevel: Double = 0.05
    ): WaveformWindow {
        val sampleRate = 1000.0
        val numSamples = 100
        val dt = 1.0 / sampleRate

        val points = mutableListOf<WavePoint>()
        val waveA = mutableListOf<Double>()
        val waveB = mutableListOf<Double>()
        val waveC = mutableListOf<Double>()

        // Update time offset to simulate a continuous scrolling wave
        timeOffset += 0.02 // Scroll by 20ms each packet

        // Determine fault attributes
        val currentOverloadFactor = if (faultType == FaultType.OVERLOAD) {
            // Gradually rise to 1.8x current over 5 seconds
            val progress = (overloadSecondsElapsed / 5.0).coerceAtMost(1.0)
            1.0 + (0.8 * progress)
        } else {
            1.0
        }

        val voltageOverloadFactor = if (faultType == FaultType.OVERLOAD) {
            // Gradually sag to 0.85x voltage
            val progress = (overloadSecondsElapsed / 5.0).coerceAtMost(1.0)
            1.0 - (0.15 * progress)
        } else {
            1.0
        }

        for (i in 0 until numSamples) {
            val t = i * dt + timeOffset
            val angleA = 2.0 * PI * 50.0 * t
            val angleB = angleA - (2.0 * PI / 3.0)
            val angleC = angleA + (2.0 * PI / 3.0)

            // 1. Normal/Base voltage & currents with harmonics
            var vA = nominalVoltagePeak * sin(angleA)
            var vB = nominalVoltagePeak * sin(angleB)
            var vC = nominalVoltagePeak * sin(angleC)

            var iA = nominalCurrentPeak * sin(angleA - PI * 0.15) // ~0.95 PF
            var iB = nominalCurrentPeak * sin(angleB - PI * 0.15)
            var iC = nominalCurrentPeak * sin(angleC - PI * 0.15)

            // Inject 1.5% 3rd, 5th, 7th harmonic background noise
            vA += nominalVoltagePeak * 0.015 * sin(3 * angleA) + nominalVoltagePeak * 0.01 * sin(5 * angleA)
            vB += nominalVoltagePeak * 0.015 * sin(3 * angleB) + nominalVoltagePeak * 0.01 * sin(5 * angleB)
            vC += nominalVoltagePeak * 0.015 * sin(3 * angleC) + nominalVoltagePeak * 0.01 * sin(5 * angleC)

            iA += nominalCurrentPeak * 0.02 * sin(3 * angleA) + nominalCurrentPeak * 0.015 * sin(5 * angleA)
            iB += nominalCurrentPeak * 0.02 * sin(3 * angleB) + nominalCurrentPeak * 0.015 * sin(5 * angleB)
            iC += nominalCurrentPeak * 0.02 * sin(3 * angleC) + nominalCurrentPeak * 0.015 * sin(5 * angleC)

            // Apply fault modifiers
            when (faultType) {
                FaultType.SHORT_CIRCUIT -> {
                    // Affected phase has massive current spike (~350%) and voltage sag (~25%)
                    // Also adding heavy flat clipping for current spike to simulate saturation (creates high THD!)
                    val limitCurrent = nominalCurrentPeak * 3.3
                    when (affectedPhase) {
                        "A" -> {
                            vA *= 0.25
                            val spikedCurrent = nominalCurrentPeak * 4.2 * sin(angleA - PI * 0.4)
                            iA = spikedCurrent.coerceIn(-limitCurrent, limitCurrent)
                        }
                        "B" -> {
                            vB *= 0.25
                            val spikedCurrent = nominalCurrentPeak * 4.2 * sin(angleB - PI * 0.4)
                            iB = spikedCurrent.coerceIn(-limitCurrent, limitCurrent)
                        }
                        "C" -> {
                            vC *= 0.25
                            val spikedCurrent = nominalCurrentPeak * 4.2 * sin(angleC - PI * 0.4)
                            iC = spikedCurrent.coerceIn(-limitCurrent, limitCurrent)
                        }
                        else -> { // ALL
                            vA *= 0.25; vB *= 0.25; vC *= 0.25
                            iA = (nominalCurrentPeak * 4.2 * sin(angleA - PI * 0.4)).coerceIn(-limitCurrent, limitCurrent)
                            iB = (nominalCurrentPeak * 4.2 * sin(angleB - PI * 0.4)).coerceIn(-limitCurrent, limitCurrent)
                            iC = (nominalCurrentPeak * 4.2 * sin(angleC - PI * 0.4)).coerceIn(-limitCurrent, limitCurrent)
                        }
                    }
                }
                FaultType.OVERLOAD -> {
                    // Gradual voltage drop and massive proportional current rise on ALL phases
                    vA *= voltageOverloadFactor
                    vB *= voltageOverloadFactor
                    vC *= voltageOverloadFactor

                    iA *= currentOverloadFactor
                    iB *= currentOverloadFactor
                    iC *= currentOverloadFactor
                }
                FaultType.PHASE_IMBALANCE -> {
                    // One phase has heavy deviation: say, phase B voltage sags by 35% and phase B current drops to 30%
                    when (affectedPhase) {
                        "A" -> {
                            vA *= 0.65
                            iA *= 0.30
                        }
                        "B" -> {
                            vB *= 0.65
                            iB *= 0.30
                        }
                        "C" -> {
                            vC *= 0.65
                            iC *= 0.30
                        }
                    }
                }
                FaultType.NORMAL -> {
                    // No major faults, standard small fluctuations
                }
            }

            // Apply fine random noises to simulate physical lines
            val noiseV = Random.nextDouble(-noiseLevel, noiseLevel) * nominalVoltagePeak
            val noiseI = Random.nextDouble(-noiseLevel, noiseLevel) * nominalCurrentPeak

            vA += noiseV; vB += noiseV; vC += noiseV
            iA += noiseI; iB += noiseI; iC += noiseI

            points.add(WavePoint(t * 1000.0, vA, vB, vC, iA, iB, iC))
            waveA.add(iA) // Focus FFT analysis on Phase A current (or worst-affected phase current)
            waveB.add(iB)
            waveC.add(iC)
        }

        // Run Fourier analysis
        // Let's analyze Phase A current if NORMAL or OVERLOAD. If SHORT_CIRCUIT/IMBALANCE, analyze the worst affected phase current
        val activeWave = when (faultType) {
            FaultType.SHORT_CIRCUIT, FaultType.PHASE_IMBALANCE -> {
                if (affectedPhase == "B") waveB else if (affectedPhase == "C") waveC else waveA
            }
            else -> waveA
        }

        val targetFreqs = listOf(50, 150, 250, 350)
        val magnitudes = fftProcessor.computeMagnitudes(activeWave, sampleRate, targetFreqs)
        val thd = fftProcessor.computeThd(magnitudes)

        // Compute RMS values of current and voltage for the packet
        val rmsVA = calculateRms(points.map { it.vA })
        val rmsVB = calculateRms(points.map { it.vB })
        val rmsVC = calculateRms(points.map { it.vC })

        val rmsIA = calculateRms(points.map { it.iA })
        val rmsIB = calculateRms(points.map { it.iB })
        val rmsIC = calculateRms(points.map { it.iC })

        // Power factor simulation
        val basePf = when (faultType) {
            FaultType.NORMAL -> 0.95 + Random.nextDouble(-0.01, 0.01)
            FaultType.SHORT_CIRCUIT -> 0.42 + Random.nextDouble(-0.03, 0.03)
            FaultType.OVERLOAD -> 0.81 + Random.nextDouble(-0.02, 0.02)
            FaultType.PHASE_IMBALANCE -> 0.74 + Random.nextDouble(-0.02, 0.02)
        }.coerceIn(0.2, 1.0)

        val packet = SignalPacket(
            timestamp = System.currentTimeMillis(),
            phases = Phases(
                A = PhaseValue(rmsVA, rmsIA),
                B = PhaseValue(rmsVB, rmsIB),
                C = PhaseValue(rmsVC, rmsIC)
            ),
            fftData = FftData(targetFreqs, magnitudes),
            powerFactor = basePf,
            thd = thd
        )

        return WaveformWindow(points, packet)
    }

    private fun calculateRms(samples: List<Double>): Double {
        if (samples.isEmpty()) return 0.0
        val sumSq = samples.sumOf { it * it }
        return sqrt(sumSq / samples.size)
    }
}

data class WaveformWindow(
    val points: List<WavePoint>,
    val packet: SignalPacket
)
