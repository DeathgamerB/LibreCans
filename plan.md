1. **Understand the Goal**: The objective is to add a small UX improvement related to accessibility or usability.
2. **Identify the Issue**: In `app/src/main/kotlin/org/librecans/app/PairingScreen.kt`, the `DeviceItemRow` uses `Modifier.clickable` on a `Card` to allow users to select a Bluetooth device to connect to. However, it lacks proper accessibility semantics for screen readers (like `onClickLabel` and `role = Role.Button`).
3. **Proposed Fix**: Add `onClickLabel = "Connect to $deviceName"` and `role = Role.Button` to the `Modifier.clickable` in `DeviceItemRow` in `PairingScreen.kt`.
4. **Verification**: Compile the app (`./gradlew :app:assembleDebug`) to ensure no syntax errors were introduced.
5. **Commit and Submit**: Create a PR with a descriptive title and description.
