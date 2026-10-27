package io.github.gustavommcv.ciscsmarthome.testing

import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.Device
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.SensorValue

/**
 * A house like the board's, for the tests: the kitchen with its gas sensor and, when asked, the bedroom with the
 * switch of the dehumidifier. The ids are spelled out here, as the board sends them, and not taken from the
 * constants of the app, so a mistyped constant cannot make a test agree with it.
 */
fun houseOfTheMaquete(
  boot: BootId = BootId("aaaa1111"),
  uptimeMs: Long = 1_000,
  gasState: String = "normal",
  gasAlerts: Long = 0,
  withBedroom: Boolean = false,
): Home {
  val kitchen =
    Area(
      AreaId("kitchen"),
      "Kitchen",
      listOf(
        Device(
          DeviceId("gas-sensor"),
          "Gas sensor",
          listOf(
            Entity.Sensor(EntityId("gas-state"), "Gas state", SensorValue.Text(gasState)),
            Entity.Sensor(EntityId("gas-alerts"), "Gas alerts", SensorValue.Number(gasAlerts.toDouble())),
          ),
        ),
      ),
    )
  val bedroom =
    Area(
      AreaId("bedroom"),
      "Bedroom",
      listOf(Device(DeviceId("dehumidifier"), "Dehumidifier", listOf(Entity.Switch(EntityId("dehumidifier-power"), "Power", on = false, commandable = true)))),
    )
  return Home(boot, uptimeMs, if (withBedroom) listOf(kitchen, bedroom) else listOf(kitchen))
}
