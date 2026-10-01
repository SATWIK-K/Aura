package com.aura.weight

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aura.weight.core.AuraBackdropHost
import com.aura.weight.core.Backdrops
import com.aura.weight.ui.AuraViewModel
import com.aura.weight.ui.Screen
import com.aura.weight.ui.screens.HistoryScreen
import com.aura.weight.ui.screens.HomeScreen
import com.aura.weight.ui.screens.LogSheet
import com.aura.weight.ui.screens.OnboardingScreen
import com.aura.weight.ui.screens.SettingsScreen
import com.aura.weight.ui.theme.AuraTheme
import com.aura.weight.ui.theme.DarkGlass
import com.aura.weight.ui.theme.LightGlass

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { AuraRoot(viewModel()) }
    }
}

@Composable
private fun AuraRoot(vm: AuraViewModel) {
    val profile by vm.profile.collectAsState()
    val entries by vm.entries.collectAsState()
    val screen by vm.screen.collectAsState()
    val logOpen by vm.logOpen.collectAsState()
    val widgetProviders by vm.widgetProviders.collectAsState()
    val dark = profile.darkTheme

    AuraTheme(dark) {
        val spec = Backdrops.of(profile.backdropId)
        AuraBackdropHost(
            spec = if (dark) spec else Backdrops.lightVariant(spec),
            glass = if (dark) DarkGlass else LightGlass,
        ) {
            Box(Modifier.fillMaxSize()) {
                when (screen) {
                    Screen.Onboarding -> OnboardingScreen(profile) { updated ->
                        vm.saveProfile(updated)
                        vm.go(Screen.Home)
                    }

                    Screen.Home -> HomeScreen(
                        profile = profile,
                        entries = entries,
                        onLog = { vm.setLogOpen(true) },
                        onHistory = { vm.go(Screen.History) },
                        onSettings = { vm.go(Screen.Settings) },
                    )

                    Screen.History -> HistoryScreen(
                        profile = profile,
                        entries = entries,
                        onBack = { vm.go(Screen.Home) },
                        onDelete = { vm.delete(it) },
                    )

                    Screen.Settings -> SettingsScreen(
                        profile = profile,
                        widgetProviders = widgetProviders,
                        onBack = { vm.go(Screen.Home) },
                        onSave = { updated ->
                            vm.saveProfile(updated)
                            vm.go(Screen.Home)
                        },
                    )
                }

                val today = vm.today()
                LogSheet(
                    open = logOpen,
                    profile = profile,
                    existingTodayKg = entries.firstOrNull { it.day == today }?.kg,
                    onSave = { kg ->
                        vm.logWeight(kg)
                        vm.setLogOpen(false)
                    },
                    onDismiss = { vm.setLogOpen(false) },
                )
            }
        }
    }
}
