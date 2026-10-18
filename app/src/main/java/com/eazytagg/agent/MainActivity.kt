package com.eazytagg.agent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.eazytagg.agent.ui.EazyTheme
import com.eazytagg.agent.ui.Root
import com.eazytagg.agent.ui.isDarkFor

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dark = isDarkFor(vm.theme)
            val view = LocalView.current
            SideEffect {
                val c = WindowCompat.getInsetsController(window, view)
                c.isAppearanceLightStatusBars = !dark
                c.isAppearanceLightNavigationBars = !dark
            }
            EazyTheme(dark) {
                BackHandler(enabled = vm.stack.size > 1) { vm.back() }
                Root(vm)
            }
        }
    }
}
