package io.github.gustavommcv.ciscsmarthome.domain

// The ids and words of docs/api.md that the app itself has to know. They are written here, once, and the rest of
// the code names them instead of spelling them; ContractTest keeps them equal to the document.

/** The entities of the maquete ("As entidades da maquete"). */
object EntityIds {
  val GasLevel = EntityId("gas-level")
  val GasState = EntityId("gas-state")
  val GasAlerts = EntityId("gas-alerts")
  val ValveOpen = EntityId("valve-open")
  val Temperature = EntityId("temperature")
  val Humidity = EntityId("humidity")
  val DehumidifierPower = EntityId("dehumidifier-power")
  val DehumidifierMode = EntityId("dehumidifier-mode")
  val DehumidifierOnAt = EntityId("dehumidifier-on-at")
  val DehumidifierOffAt = EntityId("dehumidifier-off-at")
}

object AreaIds {
  val Kitchen = AreaId("kitchen")
  val Bedroom = AreaId("bedroom")
}

/** The devices of the maquete. A device is a unit someone would buy and install, and it has the entities it senses and controls. */
object DeviceIds {
  val GasMonitor = DeviceId("gas-monitor")
  val ClosetMonitor = DeviceId("closet-monitor")
}

/** What `gas-state` says. */
object GasStates {
  const val Starting = "starting"
  const val Normal = "normal"
  const val Emergency = "emergency"
}

/** What `dehumidifier-mode` says and takes. */
object DehumidifierModes {
  const val Automatic = "automatic"
  const val Manual = "manual"
}

/** The `type` of an `alert` event of the stream. */
object AlertTypes {
  const val GasLeak = "gas-leak"
}
