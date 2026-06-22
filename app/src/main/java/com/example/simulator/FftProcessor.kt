package com.example.simulator

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class FftProcessor {

    /**
     * Computes the magnitude of specific frequency components in a given waveform.
     * Uses a direct Discrete Fourier Transform (DFT) for target frequency bins,
     * which is highly efficient and exact for arbitrary frequencies.
     *
     * @param samples The waveform samples.
     * @param sampleRateHz The sampling rate of the waveform in Hz.
     * @param targetFreqs The specific frequencies to analyze (e.g., [50, 150, 250, 350]).
     * @return List of magnitudes corresponding to the target frequencies.
     */
    fun computeMagnitudes(
        samples: List<Double>,
        sampleRateHz: Double,
        targetFreqs: List<Int>
    ): List<Double> {
        if (samples.isEmpty()) return List(targetFreqs.size) { 0.0 }

        val n = samples.size
        val dt = 1.0 / sampleRateHz

        return targetFreqs.map { freq ->
            var sumCos = 0.0
            var sumSin = 0.0

            for (i in 0 until n) {
                val t = i * dt
                val angle = 2.0 * Math.PI * freq * t
                sumCos += samples[i] * cos(angle)
                sumSin += samples[i] * sin(angle)
            }

            // Normalize the amplitude (multiply by 2/N)
            val mag = (2.0 / n) * sqrt(sumCos * sumCos + sumSin * sumSin)
            mag
        }
    }

    /**
     * Computes Total Harmonic Distortion (% THD) based on extracted harmonic magnitudes.
     * THD = sqrt(V3^2 + V5^2 + V7^2) / V1 * 100
     *
     * @param magnitudes Magnitudes where index 0 is fundamental (50Hz), indices 1..3 are harmonics.
     */
    fun computeThd(magnitudes: List<Double>): Double {
        if (magnitudes.size < 2 || magnitudes[0] < 0.01) return 0.0
        val sumHarmonicsSq = magnitudes.drop(1).sumOf { it * it }
        return (sqrt(sumHarmonicsSq) / magnitudes[0]) * 100.0
    }
}
