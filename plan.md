1. **Manual device selection**: Tapping a device should immediately change the state so the UI reflects "connecting" without waiting for the FGS to start or the `BluetoothRepository` to update.
    - In `PairingScreen.kt`, tapping a row calls `viewModel.connectDevice()`.
    - In `HeadphonesViewModel.kt`, `connectDevice` already sets `_connectionState.value = BluetoothDeviceState(connecting = true, ...)`. However, in `HeadphonesViewModel.kt`, `BluetoothRepository.connect` also sets `connecting = true`, but there's a collector that might overwrite the immediate local state if the service bind hasn't completed or if a stale state is emitted. More importantly, in `PairingScreen.kt`, the `onClick` handler of the row checks `!state.connecting`. It should show a loading indicator. I will ensure `PairingScreen.kt` reacts to `state.connecting` appropriately or `viewModel` stops the collector from emitting a stale state. Wait! In `PairingScreen.kt`: `if (state.connecting) { ... } else { LazyColumn ... }`. If `state.connecting` becomes true, the list is hidden and a loading spinner shows. Why is it flickering or requiring multiple taps? If `autoConnectIfPaired` runs, or if `serviceConnection` binds and *immediately* sends the current (stale) `BluetoothDeviceState(connected=false, connecting=false)` from the `BluetoothRepository`, it overwrites the `connecting=true` set locally in `HeadphonesViewModel`.
    - Fix: In `BluetoothRepository.connect`, ensure it sets `connecting = true` immediately, and in `HeadphonesViewModel.connectDevice`, ensure the local state isn't clobbered by an immediate emit of a stale state from the newly bound service.

2. **Focus on Voice / Transparency toggle**:
    - The bug: Toggling Voice resets Ambient level to 20 instead of enforcing a minimum of 3.
    - If I look at `AncAmbientControl.kt`: `val targetLevel = if (checked && state.asmLevel < 3) 3 else state.asmLevel`.
    - Wait, does `state.asmLevel` change unexpectedly? Let's check where `state.asmLevel` is read or assigned.
    - If I look at `BluetoothRepository.setAmbientMode`, it writes `KEY_ASM_LEVEL` if `level > 0`.
    - Where does it get reset to 20? When tapping "Ambient" button in `AncAmbientControl`: `val targetLevel = if (state.asmLevel > 0) state.asmLevel else 20`. Wait, what if the user is in "NC" mode (where `asmLevel == 0`), and then taps "Ambient"? It becomes 20 because `state.asmLevel` is 0. Wait, that's not what the user said. They said: "Focus on Voice / Transparency toggle resets the Ambient Sound level to 20".
    - What if I look at `MainDashboard.kt`? Is there another place? No.
    - Let's read the user request again: "Focus on Voice / Transparency toggle resets the Ambient Sound level to 20 (max) every time it's toggled, instead of just enforcing a MINIMUM of 3." Oh! The toggle's on-change handler might be calling `setAmbientMode` which writes the new value, but maybe `BluetoothRepository` is sending 20? Wait. `AncAmbientControl.kt`:
    `onSetAmbientMode(true, targetLevel, checked, true)`
    Is there a bug here? `val targetLevel = if (checked && state.asmLevel < 3) 3 else state.asmLevel`. But wait, in `AncAmbientControl.kt`:
    `Switch(checked = state.focusOnVoice, onCheckedChange = { checked -> ... })`
    What if `state.asmLevel` is 0? If we are in Ambient mode, `state.asmLevel` should be > 0. If it's a toggle, when we turn it off (`checked = false`), `if (checked && state.asmLevel < 3)` is false, so it uses `state.asmLevel`.
    Wait, look at `AncAmbientControl.kt` lines 71-73:
    `val targetLevel = if (state.asmLevel > 0) state.asmLevel else 20`
    This is for the "Ambient" segmented button.
    Wait... let's grep for `20` in `AncAmbientControl.kt`.
    ```
    72: val targetLevel = if (state.asmLevel > 0) state.asmLevel else 20
    ```
    Is there any other place?
    Ah, what about `BluetoothRepository.kt`?
    Let's check `BluetoothRepository.kt`:
    ```
    34: val storedLevel = prefs.getInt(KEY_ASM_LEVEL, 20)
    119: val storedLevel = prefs.getInt(KEY_ASM_LEVEL, 20)
    ```
    What if I check `SettingsControl.kt`? No.
    What about the parser for incoming state? In `BluetoothRepository.kt` `parseStateUpdate`?
