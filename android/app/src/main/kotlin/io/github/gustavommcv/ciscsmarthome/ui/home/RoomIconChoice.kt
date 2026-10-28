package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.RoomIcon
import io.github.gustavommcv.ciscsmarthome.ui.components.Tone
import io.github.gustavommcv.ciscsmarthome.ui.components.colors
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing

private val OptionSize = 48.dp

/**
 * The icons a room can have, all of them in sight, the chosen one marked. Each is a round button as big as a finger needs, and says its name to a screen
 * reader; the name of the chosen one is also written below, so the choice is not a guess at a small picture.
 */
@Composable
internal fun RoomIconChoice(chosen: RoomIcon, onChoose: (RoomIcon) -> Unit, modifier: Modifier = Modifier) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
    Text(stringResource(R.string.room_icon_title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.S), verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
      RoomIcon.entries.forEach { option -> Option(option, selected = option == chosen, onClick = { onChoose(option) }) }
    }
    Text(stringResource(chosen.labelRes()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun Option(option: RoomIcon, selected: Boolean, onClick: () -> Unit) {
  val colors = Tone.Primary.colors()
  val label = stringResource(option.labelRes())
  Box(
    modifier =
      Modifier.size(OptionSize)
        .clip(CircleShape)
        .background(if (selected) colors.container else MaterialTheme.colorScheme.surfaceVariant)
        .border(if (selected) BorderStroke(2.dp, colors.accent) else BorderStroke(0.dp, androidx.compose.ui.graphics.Color.Transparent), CircleShape)
        .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    contentAlignment = Alignment.Center,
  ) {
    Icon(painterResource(option.iconRes()), contentDescription = label, tint = if (selected) colors.content else MaterialTheme.colorScheme.onSurfaceVariant)
  }
}
