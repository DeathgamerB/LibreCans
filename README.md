# LibreCans

**LibreCans** is an independent, native Android application written in Kotlin utilizing Jetpack Compose and Material 3 Expressive-inspired design components. It acts as an autonomous companion for Sony headphones, allowing complete control over Noise Cancelling, Ambient Sound levels, Equalizer, DSEE, and capability-gated options without requiring the official smartphone app or online tracking.

This project is built as a permitted derivative of **SonyBridge** and **SonyHeadphonesClient**, combining reliable RFCOMM SPP protocol implementations with a modern, lightweight Android background service.

---

## Features
- **Ambient Sound Control**: Noise Cancelling, Ambient Sound (0–20 levels), and Off modes.
- **Focus on Voice**: Capability-gated passthrough toggle.
- **Equalizer**: Presets + full manual mode (5 bands + Clear Bass) with reactive dynamic labels.
- **DSEE Upscaling**: Dynamic high-frequency audio upscaling toggle.
- **Battery Levels**: Per-earbud and case indicators for TWS models, or master single level + charging state for over-ear/traditional models.
- **Hardware Readouts**: Instant lookup of active audio codec, firmware version, and active protocol generation.
- **Capability-Gated Extras**: Auto Power-Off, Speak-to-Chat, and Adaptive Volume are dynamically queried and rendered *only* if the connected device physically supports them.
- **Expressive Styling**: Custom premium dark theme with spring-based motion physics and responsive curved card frames.
- **Foreground Connection Service**: A persistent background service and ongoing notification hold the Bluetooth RFCOMM socket open reliably, preventing connection drops during Doze or system-initiated standby.
- **Emulator Simulation (Mock Mode)**: An isolated debug toggle enables simulated testing of all WH-CH720N features in an emulator or without physical headphones.

---

## Known Limitations & Testing
- **Hardware Verification**: Only the **WH-CH720N and Wf-1000xm5** have been hardware-verified by the upstream projects and me; other Sony models (e.g. WH-1000XM3, WH-1000XM4, WH-1000XM5, LinkBuds S, WF-C700N, etc.) are highly expected to work based on shared protocol families.

## Architecture
The project is structured into two modules:
1. **`:protocol`** (Pure Kotlin module with no Android dependencies):
   - Handles checksum calculations (`_sumChecksum`), packet escaping/unescaping, and framing (`packageDataForBt` / `unpackBtMessage`).
   - Translates all protocol commands (v1 & v2 generations) into unit-testable builders.
2. **`:app`** (Android application module):
   - **`BluetoothRepository`**: Exposes flows representing the current status and executes connection fallbacks (cached version, then V1 UUID, then V2 UUID).
   - **`BluetoothConnectionService`**: A thin Android Foreground Service hosting the active repository instance.
   - **`HeadphonesViewModel`**: Coordinates screen navigation states, active device list loader, and action dispatches.
   - **UI Layers**: `PairingScreen.kt`, `AncAmbientControl.kt`, `EqualizerControl.kt`, `SettingsControl.kt`, and `AboutScreen.kt` written using modern Compose elements.

---

## Build and Run

### Running Unit Tests
To execute all protocol, checksum, and serialization unit tests:
```bash
./gradlew test
```

### Compiling Debug APK
To assemble the debug build:
```bash
./gradlew :app:assembleDebug
```
The compiled APK will be available under `app/build/outputs/apk/debug/app-debug.apk`.

---

## License & Credits
LibreCans is released under the **MIT License**. See the `LICENSE` file for the full text.

Special thanks to the original open-source creators of the upstream projects:
- **Plutoberth**, **Mr-M33533K5**, and **semvis123** (SonyHeadphonesClient)
- **AmitRajput-Dev** (SonyBridge)
- **GadgetBridge Contributors**

---

*Disclaimer: LibreCans is an independent, unofficial, open-source project and is NOT affiliated with, authorized, maintained, sponsored, or endorsed by Sony Corporation or any of its affiliates.*
