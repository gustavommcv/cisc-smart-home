package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import io.github.gustavommcv.ciscsmarthome.ui.components.CiscCard
import io.github.gustavommcv.ciscsmarthome.ui.components.IconBadge
import io.github.gustavommcv.ciscsmarthome.ui.components.LockedCard
import io.github.gustavommcv.ciscsmarthome.ui.components.Tone
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.Home

/** The first screen: a card for each room, with what matters about it, and the cards of what is to come. */
@Composable
internal fun RoomsScreen(home: Home, connection: ConnectionState, onOpenRoom: (AreaId) -> Unit, modifier: Modifier = Modifier) {
  val lastReading = connection is ConnectionState.Disconnected
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(Spacing.Xl),
    verticalArrangement = Arrangement.spacedBy(Spacing.M),
  ) {
    item {
      Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineLarge)
      Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.titleMedium)
      ConnectionBanner(connection, showingLastReading = true, modifier = Modifier.padding(top = Spacing.L, bottom = Spacing.Xs))
    }
    items(home.areas, key = { it.id.value }) { area ->
      RoomCard(area, inEmergency = home.isInEmergency(area), dimmed = lastReading, onClick = { onOpenRoom(area.id) })
    }
    item { LockedCard(R.string.rooms_add) }
  }
}

@Composable
private fun RoomCard(area: Area, inEmergency: Boolean, dimmed: Boolean, onClick: () -> Unit) {
  CiscCard(onClick = onClick, tone = if (inEmergency) Tone.Alert else null, modifier = Modifier.alpha(if (dimmed) DIMMED else 1f)) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.M)) {
      // A room in emergency says so with a solid red warning in the place of its own icon.
      IconBadge(if (inEmergency) R.drawable.ic_warning else area.id.iconRes(), if (inEmergency) Tone.Alert else Tone.Primary, solid = inEmergency)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
        Text(roomName(area), style = MaterialTheme.typography.titleMedium)
        val highlights = area.highlights()
        if (highlights.isEmpty()) Text(stringResource(R.string.room_empty), style = MaterialTheme.typography.bodyMedium)
        highlights.forEach { entity ->
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(entityLabel(entity), style = MaterialTheme.typography.bodyMedium)
            Text(entityValue(entity), style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }
  }
}

// The cards say they show the last reading, not the present one, by being fainter.
private const val DIMMED = 0.6f
