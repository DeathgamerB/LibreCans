1. **Examine `SettingsControl.kt`**
   - Locate `DeviceHeroCard` which has the `Image` composable responsible for drawing headphones/earbuds.
   - The current behavior scales both earbud and headphone down by `(1f - (scrollValue / (800 * density))).coerceIn(0.5f, 1f)`.
   - The user requests:
     - **For headphones only** (`!isTWS`):
       - Scaled 20% down at start (0.8f).
       - Shrink by 50% while scrolling (down to 0.4f).
       - Increase the shrinkage factor: It currently uses `800 * density`. To shrink faster, use `400 * density` or something similar so the image shrinks completely before scrolling ends.
       - "mostly shrinken to the bottom so there is more free space at the top, but only about 15% more free space."
         When the size shrinks from 0.8 to 0.4, there is 40% empty space. 15% of this extra space should be put at the top of the image to push it slightly downward, meaning it layout vertically slightly lower than origin.
   - We will modify the `layout` modifier:
     - Compute different `targetScale` for TWS vs Headphones.
     - For TWS: `val targetScale = (1f - (scrollValue / (800 * density))).coerceIn(0.5f, 1f)`
     - For headphones: `val targetScale = (0.8f - (scrollValue / (400 * density))).coerceIn(0.4f, 0.8f)`
     - Compute the top margin for headphones to leave ~15% extra space at top. The space left by shrinking is `maxWidth - placeable.height`.
     - In the original layout, we just put it at 0,0 and returned `layout(placeable.width, placeable.height)`. The container shrink wraps it, meaning 100% of the shrinkage is pulled up from the bottom.
     - If we add `topPadding` to the layout height and place it at `y = topPadding`, then the overall height of the element is `placeable.height + topPadding`, and the elements below it will pull up by `maxWidth - (placeable.height + topPadding)`.
     - The user asks for "only about 15% more free space" at the top. So `topPadding = (maxWidth * 0.8f - placeable.height) * 0.15f`.
     - Wait, the space left by shrinking is compared to the start height (`maxWidth * 0.8f`). The change in height is `(0.8f - targetScale) * maxWidth`.
     - So `topPadding = ((0.8f - targetScale) * constraints.maxWidth * 0.15f).toInt()`.
     - We'll return `layout(placeable.width, placeable.height + topPadding) { placeable.placeRelative(0, topPadding) }` for headphones, and `layout(placeable.width, placeable.height) { placeable.placeRelative(0, 0) }` for TWS.

2. **Run build and tests**
   - `./gradlew :app:assembleDebug`
   - `./gradlew :protocol:test`

3. **Pre-commit checks**
   - Complete pre-commit steps to ensure proper testing, verification, review, and reflection are done.
