## $(date +%Y-%m-%d) - Added semantics to CircularProgressIndicator
**Learning:** In Jetpack Compose, indeterminate `CircularProgressIndicator` components lack inherent semantic context for screen readers by default. Always apply `Modifier.semantics { contentDescription = "..." }` to ensure the loading state (e.g., 'Connecting') is announced by TalkBack.
**Action:** When adding indeterminate progress indicators in Compose, immediately add a semantics modifier with a descriptive contentDescription.
