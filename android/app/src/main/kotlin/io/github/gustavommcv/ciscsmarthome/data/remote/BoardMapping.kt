package io.github.gustavommcv.ciscsmarthome.data.remote

import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.Device
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.HistoryPage
import io.github.gustavommcv.ciscsmarthome.domain.HistorySample
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.LimitKind
import io.github.gustavommcv.ciscsmarthome.domain.LimitSpec
import io.github.gustavommcv.ciscsmarthome.domain.SensorKind
import io.github.gustavommcv.ciscsmarthome.domain.SensorValue
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

// The app is tolerant, as the contract asks: an entity of a kind it does not know is left out, and a
// value that does not fit its kind counts as "no valid reading" instead of breaking the whole home.

internal fun HomeDto.toDomain(): Home = Home(BootId(bootId), uptimeMs, areas.map { it.toDomain() })

private fun AreaDto.toDomain(): Area =
  Area(AreaId(id), name, devices.map { device -> Device(DeviceId(device.id), device.name, device.entities.mapNotNull { it.toDomain() }) })

/** The entity, or null when its kind is not one the app knows. */
internal fun EntityDto.toDomain(): Entity? {
  val entityId = EntityId(id)
  return when (kind) {
    "sensor" -> Entity.Sensor(entityId, name, sensorValue(value), unit, sensorKind(sensorClass), limits.orEmpty().mapNotNull { it.toDomain() })
    "binary_sensor" -> Entity.BinarySensor(entityId, name, flag(value))
    "switch" -> Entity.Switch(entityId, name, flag(value), commandable)
    "select" -> Entity.Select(entityId, name, text(value), options.orEmpty(), commandable)
    // A number the board gave no range or step for cannot be drawn, so it is left out like a kind the app does not know.
    "number" -> numberEntity(entityId)
    else -> null
  }
}

// A class or a kind of limit the app does not know is left out, and the sensor is drawn by what the app knows of it.
private fun sensorKind(name: String?): SensorKind? =
  when (name) {
    "gas" -> SensorKind.Gas
    "temperature" -> SensorKind.Temperature
    "humidity" -> SensorKind.Humidity
    else -> null
  }

private fun LimitDto.toDomain(): LimitSpec? {
  val limitKind =
    when (kind) {
      "alarm" -> LimitKind.Alarm
      "turns-on" -> LimitKind.DehumidifierOn
      "turns-off" -> LimitKind.DehumidifierOff
      else -> return null
    }
  return when {
    value != null -> LimitSpec.Fixed(limitKind, value)
    entity != null -> LimitSpec.OfEntity(limitKind, EntityId(entity))
    else -> null
  }
}

private fun EntityDto.numberEntity(entityId: EntityId): Entity? {
  val min = min ?: return null
  val max = max ?: return null
  val step = step?.takeIf { it > 0.0 } ?: return null
  return Entity.Number(entityId, name, number(value), unit, min, max, step, default, commandable)
}

internal fun HistoryDto.toDomain(): HistoryPage =
  HistoryPage(
    BootId(bootId),
    samples.map { sample ->
      HistorySample(
        sample.uptimeMs,
        sample.values.mapKeys { EntityId(it.key) },
        sample.states.filterValues { it !is JsonNull }.map { (id, state) -> EntityId(id) to state.content }.toMap(),
      )
    },
  )

private fun primitive(value: JsonElement?): JsonPrimitive? = (value as? JsonPrimitive)?.takeIf { it !is JsonNull }

private fun sensorValue(value: JsonElement?): SensorValue {
  val primitive = primitive(value) ?: return SensorValue.Unavailable
  if (primitive.isString) return SensorValue.Text(primitive.content)
  return primitive.doubleOrNull?.let { SensorValue.Number(it) } ?: SensorValue.Unavailable
}

private fun flag(value: JsonElement?): Boolean? = primitive(value)?.takeIf { !it.isString }?.booleanOrNull

private fun number(value: JsonElement?): Double? = primitive(value)?.takeIf { !it.isString }?.doubleOrNull

private fun text(value: JsonElement?): String? = primitive(value)?.takeIf { it.isString }?.content
