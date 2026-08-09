## 2026-08-09 - Adding accessibility semantic labels to Compose Sliders
**Learning:** By default, standard Jetpack Compose `Slider` components lack contextual semantic information for screen readers like TalkBack, making them read only as "slider" followed by a percentage, without explaining what the slider controls (e.g. Ambient Sound Level vs Clear Bass).
**Action:** When adding or auditing standalone `Slider` components (especially those without explicit programmatic label linkages), manually add `Modifier.semantics { contentDescription = "..." }` to ensure they are fully accessible to screen-reader users.
## 2026-08-08 - Adding accessibility semantic labels to Compose Switches
**Learning:** By default, standard Jetpack Compose `Switch` components without text labels lack contextual semantic information for screen readers like TalkBack, making them read only as "switch, off/on" without explaining what the switch controls (e.g. Adaptive Volume).
**Action:** When adding or auditing standalone `Switch` components (especially those without explicit programmatic label linkages), manually add `Modifier.semantics { contentDescription = "..." }` to ensure they are fully accessible to screen-reader users.
## 2026-08-09 - Adding accessibility semantic labels to CircularProgressIndicator
**Learning:** By default, Jetpack Compose `CircularProgressIndicator` lacks inherent semantic context for screen readers like TalkBack, meaning indeterminate loading states may be ignored or not convey what is actually happening (e.g., "Connecting").
**Action:** When auditing or adding loading indicators like `CircularProgressIndicator`, explicitly attach `Modifier.semantics { contentDescription = "..." }` so screen readers convey meaningful feedback to the user.
