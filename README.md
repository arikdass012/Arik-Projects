# GridGuard: Smart Power Grid Fault Detection Simulator & Dashboard

GridGuard is a high-fidelity industrial SCADA (Supervisory Control and Data Acquisition) monitoring application and signal simulation engine built natively for Android using Jetpack Compose, Kotlin, and modern Material 3 design principles.

The application mimics an embedded 3-phase real-time power grid monitoring node. All signal processing, waveform aggregation, Fast Fourier analysis, rule classification, and confidence scoring run dynamically in Kotlin as a background reactive telemetry model.

---

## Technical Stack & Architecture

- **Platform:** Native Android (Kotlin, Jetpack Compose, Material 3)
- **Model-View-ViewModel (MVVM):** Reactive UI states driven by structured Kotlin Coroutines flows & `SharedFlow`/`StateFlow`.
- **Dynamic Waveform Generation:** Core mathematical oscillators synthesizing real-time 3-phase AC voltage and current sines at 50 Hz, sampling at 1000 Hz.
- **Embedded DFT (Discrete Fourier Transform):** Custom-coded Fourier extraction processing sliding 100ms sample buffers to extract EXACT harmonic components (50Hz fundamental, 150Hz 3rd, 250Hz 5th, and 350Hz 7th harmonics).
- **SCADA Hardware UI:** Custom graphics canvas drawing vector CRT-style oscilloscopes, frequency spectrum graphs, historic health trends, and sweeping 270° concentric circular gauges.
- **CSV Log Export:** Integrated with Android `<provider>` FileProvider, allowing users to securely share or download chronological CSV logs of grid anomalies.

---

## How It Works: Signal Processing Pipeline

The GridGuard telemetry loop is built as a sequential signal-processing pipeline executing every 200ms (multiplied by the simulation speed slider):

```
[ Signal Generator Engine ]
            │ (Generates 100 points of Va, Vb, Vc, Ia, Ib, Ic at 50Hz)
            ▼
[ Fault Injector Layer ]
            │ (Overlays clipping sags, Overload ramps, or Phase Imbalances)
            ▼
[ Direct Fourier Analyzer ] 
            │ (Computes Euler cos/sin terms for 50Hz, 150Hz, 250Hz, 350Hz)
            ├───────────────► [ Calculate THD % ] 
            ▼                                
[ Rule-Based & ML Classifier ]
            │ (Classifies grid status using weighted confidence score metrics)
            ▼
[ Broadcast Flow ] ────► [ Live Waveform Scope ] ────► [ Health Gauges ] ────► [ Alert Log ]
```

### 1. The Signal Simulator (Continuous Oscillators)
Three phase AC voltages and currents are generated with standard $120^\circ$ ($2\pi/3$ rad) offsets:
- $\phi_A = 0$
- $\phi_B = -2\pi/3$
- $\phi_C = +2\pi/3$

Nominal voltages are kept at 120 V RMS (170 V Peak) and currents at 10 A RMS (14.14 A Peak).
When faults are injected:
1. **Short Circuit:** The affected Phase voltage sags down to 25%, and current spikes to 420%, with soft flat-clipping at 330% to simulate vacuum/transformer core saturation (which naturally generates odd harmonics).
2. **Overload:** All phases undergo a gradual logarithmic current ramp up to 180% current, and voltage sinks slightly by 15%.
3. **Phase Imbalance:** One selected phase suffers a 35% voltage drop and 70% current drop (creating an asymmetric high-imbalance configuration across the grid).

### 2. The Discrete Fourier (DFT) & THD Extraction
Rather than running a full Radix-2 FFT which requires $2^n$ buffer limits, GridGuard utilizes direct Discrete Fourier bin calculations. For our specific target frequencies $f \in \{50, 150, 250, 350\}$:
$$C(f) = \sum_{n=0}^{N-1} x[n] \cos(2\pi f n \cdot dt)$$
$$S(f) = \sum_{n=0}^{N-1} x[n] \sin(2\pi f n \cdot dt)$$
$$\text{Magnitude}(f) = \frac{2}{N}\sqrt{C(f)^2 + S(f)^2}$$

Total Harmonic Distortion (% THD) is then extracted using fundamental harmonic ($A_1$) alongside odd upper harmonics:
$$\text{THD}\% = \frac{\sqrt{A_3^2 + A_5^2 + A_7^2}}{A_1} \times 100\%$$

### 3. Rule Classification & Confidence Matrix
The `Classifier` reads the consolidated telemetry packet and identifies grid anomalies:
- **SHORT_CIRCUIT:** Sparked when any phase current $> 25$ A and voltage $< 60$ V. Confidence score is proportional to current spike weight (50%), voltage sag severity (30%), and harmonic distortion growth (20%).
- **OVERLOAD:** Triggered if mean grid current $> 14.5$ A. Confidence tracks current escalation and power factor decay.
- **PHASE_IMBALANCE:** Fired if the current or voltage disparity between phases deviates by more than 20-25%.
- **NORMAL:** Active otherwise.

---

## Developer Operations

### Build & Run
Ensure Android Studio is configured, or run matching tasks via Gradle:

```bash
# Verify build & resources
gradle assembleDebug

# Compile and check tests
gradle test
```

### CSV Provider
The `<provider>` is configured in `AndroidManifest.xml` under authorities `${applicationId}.fileprovider`. Temporary CSV outputs are generated in the application's secure cache directory (`/cache/export_cache`) and shared with standard platform share-action intents.
