1. **Analyze TWS scaling requirements**
   - User states: "reapply the same 50% shrinking factor to the tws... shrink the default hero size by about 5% and remove a bit of the space below the image... fix too sensitive scrolling after redoing the TWS shrinking logic and make it also feel smoother again. After resizing the bero image we had similar Problems, but after you(another instance) did the troubleshooting steps and fixed the smoothness and scroll speed everything was ok."
   - Let's clarify the changes needed for TWS:
     - Start size should be shrunk by 5% (i.e. start at 0.95f).
     - It should shrink by 50% from that starting size (down to 0.475f).
     - Remove "a bit of the space below the image" to match headphone positioning. The TWS images don't reach as far down. So we'll subtract some vertical height from the layout.
     - "Fix too sensitive scrolling... make it feel smoother again." To make it smoother, maybe we need to adjust the scroll divisor to `800 * density` (slower/smoother shrink) rather than `400 * density` (which shrinks rapidly and makes the scroll feel too sensitive/jumpy as the layout height collapses quickly). Wait, they said "after you did the troubleshooting steps and fixed the smoothness... everything was ok". For the headphones I used `400 * density` and they said "The Headphones are all good now". I'll use `400 * density` or maybe `600 * density` for TWS, or whatever I used for headphones if they are comparing to that. Let's stick with `400 * density` as the shrinkage rate since it worked well for headphones, or if they mean the scrolling is jumping because the TWS layout is shrinking too fast, let's use a slightly slower rate like `600 * density` if it feels too sensitive. "After resizing the bero image we had similar Problems, but after you... fixed the smoothness and scroll speed everything was ok." - this indicates there was a previous fix for smoothness and scroll speed.
     - Wait, in memory I see: "In Jetpack Compose, to dynamically shrink a component... apply the dynamic size calculation directly within the layout phase using Modifier.layout rather than state animations... This prevents scroll feedback loops (layout thrashing/wiggling)". I am ALREADY using `Modifier.layout` in `SettingsControl.kt`, which is the fix. The current code uses `Modifier.layout`. So the "troubleshooting steps" are already applied by using `Modifier.layout`! I just need to make sure I *keep* using `Modifier.layout` and properly update the math for TWS.

2. **Update the code**
   - File: `app/src/main/kotlin/org/librecans/app/SettingsControl.kt`
   - In `DeviceHeroCard` modifier layout block:
     ```kotlin
     if (isTWS) {
         targetScale = (0.95f - (scrollValue / (400 * density))).coerceIn(0.475f, 0.95f)
         topPadding = 0
         // Calculate crop for the bottom. Let's crop 15% of the height from the bottom so the elements below sit higher up.
         // We do this by reducing the overall returned layout height.
     }
     ```
   - Let's determine the exact `bottomCrop` for TWS. "remove a bit of the space below the image so they are at a similar positions as the headphone hero image( keep in mind that the TWS are smaller in general so they dont reach as far down on the picture, as the Headphones)".
   - TWS doesn't reach the bottom. If we want elements below to be closer, we reduce the height returned by `layout(...)`.
   - Let's use `val bottomCrop = (placeable.height * 0.15f).toInt()` for TWS, and for headphones it's 0.
   - So for TWS, we will return `layout(placeable.width, placeable.height - bottomCrop) { placeable.placeRelative(0, 0) }`.
   - Wait, `topPadding` is used for headphones to push it down and leave space at the top. For TWS, we will set `topPadding = 0`, but we will have a `bottomCrop` that reduces the layout height.
   - Wait, if `bottomCrop` is dynamically calculated based on `placeable.height` which changes, it might look jumpy. Let's calculate it based on `constraints.maxWidth` which is static. `val bottomCrop = (constraints.maxWidth * 0.15f).toInt()`. This will consistently remove 15% of the max width from the bottom space.
   - Wait, if we crop 15% of `maxWidth`, when the image shrinks, it might crop *into* the TWS image. The TWS starts at 95% width, shrinks to 47.5%. The space removed by shrinking is 47.5% of max width. If we permanently crop 15% of `maxWidth`, it might be fine, since the TWS has whitespace at the bottom.
   - Wait, maybe I should just use a constant value, or scale it with the `targetScale`?
   - Let's use `val bottomCrop = ((0.95f - targetScale) * constraints.maxWidth * 0.15f).toInt()`. No, this only crops *during* scroll. The user said: "shrink the default hero size by about 5% and remove a bit of the space below the image so they are at a similar positions...". This implies the *default* layout needs the space removed, even before scrolling.
   - So `val bottomCrop = (constraints.maxWidth * 0.10f).toInt()`. Let's use 10% of max width.
   - Layout for TWS: `layout(placeable.width, placeable.height - bottomCrop) { placeable.placeRelative(0, 0) }`

3. **Refine the Plan**
   - Update `targetScale` for TWS to `(0.95f - (scrollValue / (400 * density))).coerceIn(0.475f, 0.95f)`.
   - Update TWS layout logic to remove some space from the bottom (`bottomCrop = (constraints.maxWidth * 0.10f).toInt()`).
   - Use `layout(placeable.width, placeable.height + topPadding - bottomCrop)`.
     - For TWS: `topPadding = 0`, `bottomCrop = (constraints.maxWidth * 0.10f).toInt()`.
     - For Headphones: `topPadding = ((0.8f - targetScale) * constraints.maxWidth * 0.15f).toInt()`, `bottomCrop = 0`.
   - Build and test.
