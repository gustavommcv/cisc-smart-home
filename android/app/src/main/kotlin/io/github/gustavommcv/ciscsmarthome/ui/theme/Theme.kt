package io.github.gustavommcv.ciscsmarthome.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors =
  lightColorScheme(
    primary = Color(0xFF006874),
    primaryContainer = Color(0xFF9EF0FF),
    secondary = Color(0xFF4A6267),
    tertiary = Color(0xFF525E7D),
  )

private val DarkColors =
  darkColorScheme(
    primary = Color(0xFF4FD8EB),
    primaryContainer = Color(0xFF004F58),
    secondary = Color(0xFFB1CBD0),
    tertiary = Color(0xFFBAC6EA),
  )

/** The look of the app. The colors follow the system's light and dark setting. */
@Composable
fun CiscTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
