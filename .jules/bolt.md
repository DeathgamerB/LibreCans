## 2024-08-05 - Caching SharedPreferences in BluetoothRepository
**Learning:** `PrefsHelper.getEncryptedPrefs(context, AMBIENT_PREFS_NAME)` is called repeatedly in `BluetoothRepository`, specifically during `init`, `connect`, and `setAmbientMode`. `getEncryptedPrefs` reads a file from disk and parses XML. We can cache the instance as a class-level `lazy` property rather than repeatedly fetching it. This avoids redundant I/O and context lookup overhead as documented in memory.
**Action:** Replace inline `PrefsHelper.getEncryptedPrefs(...)` calls with a cached `lazy` property.
