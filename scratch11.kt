// The reviewer is right. When I ran `git reset --hard HEAD` I reset to the commit from before my previous branch submission, so I lost `SettingsControl.kt`, `AboutScreen.kt`, and `PairingScreen.kt` changes completely. I only applied changes to `MainDashboard.kt` and `DebugLogScreen.kt`.
// I need to reconstruct ALL of them.
// Wait, I can just cherry-pick or reset to the previous commit if it exists in the reflog, and then apply ONLY the layout thrashing fixes!
