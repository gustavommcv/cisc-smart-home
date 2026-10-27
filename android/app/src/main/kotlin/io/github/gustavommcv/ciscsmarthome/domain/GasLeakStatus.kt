package io.github.gustavommcv.ciscsmarthome.domain

/** What the house says about gas leaks: how many there were since the board started, and whether one is happening now. */
data class GasLeakStatus(val areaId: AreaId, val alertCount: Long, val active: Boolean)

private val GAS_STATE = EntityId("gas-state")
private val GAS_ALERTS = EntityId("gas-alerts")
private const val EMERGENCY = "emergency"

/**
 * The leak status of this house (the entities of "As entidades da maquete" in docs/api.md), or null
 * when the house cannot say: no gas state, or a count of alerts that is missing or cannot be read.
 * Without the count a leak cannot be told apart from one already announced.
 */
fun Home.gasLeakStatus(): GasLeakStatus? {
  val state = locate(GAS_STATE)?.takeIf { it.entity is Entity.Sensor } ?: return null
  val count = ((findEntity(GAS_ALERTS) as? Entity.Sensor)?.value as? SensorValue.Number)?.value?.toLong() ?: return null
  val active = ((state.entity as Entity.Sensor).value as? SensorValue.Text)?.value == EMERGENCY
  return GasLeakStatus(state.area.id, count, active)
}
