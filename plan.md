# Plan to Fix Optimistic UI Reverting

1. **Analyze `BluetoothRepository.kt`:**
   - Observe that `setAmbientMode`, `setEqualizerPreset`, `setEqualizerCustom`, etc. use `updateStateOptimistically`.
   - Observe that `parseStateUpdate` blindly overwrites `_deviceState` without checking if an optimistic update just happened.

2. **Add a Timestamp Tracking Mechanism:**
   - Introduce a variable like `private var lastOptimisticUpdateTime: Long = 0` in `BluetoothRepository.kt`.
   - Alternatively, add specific timestamps for categories (e.g. `lastAmbientUpdateTime`, `lastEqUpdateTime`).

3. **Update `updateStateOptimistically`:**
   - Whenever an optimistic update is applied, set the `lastOptimisticUpdateTime` to `System.currentTimeMillis()`.

4. **Guard `parseStateUpdate`:**
   - Inside `parseStateUpdate` (or specific cases inside it like `0x67.toByte()` for NCASM or `V2Command.EQ_RET`), check if `System.currentTimeMillis() - lastOptimisticUpdateTime` is less than a threshold (e.g., `1000L` ms).
   - If it is within the window, simply return and ignore the incoming status update because it is likely a stale read or poll response.
   - We might need specific timestamps for different features (ambient vs EQ) to avoid one blocking the other, or a general one might be sufficient since they are user-driven.

5. **Verify and Test:**
   - Check the code compilation.
   - We will run the protocol tests or unit tests if any exist.

6. **Pre-commit Steps:**
   - Run pre-commit instructions to ensure everything is solid.

7. **Submit Change:**
   - Push branch and complete task.
