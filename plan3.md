1. **Restore original scroll speed factor**
   - The user reported the scrolling feels "faster than my finger". This is caused by the dynamic height reduction inside `Modifier.layout` acting as a multiplier on the scroll speed (since items below move up by the scrolled amount PLUS the shrunk layout amount).
   - In my previous changes, I halved the scroll divisor from `800 * density` to `400 * density`. This caused the layout to shrink twice as fast per pixel of scroll, drastically increasing the multiplier effect.
   - I will revert the factor to `800 * density` for both TWS and headphones in `SettingsControl.kt`.
     - TWS: `targetScale = (0.95f - (scrollValue / (800 * density))).coerceIn(0.475f, 0.95f)`
     - Headphones: `targetScale = (0.8f - (scrollValue / (800 * density))).coerceIn(0.4f, 0.8f)`
2. **Build and Test**
   - Run `./gradlew :app:assembleDebug` and `./gradlew :protocol:test`.
3. **Pre-commit checks**
   - Complete pre-commit steps.
