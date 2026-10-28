package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
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

private fun previewHome(gasState: String) =
  Home(
    BootId("5f3a9c21"),
    183_450,
    listOf(
      Area(
        AreaId("kitchen"),
        "Kitchen",
        listOf(
          Device(
            DeviceId("gas-monitor"),
            "Gas monitor",
            listOf(
              Entity.Sensor(EntityId("gas-level"), "Gas level", SensorValue.Number(142.0), "adc"),
              Entity.Sensor(EntityId("gas-state"), "Gas state", SensorValue.Text(gasState)),
              Entity.Sensor(EntityId("gas-alerts"), "Gas alerts since boot", SensorValue.Number(if (gasState == "emergency") 1.0 else 0.0)),
              Entity.BinarySensor(EntityId("valve-open"), "Valve open", gasState != "emergency"),
            ),
          ),
        ),
      ),
      Area(
        AreaId("bedroom"),
        "Bedroom",
        listOf(
          Device(
            DeviceId("closet-monitor"),
            "Closet monitor",
            listOf(
              Entity.Sensor(EntityId("temperature"), "Temperature", SensorValue.Number(21.4), "°C"),
              Entity.Sensor(EntityId("humidity"), "Humidity", SensorValue.Unavailable, "%"),
              Entity.Switch(EntityId("dehumidifier-power"), "Power", on = false, commandable = true),
              Entity.Select(EntityId("dehumidifier-mode"), "Mode", "automatic", listOf("automatic", "manual"), commandable = true),
            ),
          ),
        ),
      ),
    ),
  )

private val unreachable = ConnectionState.Disconnected(ConnectionProblem.Unreachable)

@Preview(showBackground = true)
@Composable
private fun RoomsPreview() {
  CiscTheme { HomeContent(HomeState(previewHome("normal"), ConnectionState.Connected), onOpenRoom = {}, onBack = {}, onCommand = { _, _ -> }, onCommandsInOrder = {}, onNoticeShown = {}, onRenameRoom = { _, _ -> }, onChooseRoomIcon = { _, _ -> }, onChooseLeakAlertMode = { _, _ -> }, onRenameDevice = { _, _ -> }) }
}

@Preview(showBackground = true)
@Composable
private fun RoomsInEmergencyPreview() {
  CiscTheme { HomeContent(HomeState(previewHome("emergency"), ConnectionState.Connected), onOpenRoom = {}, onBack = {}, onCommand = { _, _ -> }, onCommandsInOrder = {}, onNoticeShown = {}, onRenameRoom = { _, _ -> }, onChooseRoomIcon = { _, _ -> }, onChooseLeakAlertMode = { _, _ -> }, onRenameDevice = { _, _ -> }) }
}

@Preview(showBackground = true)
@Composable
private fun RoomsWithoutConnectionPreview() {
  CiscTheme { HomeContent(HomeState(previewHome("normal"), unreachable), onOpenRoom = {}, onBack = {}, onCommand = { _, _ -> }, onCommandsInOrder = {}, onNoticeShown = {}, onRenameRoom = { _, _ -> }, onChooseRoomIcon = { _, _ -> }, onChooseLeakAlertMode = { _, _ -> }, onRenameDevice = { _, _ -> }) }
}

@Preview(showBackground = true)
@Composable
private fun NoConnectionPreview() {
  CiscTheme { HomeContent(HomeState(null, unreachable), onOpenRoom = {}, onBack = {}, onCommand = { _, _ -> }, onCommandsInOrder = {}, onNoticeShown = {}, onRenameRoom = { _, _ -> }, onChooseRoomIcon = { _, _ -> }, onChooseLeakAlertMode = { _, _ -> }, onRenameDevice = { _, _ -> }) }
}

@Preview(showBackground = true)
@Composable
private fun RoomPreview() {
  val home = previewHome("normal")
  CiscTheme { HomeContent(HomeState(home, ConnectionState.Connected, Screen.Room(AreaId("bedroom"))), onOpenRoom = {}, onBack = {}, onCommand = { _, _ -> }, onCommandsInOrder = {}, onNoticeShown = {}, onRenameRoom = { _, _ -> }, onChooseRoomIcon = { _, _ -> }, onChooseLeakAlertMode = { _, _ -> }, onRenameDevice = { _, _ -> }) }
}
