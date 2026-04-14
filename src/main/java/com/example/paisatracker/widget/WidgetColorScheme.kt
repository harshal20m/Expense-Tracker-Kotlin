package com.example.paisatracker.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.material3.ColorProviders

// All widget colors live here — one source of truth.
// These are intentionally dark-only; widgets always show dark UI.
private val WidgetDarkColors = androidx.compose.material3.darkColorScheme(
    primary        = Color(0xFF10B981), // green
    secondary      = Color(0xFFF59E0B), // amber
    error          = Color(0xFFEF4444), // red
    background     = Color(0xFF1A1A2E), // dark navy
    surface        = Color(0xFF111827), // darker surface
    onPrimary      = Color.White,
    onSecondary    = Color.White,
    onBackground   = Color.White,
    onSurface      = Color.White,
    onError        = Color.White,
)

// Pass the same palette for light — widgets won't use light mode
// but ColorProviders requires both parameters
object WidgetColorScheme {
    val colors = ColorProviders(
        light = WidgetDarkColors,
        dark  = WidgetDarkColors
    )
}

// Semantic convenience — read these inside provideContent { GlanceTheme { ... } }
// via GlanceTheme.colors.primary, .secondary, .error, .onBackground, etc.