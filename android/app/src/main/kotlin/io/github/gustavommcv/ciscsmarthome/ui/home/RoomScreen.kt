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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.CommandValue
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.Device
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.EntityIds
import io.github.gustavommcv.ciscsmarthome.domain.Names
import io.github.gustavommcv.ciscsmarthome.ui.components.CiscCard
import io.github.gustavommcv.ciscsmarthome.ui.components.IconBadge
import io.github.gustavommcv.ciscsmarthome.ui.components.LockedCard
import io.github.gustavommcv.ciscsmarthome.ui.components.RenameDialog
import io.github.gustavommcv.ciscsmarthome.ui.components.SectionTitle
import io.github.gustavommcv.ciscsmarthome.ui.components.Tone
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing

/** One room: its devices, what each reads, and the controls of what can be commanded. The history chart comes to it next. */
@Composable
internal fun RoomScreen(
  area: Area,
  connection: ConnectionState,
  names: Names,
  pending: Set<EntityId>,
  notice: CommandNotice?,
  onBack: () -> Unit,
  onCommand: (EntityId, CommandValue) -> Unit,
  onNoticeShown: () -> Unit,
  onRenameRoom: (String?) -> Unit,
  onRenameDevice: (DeviceId, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  // Which dialog is open: the room's, or the device with this id. Kept across a rotation, so a name half typed is not lost with the screen.
  var renamingRoom by rememberSaveable { mutableStateOf(false) }
  var renamingDevice by rememberSaveable { mutableStateOf<String?>(null) }
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
        Text(roomName(area, names), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        RenameButton(roomName(area, names), onClick = { renamingRoom = true })
      }
      LazyColumn(contentPadding = PaddingValues(start = Spacing.Xl, end = Spacing.Xl, bottom = Spacing.Xl), verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
        item { ConnectionBanner(connection, showingLastReading = true, modifier = Modifier.padding(bottom = Spacing.S)) }
        item { SectionTitle(R.string.section_devices) }
        if (area.devices.isEmpty()) item { Text(stringResource(R.string.room_empty), style = MaterialTheme.typography.bodyMedium) }
        items(area.devices, key = { it.id.value }) { device -> DeviceCard(device, names, connection, pending, onCommand, onRename = { renamingDevice = device.id.value }) }
        item { LockedCard(R.string.device_add) }
      }
    }
  }

  if (renamingRoom) {
    RenameDialog(
      title = R.string.rename_room_title,
      current = roomName(area, names),
      hasCustomName = area.id in names.areas,
      onSave = { onRenameRoom(it); renamingRoom = false },
      onUseDefault = { onRenameRoom(null); renamingRoom = false },
      onDismiss = { renamingRoom = false },
    )
  }
  area.devices.firstOrNull { it.id.value == renamingDevice }?.let { device ->
    RenameDialog(
      title = R.string.rename_device_title,
      current = deviceName(device, names),
      hasCustomName = device.id in names.devices,
      onSave = { onRenameDevice(device.id, it); renamingDevice = null },
      onUseDefault = { onRenameDevice(device.id, null); renamingDevice = null },
      onDismiss = { renamingDevice = null },
    )
  }
}

/** The pencil that renames what it sits beside; a screen reader says which name it renames. */
@Composable
private fun RenameButton(name: String, onClick: () -> Unit) {
  IconButton(onClick = onClick) { Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.rename_description, name)) }
}

/** A device in a card: its icon, its name and how it is, and inside the card what it reads and what it lets the user command. */
@Composable
private fun DeviceCard(device: Device, names: Names, connection: ConnectionState, pending: Set<EntityId>, onCommand: (EntityId, CommandValue) -> Unit, onRename: () -> Unit) {
  val emergency = device.isInEmergency()
  val subtle = if (emergency) LocalContentColor.current else MaterialTheme.colorScheme.onSurfaceVariant
  CiscCard(tone = if (emergency) Tone.Alert else null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.M)) {
      IconBadge(device.id.iconRes(), if (emergency) Tone.Alert else Tone.Primary, solid = emergency)
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
        Text(deviceName(device, names), style = MaterialTheme.typography.titleMedium)
        headlineText(device)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = subtle) }
      }
      RenameButton(deviceName(device, names), onClick = onRename)
    }
    device.rows().forEachIndexed { index, entity ->
      // The first line sets the rows apart from the header; the rows carry the room above and below themselves, so each sits halfway between two lines.
      HorizontalDivider(modifier = Modifier.padding(top = if (index == 0) Spacing.M else 0.dp), thickness = Dp.Hairline, color = MaterialTheme.colorScheme.outlineVariant)
      val enabled = entity.controlEnabled(connection, pending)
      when {
        entity is Entity.Switch && entity.commandable -> SwitchRow(entity, enabled) { onCommand(entity.id, entity.toggleCommand()) }
        entity is Entity.Select && entity.commandable -> SelectRow(entity, enabled) { option -> onCommand(entity.id, CommandValue.Choice(option)) }
        else -> ReadRow(entity)
      }
    }
  }
}

/** The line under the name of a device: how it is, in its own words. */
@Composable
private fun headlineText(device: Device): String? =
  device.headline()?.let { headline ->
    val value = entityValue(headline.entity)
    if (headline.withLabel) "${entityLabel(headline.entity)} $value" else value
  }

@Composable
private fun ReadRow(entity: Entity) {
  Row(modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.S), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(entityLabel(entity), style = MaterialTheme.typography.bodyLarge)
    Text(entityValue(entity), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

/** The whole row is the target of the touch, not only the small switch. */
@Composable
private fun SwitchRow(entity: Entity.Switch, enabled: Boolean, onToggle: () -> Unit) {
  Row(
    modifier =
      Modifier.fillMaxWidth().toggleable(value = entity.on == true, enabled = enabled, role = Role.Switch, onValueChange = { onToggle() }).padding(vertical = Spacing.S),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(entityLabel(entity), style = MaterialTheme.typography.bodyLarge)
    Switch(checked = entity.on == true, onCheckedChange = null, enabled = enabled)
  }
}

@Composable
private fun SelectRow(entity: Entity.Select, enabled: Boolean, onChoose: (String) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(Spacing.S), modifier = Modifier.padding(vertical = Spacing.S)) {
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
