package com.eazytagg.agent

import android.app.Application
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.eazytagg.agent.data.*
import com.eazytagg.agent.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w1280dp-h1280dp-mdpi")
class AgentLayoutTest {
    @get:Rule val compose = createComposeRule()

    private fun signIn(width: Int, height: Int, dark: Boolean) {
        val vm = AppViewModel(RuntimeEnvironment.getApplication())
        compose.setContent { PDynamicsTheme(dark) { Box(Modifier.requiredSize(width.dp, height.dp)) { SignInScreen(vm) } } }
        compose.onNodeWithText("Sign in", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Test connection").performScrollTo().assertIsDisplayed()
        compose.onAllNodesWithContentDescription("Popular Dynamics").assertCountEquals(2)
    }
    @Test fun lightPortraitSignInKeepsConnectionAndSignInAccessible() = signIn(360, 740, false)
    @Test fun darkPortraitSignInKeepsConnectionAndSignInAccessible() = signIn(360, 740, true)
    @Test fun landscapeUsesTheSplitTemplateWithAccessibleActions() = signIn(1080, 640, false)
    @Test fun shortKeyboardWindowAllowsScrollingToSignIn() = signIn(360, 380, true)

    private fun guestIcons(dark: Boolean) {
        val vm = AppViewModel(RuntimeEnvironment.getApplication())
        vm.selectedGuest = SearchHit("synthetic", "Synthetic Guest", mapOf("phone" to "0000000000"))
        vm.event = EventInfo("TEST", listOf(Field("phone", "Phone", "phone", false, emptyList(), "")), "", "", "Test event")
        compose.setContent { PDynamicsTheme(dark) { Box(Modifier.requiredSize(400.dp, 800.dp)) { GuestScreen(vm) } } }
        for (label in listOf("Check in", "Check out", "Print tag", "Sessions", "Edit details"))
            compose.onNodeWithContentDescription(label).assertIsDisplayed()
    }
    @Test fun allFiveGuestIconsAreVisibleInLightMode() = guestIcons(false)
    @Test fun allFiveGuestIconsAreVisibleInDarkMode() = guestIcons(true)
    @Test fun unavailableTorchIsDisabledAndCameraChoiceWorks() {
        val front = mutableStateOf(false)
        compose.setContent { PDynamicsTheme(true) { CameraControls(front.value, true, true, false, false, { front.value = it }, {}) } }
        compose.onNodeWithText("Front").performClick()
        compose.runOnIdle { assertTrue(front.value) }
        compose.onNode(isToggleable()).assertIsNotEnabled()
        compose.onNodeWithText("Torch unavailable").assertIsDisplayed()
    }
}
