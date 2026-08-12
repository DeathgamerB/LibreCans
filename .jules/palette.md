## 2026-08-12 - Adding accessibility semantic labels to Compose Sliders
**Learning:** By default, standard Jetpack Compose `Slider` components lack contextual semantic information for screen readers like TalkBack, making them read only as "slider" followed by a percentage, without explaining what the slider controls (e.g. Ambient Sound Level vs Clear Bass).
**Action:** When adding or auditing standalone `Slider` components (especially those without explicit programmatic label linkages), manually add `Modifier.semantics { contentDescription = "..." }` to ensure they are fully accessible to screen-reader users.
## 2026-08-08 - Adding accessibility semantic labels to Compose Switches
**Learning:** By default, standard Jetpack Compose `Switch` components without text labels lack contextual semantic information for screen readers like TalkBack, making them read only as "switch, off/on" without explaining what the switch controls (e.g. Adaptive Volume).
**Action:** When adding or auditing standalone `Switch` components (especially those without explicit programmatic label linkages), manually add `Modifier.semantics { contentDescription = "..." }` to ensure they are fully accessible to screen-reader users.
## 2026-08-12 - Adding accessibility semantic labels to CircularProgressIndicator
**Learning:** In Jetpack Compose, indeterminate `CircularProgressIndicator` components lack inherent semantic context for screen readers by default. Screen readers like TalkBack might skip them or provide no context about what is loading.
**Action:** When adding or auditing `CircularProgressIndicator` components indicating a loading state, apply `Modifier.semantics { contentDescription = "..." }` to ensure the loading state (e.g., 'Connecting') is announced properly by screen readers.
