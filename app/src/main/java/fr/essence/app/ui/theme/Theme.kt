package fr.essence.app.ui.theme

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
import fr.essence.core.domain.PriceTier

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6B50),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA6F2CF),
    onPrimaryContainer = Color(0xFF002116),
    secondary = Color(0xFF4C6358),
    secondaryContainer = Color(0xFFCEE9DA),
    onSecondaryContainer = Color(0xFF092017),
    tertiary = Color(0xFFB45309),
    tertiaryContainer = Color(0xFFFFDCC2),
    onTertiaryContainer = Color(0xFF2E1500),
    background = Color(0xFFF6FBF7),
    surface = Color(0xFFF6FBF7),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8AD6B4),
    onPrimary = Color(0xFF003827),
    primaryContainer = Color(0xFF00513A),
    onPrimaryContainer = Color(0xFFA6F2CF),
    secondary = Color(0xFFB3CCBF),
    secondaryContainer = Color(0xFF354B41),
    onSecondaryContainer = Color(0xFFCEE9DA),
    tertiary = Color(0xFFFFB77C),
    tertiaryContainer = Color(0xFF713700),
    onTertiaryContainer = Color(0xFFFFDCC2),
    background = Color(0xFF0F1512),
    surface = Color(0xFF0F1512),
)

/** Couleurs fixes des bulles de prix : elles gardent leur sens quel que soit le thème. */
object PriceColors {
    val Cheap = Color(0xFF2E9E57)
    val Medium = Color(0xFFE09B12)
    val Expensive = Color(0xFFD9452B)
    val OutOfStock = Color(0xFF8A8F8C)

    fun of(tier: PriceTier): Color = when (tier) {
        PriceTier.CHEAP -> Cheap
        PriceTier.MEDIUM -> Medium
        PriceTier.EXPENSIVE -> Expensive
    }
}

/** Thème de l'appli ; sur Android 12+, les couleurs suivent le fond d'écran. */
@Composable
fun EssenceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
