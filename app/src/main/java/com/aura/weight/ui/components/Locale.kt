package com.aura.weight.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.util.Locale

@Composable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
