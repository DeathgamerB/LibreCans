package org.librecans.app

import android.app.Application
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import org.librecans.protocol.SonyProtocolVersion

class UiTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun dashboard_renders() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = HeadphonesViewModel(application)

        composeTestRule.setContent {
            LibreCansTheme {
                MainDashboard(
                    viewModel = viewModel,
                    onNavigateToAbout = {},
                    onNavigateToPairing = {}
                )
            }
        }

        // Verify the main title / structural container renders
        composeTestRule.onNodeWithText("LibreCans").assertExists()
    }

    @Test
    fun panels_expand_and_collapse() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = HeadphonesViewModel(application)

        composeTestRule.setContent {
            LibreCansTheme {
                MainDashboard(
                    viewModel = viewModel,
                    onNavigateToAbout = {},
                    onNavigateToPairing = {}
                )
            }
        }

        // Assert EQ panel is not expanded
        composeTestRule.onNodeWithTag("eq_panel_content").assertDoesNotExist()

        // Click to expand
        composeTestRule.onNodeWithText("Equalizer").performClick()

        // Assert it is now displayed structurally
        composeTestRule.onNodeWithTag("eq_panel_content").assertIsDisplayed()

        // Same for Settings/Features
        composeTestRule.onNodeWithTag("settings_panel_content").assertDoesNotExist()
        composeTestRule.onNodeWithText("Features & Configuration").performClick()
        composeTestRule.onNodeWithTag("settings_panel_content").assertIsDisplayed()
    }

    @Test
    fun navigation_back_stack() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = HeadphonesViewModel(application)

        composeTestRule.setContent {
            var currentScreen by remember { mutableStateOf("main") }

            LibreCansTheme {
                if (currentScreen == "main") {
                    MainDashboard(
                        viewModel = viewModel,
                        onNavigateToAbout = { currentScreen = "about" },
                        onNavigateToPairing = {}
                    )
                } else if (currentScreen == "about") {
                    AboutScreen(
                        onNavigateToDebugLogs = {},
                        onBack = { currentScreen = "main" }
                    )
                }
            }
        }

        // Start on Main
        composeTestRule.onNodeWithText("LibreCans").assertIsDisplayed()

        // Navigate to About
        composeTestRule.onNodeWithText("About").performClick()

        // Assert About screen renders
        composeTestRule.onNodeWithText("Version 1.0.0").assertIsDisplayed()

        // Navigate Back
        composeTestRule.onNodeWithText("Back to Dashboard").performClick()

        // Assert Main is back
        composeTestRule.onNodeWithText("LibreCans").assertIsDisplayed()
    }

    @Test
    fun device_list_renders_and_loading() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = HeadphonesViewModel(application)

        composeTestRule.setContent {
            LibreCansTheme {
                PairingScreen(
                    viewModel = viewModel,
                    onNavigateToMain = {}
                )
            }
        }

        // Without mocked internal paired devices, verify the empty state structually
        composeTestRule.onNodeWithTag("device_list").assertDoesNotExist()
        composeTestRule.onNodeWithText("No paired Sony headsets found. Pair them in Android settings first.").assertIsDisplayed()

        // Trigger connection state by simulating mock button click
        composeTestRule.onNodeWithText("Simulate").performClick()

        // Verify the loading struct exists
        composeTestRule.onNodeWithTag("loading_state").assertExists()
    }
}
