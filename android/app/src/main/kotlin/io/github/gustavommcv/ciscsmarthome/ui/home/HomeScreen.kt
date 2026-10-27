package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.messageRes
import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.Device
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.SensorValue
import io.github.gustavommcv.ciscsmarthome.ui.theme.CiscTheme

/**
 * The first screen that talks to the board: whether it is connected, and the house exactly as the
 * board describes it. The screen of the rooms, with names and controls made for each, replaces the list.
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  HomeContent(state, modifier)
}

@Composable
internal fun HomeContent(state: HomeState, modifier: Modifier = Modifier) {
  LazyColumn(
    modifier = modifier.fillMaxSize().safeDrawingPadding(),
    contentPadding = PaddingValues(24.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    item {
      Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineLarge)
      Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.titleMedium)
    }
    item { ConnectionText(state.connection, Modifier.padding(vertical = 16.dp)) }
    state.home?.areas?.forEach { area ->
      item { Text(area.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 16.dp)) }
      area.devices.forEach { device ->
        item { Text(device.name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary) }
        items(device.entities, key = { it.id.value }) { entity -> EntityRow(entity) }
      }
    }
  }
}

@Composable
private fun ConnectionText(connection: ConnectionState, modifier: Modifier = Modifier) {
  val color =
    when (connection) {
      ConnectionState.Connecting -> MaterialTheme.colorScheme.onSurfaceVariant
      ConnectionState.Connected -> MaterialTheme.colorScheme.primary
      is ConnectionState.Disconnected -> MaterialTheme.colorScheme.error
    }
  Text(stringResource(connection.messageRes()), color = color, style = MaterialTheme.typography.bodyLarge, modifier = modifier)
}

@Composable
private fun EntityRow(entity: Entity) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(entity.name, style = MaterialTheme.typography.bodyLarge)
    Text(valueText(entity), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun valueText(entity: Entity): String {
  val unavailable = stringResource(R.string.value_unavailable)
  return when (entity) {
    is Entity.Sensor ->
      when (val value = entity.value) {
        is SensorValue.Number -> formatNumber(value.value) + (entity.unit?.let { " $it" } ?: "")
        is SensorValue.Text -> value.value
        SensorValue.Unavailable -> unavailable
      }
    is Entity.BinarySensor -> entity.value?.let { stringResource(if (it) R.string.value_yes else R.string.value_no) } ?: unavailable
    is Entity.Switch -> entity.on?.let { stringResource(if (it) R.string.value_on else R.string.value_off) } ?: unavailable
    is Entity.Select -> entity.selected ?: unavailable
  }
}

private val previewHome =
  Home(
    BootId("5f3a9c21"),
    183_450,
    listOf(
      Area(
        AreaId("kitchen"),
        "Kitchen",
        listOf(
          Device(
            DeviceId("gas-sensor"),
            "Gas sensor",
            listOf(
              Entity.Sensor(EntityId("gas-level"), "Gas level", SensorValue.Number(142.0), "adc"),
              Entity.Sensor(EntityId("gas-state"), "Gas state", SensorValue.Text("normal")),
            ),
          ),
        ),
      ),
      Area(
        AreaId("bedroom"),
        "Bedroom",
        listOf(
          Device(
            DeviceId("closet-sensor"),
            "Closet sensor",
            listOf(Entity.Sensor(EntityId("temperature"), "Temperature", SensorValue.Number(21.4), "°C"), Entity.Sensor(EntityId("humidity"), "Humidity", SensorValue.Unavailable, "%")),
          ),
          Device(DeviceId("dehumidifier"), "Dehumidifier", listOf(Entity.Switch(EntityId("dehumidifier-power"), "Power", on = false, commandable = true))),
        ),
      ),
    ),
  )

@Preview(showBackground = true)
@Composable
private fun HomeConnectedPreview() {
  CiscTheme { HomeContent(HomeState(previewHome, ConnectionState.Connected)) }
}

@Preview(showBackground = true)
@Composable
private fun HomeDisconnectedPreview() {
  CiscTheme { HomeContent(HomeState(previewHome, ConnectionState.Disconnected(ConnectionProblem.Unreachable))) }
}
