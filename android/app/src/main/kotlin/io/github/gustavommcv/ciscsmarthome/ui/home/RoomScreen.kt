package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.CommandValue
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.EntityIds
import io.github.gustavommcv.ciscsmarthome.ui.components.LockedCard
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing

/** One room: its devices, what each reads, and the controls of what can be commanded. The history chart comes to it next. */
@Composable
internal fun RoomScreen(
  area: Area,
  connection: ConnectionState,
  pending: Set<EntityId>,
  notice: CommandNotice?,
  onBack: () -> Unit,
  onCommand: (EntityId, CommandValue) -> Unit,
  onNoticeShown: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val snackbar = remember { SnackbarHostState() }
  val message = notice?.let { stringResource(it.messageRes()) }
  // A command that was not done is said once, and gone: it is not a state of the room.
  LaunchedEffect(notice) {
    if (message != null) {
      snackbar.showSnackbar(message)
      onNoticeShown()
    }
  }
  // The app's own scaffold has already made room for the bars of the system, so this one asks for none.
  Scaffold(modifier = modifier.fillMaxSize(), snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = WindowInsets(0, 0, 0, 0)) { insets ->
    Column(modifier = Modifier.padding(insets)) {
      Row(modifier = Modifier.padding(horizontal = Spacing.S, vertical = Spacing.S), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back)) }
        Text(roomName(area), style = MaterialTheme.typography.headlineSmall)
      }
      LazyColumn(contentPadding = PaddingValues(start = Spacing.Xl, end = Spacing.Xl, bottom = Spacing.Xl), verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
        item { ConnectionBanner(connection, showingLastReading = true, modifier = Modifier.padding(bottom = Spacing.S)) }
        if (area.devices.isEmpty()) item { Text(stringResource(R.string.room_empty), style = MaterialTheme.typography.bodyMedium) }
        area.devices.forEach { device ->
          item { Text(deviceName(device), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = Spacing.L)) }
          items(device.entities, key = { it.id.value }) { entity ->
            val enabled = entity.controlEnabled(connection, pending)
            when {
              entity is Entity.Switch && entity.commandable -> SwitchRow(entity, enabled) { onCommand(entity.id, entity.toggleCommand()) }
              entity is Entity.Select && entity.commandable -> SelectRow(entity, enabled) { option -> onCommand(entity.id, CommandValue.Choice(option)) }
              else -> ReadRow(entity)
            }
          }
        }
        item { LockedCard(R.string.device_add, modifier = Modifier.padding(top = Spacing.L)) }
      }
    }
  }
}

@Composable
private fun ReadRow(entity: Entity) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(entityLabel(entity), style = MaterialTheme.typography.bodyLarge)
    Text(entityValue(entity), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

/** The whole row is the target of the touch, not only the small switch. */
@Composable
private fun SwitchRow(entity: Entity.Switch, enabled: Boolean, onToggle: () -> Unit) {
  Row(
    modifier =
      Modifier.fillMaxWidth().toggleable(value = entity.on == true, enabled = enabled, role = Role.Switch, onValueChange = { onToggle() }).padding(vertical = Spacing.Xs),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(entityLabel(entity), style = MaterialTheme.typography.bodyLarge)
    Switch(checked = entity.on == true, onCheckedChange = null, enabled = enabled)
  }
}

@Composable
private fun SelectRow(entity: Entity.Select, enabled: Boolean, onChoose: (String) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(Spacing.S), modifier = Modifier.padding(vertical = Spacing.Xs)) {
    Text(entityLabel(entity), style = MaterialTheme.typography.bodyLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
      entity.options.forEach { option ->
        FilterChip(
          selected = option == entity.selected,
          // Asking for what already is would only be a command for nothing.
          onClick = { if (option != entity.selected) onChoose(option) },
          enabled = enabled,
          label = { Text(optionRes(option)?.let { stringResource(it) } ?: option) },
        )
      }
    }
    if (entity.id == EntityIds.DehumidifierMode) {
      Text(stringResource(R.string.hint_dehumidifier_mode), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
