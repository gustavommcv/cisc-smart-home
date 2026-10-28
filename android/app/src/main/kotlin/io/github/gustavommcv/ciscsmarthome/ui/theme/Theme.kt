package io.github.gustavommcv.ciscsmarthome.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** The look of the app: its colors, its corners and its spacing. The colors follow the system's light and dark setting. */
@Composable
fun CiscTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, shapes = CiscShapes, content = content)
}
