package io.github.gustavommcv.ciscsmarthome.domain

/** What the house says about gas leaks: how many there were since the board started, and whether one is happening now. */
data class GasLeakStatus(val areaId: AreaId, val alertCount: Long, val active: Boolean)

/**
 * The leak status of this house (the entities of "As entidades da maquete" in docs/api.md), or null
 * when the house cannot say: no gas state, or a count of alerts that is missing or cannot be read.
 * Without the count a leak cannot be told apart from one already announced.
 */
fun Home.gasLeakStatus(): GasLeakStatus? {
  val state = locate(EntityIds.GasState)?.takeIf { it.entity is Entity.Sensor } ?: return null
  val count = ((findEntity(EntityIds.GasAlerts) as? Entity.Sensor)?.value as? SensorValue.Number)?.value?.toLong() ?: return null
  val active = ((state.entity as Entity.Sensor).value as? SensorValue.Text)?.value == GasStates.Emergency
  return GasLeakStatus(state.area.id, count, active)
}

/**
 * Whether the board says this leak is over: a home of the same boot that is not in emergency and whose
 * count has reached this leak's. A home from before the leak (a lower count) says nothing about it, and
 * neither does one from another boot, which is a board that restarted and has to find the gas again.
 */
fun GasAlert.isOverIn(home: Home): Boolean {
  if (home.bootId != bootId) return false
  val status = home.gasLeakStatus() ?: return false
  return !status.active && status.alertCount >= count
}
