package org.librecans.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

/** Benchmark builds carry a `.benchmark` suffix; the plugin passes the real id. */
val PACKAGE: String = InstrumentationRegistry.getArguments()
    .getString("androidx.benchmark.targetPackageName") ?: "org.librecans.app"
private const val TIMEOUT = 10_000L

/** Opens the demo headphones from the device list and waits for the dashboard. */
fun MacrobenchmarkScope.openDemoDashboard() {
    device.executeShellCommand("pm grant $packageName android.permission.BLUETOOTH_CONNECT")
    device.executeShellCommand("cmd statusbar collapse")
    pressHome()
    startActivityAndWait()
    val demo = device.wait(Until.findObject(By.text("Demo headphones")), TIMEOUT)
        ?: list().run {
            scrollUntil(Direction.DOWN, Until.findObject(By.text("Demo headphones")))
        }
    demo.click()
    check(device.wait(Until.hasObject(By.text("Noise control")), TIMEOUT)) { "Dashboard did not open" }
}

fun MacrobenchmarkScope.list(): UiObject2 = device.wait(Until.findObject(By.scrollable(true)), TIMEOUT)

/** Scrolls the current screen to the end and back, the way a user looks through it. */
fun MacrobenchmarkScope.scrollThrough() {
    repeat(2) {
        fling(Direction.DOWN)
        fling(Direction.UP)
    }
}

private fun MacrobenchmarkScope.fling(direction: Direction) {
    // Lists run edge to edge; swipes near the top would pull down the notification shade.
    list().apply {
        setGestureMargins(device.displayWidth / 10, device.displayHeight / 4, device.displayWidth / 10, device.displayHeight / 5)
    }.fling(direction)
    device.waitForIdle()
}

/** Opens a dashboard link, looks through the screen and returns. */
fun MacrobenchmarkScope.openAndReturn(text: String) {
    list().scrollUntil(Direction.DOWN, Until.findObject(By.text(text)))?.click()
    device.wait(Until.gone(By.text("Customize")), TIMEOUT)
    device.waitForIdle()
    scrollThrough()
    device.pressBack()
    device.wait(Until.hasObject(By.text("Customize")), TIMEOUT)
    device.waitForIdle()
}
