1. **Manual device selection flash/delay**: Modify `PairingScreen.kt`. I'll introduce a local state `var localConnecting by remember { mutableStateOf(false) }` to immediately show the loading indicator when the row is tapped. Wait, better yet, `PairingScreen` can rely on `state.connecting`, but `HeadphonesViewModel.kt` sets `_connectionState.value` to `connecting = true`, and then calls `startService`. I'll make sure in `BluetoothRepository.kt` the `_deviceState.value` is updated synchronously to `connecting = true` before launching the coroutine, so the service collector immediately gets `connecting=true`.

2. **Focus on Voice / Transparency toggle**:
Ah! I found the real issue. The user is referring to the fact that when `asmLevel` < 3, the `Switch` is entirely hidden inside an `AnimatedVisibility`. This means the user CANNOT toggle it when `asmLevel < 3`.
Wait, look at what the user wrote:
"Verify by: setting level to something like 10, toggling Voice on, confirming the level stays at 10 (not jumping to 20), then setting level to 1, toggling Voice on, confirming it becomes 3."
This means the user *expects* to be able to toggle it when level is 1!
So I need to move the `Switch` OUTSIDE the `AnimatedVisibility` block that restricts it to `>= 3`.
And I need to remove the "Focus on Voice is unavailable below level 3" text, or show it differently. Actually, if they toggle it while it's 1, it should automatically jump the level to 3. So the toggle should always be accessible.

Wait, is there a bug where toggling it sets it to 20? Let's trace `onSetAmbientMode(true, targetLevel, checked, true)`.
If `asmLevel` is 10, and we toggle it ON:
`val targetLevel = if (checked && state.asmLevel < 3) 3 else state.asmLevel` -> `targetLevel` = 10.
If `targetLevel` is 10, then it calls `onSetAmbientMode(true, 10, true, true)`.
In `BluetoothRepository.setAmbientMode`, it does `val usedLevel = if (level > 0) level else 1`. It uses 10.
Wait... what if the slider `onValueChange` resets it? No.
Maybe they were complaining about the behavior in the OLD PR that I am now replacing.

3. **Hardware Info panel position**:
- In `MainDashboard.kt`, move `DeviceHeroCard` below `SettingsControl`. Wait, the user said "Hardware Information panel is in the wrong position (should be LAST, after Features and Configuration... and is not collapsed by default".
The "Hardware Information" panel is currently inside `SettingsControl.kt`, lines 40-65. It's the first card in `SettingsControl.kt`.
I will make it collapsible by default, and put it at the bottom of `SettingsControl.kt` (which represents Configuration).

4. **Fix clipping/cutoff**:
- In `MainDashboard.kt`, `Scaffold( ... ) { innerPadding -> Column( padding(innerPadding).safeDrawingPadding() ) }`. The `padding(innerPadding)` combined with `safeDrawingPadding` is wrong. `innerPadding` already includes system bars. I should just use `padding(innerPadding)` on the `Column`. Also, I need to check `AboutScreen` and `DebugLogScreen`.

I will set this plan and begin execution.
