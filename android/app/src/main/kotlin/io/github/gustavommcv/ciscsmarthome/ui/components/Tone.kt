package io.github.gustavommcv.ciscsmarthome.ui.components

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** How loud something is: the colors of the app, always, a calm one for what is fine, a red one for what needs a look, a quiet grey for the rest. */
internal enum class Tone {
  Primary,
  Alert,
  Neutral,
}

/**
 * The colors of a tone. What is drawn over the [container] is in [content]; a small solid spot of the tone
 * (a dot, a filled badge) is [accent], and what is drawn on it is [onAccent].
 */
internal data class ToneColors(val container: Color, val content: Color, val accent: Color, val onAccent: Color)

internal fun Tone.colors(scheme: ColorScheme): ToneColors =
  when (this) {
    Tone.Primary -> ToneColors(scheme.primaryContainer, scheme.onPrimaryContainer, scheme.primary, scheme.onPrimary)
    Tone.Alert -> ToneColors(scheme.errorContainer, scheme.onErrorContainer, scheme.error, scheme.onError)
    Tone.Neutral -> ToneColors(scheme.surfaceVariant, scheme.onSurfaceVariant, scheme.onSurfaceVariant, scheme.surfaceVariant)
  }

@Composable internal fun Tone.colors(): ToneColors = colors(MaterialTheme.colorScheme)
