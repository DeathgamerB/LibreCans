package org.librecans.app

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/** Drives the real app against the simulated WF-1000XM5, end to end through the protocol stack. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class DemoFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                GrantPermissionRule.grant(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                GrantPermissionRule.grant()
            },
        )
        .around(compose)

    private fun connectDemo() {
        compose.onNodeWithTag("screen_list").performScrollToNode(hasTestTag("demo_device"))
        compose.onNodeWithTag("demo_device").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("hero_card"), timeoutMillis = 15_000)
    }

    @Test
    fun connectsToDemoAndShowsLiveState() {
        connectDemo()
        compose.onNodeWithTag("hero_card").assertIsDisplayed()
        compose.onNodeWithTag("noise_card").assertIsDisplayed()
    }

    @Test
    fun switchesNoiseMode() {
        connectDemo()
        compose.onNodeWithTag("mode_AMBIENT").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("ambient_slider"), timeoutMillis = 5_000)
        compose.onNodeWithTag("mode_OFF").performClick()
        compose.waitUntilDoesNotExist(hasTestTag("ambient_slider"), timeoutMillis = 5_000)
    }

    @Test
    fun earbudTapsAreReflectedLive() {
        connectDemo()
        compose.onNodeWithTag("simulate_tap").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("ambient_slider"), timeoutMillis = 5_000)
    }

    @Test
    fun disablesTheRightEarbud() {
        connectDemo()
        compose.onNodeWithTag("screen_list").performScrollToNode(hasTestTag("link_touch"))
        compose.onNodeWithTag("link_touch").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("change_1"), timeoutMillis = 5_000)
        compose.onNodeWithTag("change_1").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Not assigned"), timeoutMillis = 5_000)
        compose.onNodeWithText("Not assigned").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Taps do nothing"), timeoutMillis = 5_000)
    }

    @Test
    fun appliesAnEqualizerPreset() {
        connectDemo()
        compose.onNodeWithTag("screen_list").performScrollToNode(hasTestTag("link_equalizer"))
        compose.onNodeWithTag("link_equalizer").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("preset_22"), timeoutMillis = 5_000)
        compose.onNodeWithTag("preset_22").performClick()
        compose.onNodeWithTag("back").performClick()
        compose.waitUntilAtLeastOneExists(hasText("Bass Boost"), timeoutMillis = 5_000)
    }

    @Test
    fun disconnectReturnsToTheDeviceList() {
        connectDemo()
        compose.onNodeWithTag("screen_list").performScrollToNode(hasTestTag("disconnect"))
        compose.onNodeWithTag("disconnect").performClick()
        compose.waitUntilAtLeastOneExists(hasTestTag("demo_device"), timeoutMillis = 5_000)
    }
}
