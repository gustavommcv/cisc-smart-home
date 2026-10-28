package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.ui.components.CiscCard
import io.github.gustavommcv.ciscsmarthome.ui.components.IconBadge
import io.github.gustavommcv.ciscsmarthome.ui.components.LockedCard
import io.github.gustavommcv.ciscsmarthome.ui.components.SectionTitle
import io.github.gustavommcv.ciscsmarthome.ui.components.Tone
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing

/**
 * The first screen: how the house is, in one card, then a card for each room, which says how many devices it has and how it
 * is (not what each reads: that is for the room), and the card of what is to come.
 */
@Composable
internal fun RoomsScreen(home: Home, connection: ConnectionState, onOpenRoom: (AreaId) -> Unit, modifier: Modifier = Modifier) {
  // What is shown is the last reading when the board is lost, and the cards say so by being fainter.
  val dim = Modifier.alpha(if (connection is ConnectionState.Disconnected) DIMMED else 1f)
  LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(Spacing.Xl), verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
    item {
      Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineLarge)
      Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.titleMedium)
      ConnectionBanner(connection, showingLastReading = true, modifier = Modifier.padding(top = Spacing.L, bottom = Spacing.Xs))
    }
    item { SummaryCard(home, dim) }
    item { SectionTitle(R.string.section_rooms, Modifier.padding(top = Spacing.S)) }
    items(home.areas, key = { it.id.value }) { area -> RoomCard(area, dim, onClick = { onOpenRoom(area.id) }) }
    item { LockedCard(R.string.rooms_add) }
  }
}

/** How the whole house is, so the first thing the screen says is whether anything needs a look. */
@Composable
private fun SummaryCard(home: Home, modifier: Modifier) {
  val status = home.status()
  val emergency = status == Status.Emergency
  val resources = LocalResources.current
  CiscCard(modifier = modifier, tone = if (emergency) Tone.Alert else null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.M)) {
      IconBadge(status.iconRes(), status.tone(), solid = emergency)
      Column(verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
        Text(resources.summaryTitle(status), style = MaterialTheme.typography.titleMedium)
        Text(resources.summaryText(status, home.emergencyArea()?.let { roomName(it) }), style = MaterialTheme.typography.bodyMedium, color = subtleColor(emergency))
      }
    }
  }
}

@Composable
private fun RoomCard(area: Area, modifier: Modifier, onClick: () -> Unit) {
  val status = area.status()
  val emergency = status == Status.Emergency
  val resources = LocalResources.current
  CiscCard(onClick = onClick, modifier = modifier, tone = if (emergency) Tone.Alert else null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.M)) {
      // A room in emergency says so with a solid red warning in the place of its own icon.
      IconBadge(if (emergency) R.drawable.ic_warning else area.id.iconRes(), if (emergency) Tone.Alert else Tone.Primary, solid = emergency)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
        Text(roomName(area), style = MaterialTheme.typography.titleMedium)
        Text(resources.roomSubtitle(area, status), style = MaterialTheme.typography.bodyMedium, color = subtleColor(emergency))
      }
      Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = subtleColor(emergency))
    }
  }
}

// The quiet words of a card: grey on a white card, and the card's own color on an alert card, where grey would fade.
@Composable
private fun subtleColor(emergency: Boolean) = if (emergency) LocalContentColor.current else MaterialTheme.colorScheme.onSurfaceVariant

// The cards say they show the last reading, not the present one, by being fainter.
private const val DIMMED = 0.6f
