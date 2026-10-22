package org.librecans.app.ui

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.librecans.app.data.bluetooth.DeviceKind
import org.librecans.app.data.connection.DeviceTarget
import org.librecans.app.ui.info.DeviceInfoScreen
import org.librecans.app.ui.info.DeviceInfoUiState
import org.librecans.app.ui.theme.LibreCansTheme
import org.librecans.protocol.model.DeviceIdentity
import org.librecans.protocol.state.HeadphoneState
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w427dp-h952dp-xxhdpi")
class DeviceInfoTest {
    @get:Rule
    val compose = createComposeRule()

    private val buds = DeviceTarget("58:18:62:32:96:38", "WF-1000XM5", DeviceKind.EARBUDS)

    private fun show(uniqueId: String) = compose.setContent {
        LibreCansTheme(dynamicColor = false) {
            val identity = DeviceIdentity(modelName = "WF-1000XM5", firmware = "6.1.0", uniqueId = uniqueId)
            DeviceInfoScreen(DeviceInfoUiState(buds, HeadphoneState(identity = identity)), onBack = {})
        }
    }

    @Test
    fun `headphone details come first, then the app`() {
        show("ABC123")
        compose.onNodeWithTag("info_firmware").assertExists()
        compose.onNodeWithText("ABC123").assertExists()
        compose.onNodeWithTag("screen_list").performScrollToNode(hasText("Made by"))
        compose.onNodeWithText("bjakra").assertExists()
    }

    @Test
    fun `a device id that only repeats the bluetooth address is not shown twice`() {
        show("581862329638")
        compose.onNodeWithTag("info_device_id").assertDoesNotExist()
        compose.onNodeWithText("58:18:62:32:96:38").assertExists()
    }
}
