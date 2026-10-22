package org.librecans.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code paths of the main journeys so release builds ship them precompiled.
 * Run with `./gradlew :app:generateBaselineProfile` on an emulator or any API 33+ device.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
        openDemoDashboard()
        device.findObject(By.text("Ambient"))?.click()
        device.wait(Until.hasObject(By.text("Ambient sound level")), 5_000)
        scrollThrough()
        openAndReturn("Touch controls")
        openAndReturn("Equalizer")
        openAndReturn("Device settings")
    }
}
