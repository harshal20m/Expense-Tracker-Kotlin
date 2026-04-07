package com.example.paisatracker.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.paisatracker.data.AppTheme

// Default Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5),
)

// Default Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

// Midnight Theme
private val MidnightColorScheme = darkColorScheme(
    primary = MidnightPrimary,
    secondary = MidnightSecondary,
    tertiary = MidnightTertiary,
    background = MidnightBackground,
    surface = MidnightSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
)

// Soft Light Theme
private val SoftLightColorScheme = lightColorScheme(
    primary = SoftLightPrimary,
    secondary = SoftLightSecondary,
    tertiary = SoftLightTertiary,
    background = SoftLightBackground,
    surface = SoftLightSurface,
    onPrimary = Color(0xFF3E2723),
    onSecondary = Color(0xFF3E2723),
    onTertiary = Color(0xFF3E2723),
    onBackground = Color(0xFF3E2723),
    onSurface = Color(0xFF3E2723),
)

// Ocean Theme
private val OceanColorScheme = lightColorScheme(
    primary = OceanPrimary,
    secondary = OceanSecondary,
    tertiary = OceanTertiary,
    background = OceanBackground,
    surface = OceanSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

// Sunset Theme
private val SunsetColorScheme = lightColorScheme(
    primary = SunsetPrimary,
    secondary = SunsetSecondary,
    tertiary = SunsetTertiary,
    background = SunsetBackground,
    surface = SunsetSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

// Forest Theme
private val ForestColorScheme = lightColorScheme(
    primary = ForestPrimary,
    secondary = ForestSecondary,
    tertiary = ForestTertiary,
    background = ForestBackground,
    surface = ForestSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

// Rose Theme
private val RoseColorScheme = lightColorScheme(
    primary = RosePrimary,
    secondary = RoseSecondary,
    tertiary = RoseTertiary,
    background = RoseBackground,
    surface = RoseSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

// Lavender Theme
private val LavenderColorScheme = lightColorScheme(
    primary = LavenderPrimary,
    secondary = LavenderSecondary,
    tertiary = LavenderTertiary,
    background = LavenderBackground,
    surface = LavenderSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

// Deep Blue Theme (Dark)
private val DeepBlueColorScheme = darkColorScheme(
    primary = DeepBluePrimary,
    secondary = DeepBlueSecondary,
    tertiary = DeepBlueTertiary,
    background = DeepBlueBackground,
    surface = DeepBlueSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
)

// Coffee Theme (Dark)
private val CoffeeColorScheme = darkColorScheme(
    primary = CoffeePrimary,
    secondary = CoffeeSecondary,
    tertiary = CoffeeTertiary,
    background = CoffeeBackground,
    surface = CoffeeSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFF5E6D3),
    onSurface = Color(0xFFF5E6D3),
)

// Slate Theme (Dark)
private val SlateColorScheme = darkColorScheme(
    primary = SlatePrimary,
    secondary = SlateSecondary,
    tertiary = SlateTertiary,
    background = SlateBackground,
    surface = SlateSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFECEFF1),
    onSurface = Color(0xFFECEFF1),
)

// Soft Pink Theme
private val SoftPinkColorScheme = lightColorScheme(
    primary = SoftPinkPrimary,
    secondary = SoftPinkSecondary,
    tertiary = SoftPinkTertiary,
    background = SoftPinkBackground,
    surface = SoftPinkSurface,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

// Hot Pink Theme
private val HotPinkColorScheme = lightColorScheme(
    primary = HotPinkPrimary,
    secondary = HotPinkSecondary,
    tertiary = HotPinkTertiary,
    background = HotPinkBackground,
    surface = HotPinkSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

// Rose Gold Theme
private val RoseGoldColorScheme = lightColorScheme(
    primary = RoseGoldPrimary,
    secondary = RoseGoldSecondary,
    tertiary = RoseGoldTertiary,
    background = RoseGoldBackground,
    surface = RoseGoldSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
)

@Composable
fun PaisaTrackerTheme(
    appTheme: AppTheme = AppTheme.SYSTEM_DEFAULT,
    content: @Composable () -> Unit
) {
    val darkTheme: Boolean = isSystemInDarkTheme()
    val context = LocalContext.current

    val colorScheme = when (appTheme) {
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.DARK -> DarkColorScheme
        AppTheme.MIDNIGHT -> MidnightColorScheme
        AppTheme.SOFT_LIGHT -> SoftLightColorScheme
        AppTheme.OCEAN -> OceanColorScheme
        AppTheme.SUNSET -> SunsetColorScheme
        AppTheme.FOREST -> ForestColorScheme
        AppTheme.ROSE -> RoseColorScheme
        AppTheme.LAVENDER -> LavenderColorScheme
        AppTheme.DEEP_BLUE -> DeepBlueColorScheme
        AppTheme.COFFEE -> CoffeeColorScheme
        AppTheme.SLATE -> SlateColorScheme
        AppTheme.SOFT_PINK -> SoftPinkColorScheme
        AppTheme.HOT_PINK -> HotPinkColorScheme
        AppTheme.ROSE_GOLD -> RoseGoldColorScheme
        AppTheme.SYSTEM_DEFAULT -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        AppTheme.WALLPAPER_ORIENTED -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}