package io.github.gustavommcv.ciscsmarthome.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

private val BadgeSize = 40.dp

/**
 * An icon in a circle of the color of [tone]: the tinted container, or, when [solid], the strong color of the
 * tone, to call attention to it. Without a [contentDescription] it is decoration, and says nothing to a screen reader.
 */
@Composable
internal fun IconBadge(@DrawableRes icon: Int, tone: Tone, modifier: Modifier = Modifier, solid: Boolean = false, contentDescription: String? = null) {
  val colors = tone.colors()
  Box(modifier = modifier.size(BadgeSize).background(if (solid) colors.accent else colors.container, CircleShape), contentAlignment = Alignment.Center) {
    Icon(painterResource(icon), contentDescription = contentDescription, tint = if (solid) colors.onAccent else colors.content)
  }
}
