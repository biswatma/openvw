package dev.openvw.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun OpenVWTheme(content: @Composable () -> Unit) {
  val scheme = if (Palette.dark)
    darkColorScheme(primary = Ink, onPrimary = Card, secondary = Accent, background = Bg, onBackground = Ink, surface = Card, onSurface = Ink)
  else
    lightColorScheme(primary = Ink, onPrimary = Card, secondary = Accent, background = Bg, onBackground = Ink, surface = Card, onSurface = Ink)

  val view = LocalView.current
  if (!view.isInEditMode) SideEffect {
    val window = (view.context as Activity).window
    // light status/nav icons in dark mode, dark icons in light mode
    WindowCompat.getInsetsController(window, view).apply {
      isAppearanceLightStatusBars = !Palette.dark
      isAppearanceLightNavigationBars = !Palette.dark
    }
  }
  MaterialTheme(colorScheme = scheme, typography = Typography, content = content)
}
