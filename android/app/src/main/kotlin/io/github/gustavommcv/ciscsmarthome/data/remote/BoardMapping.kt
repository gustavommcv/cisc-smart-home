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
    "sensor" -> Entity.Sensor(entityId, name, sensorValue(value), unit)
    "binary_sensor" -> Entity.BinarySensor(entityId, name, flag(value))
    "switch" -> Entity.Switch(entityId, name, flag(value), commandable)
    "select" -> Entity.Select(entityId, name, text(value), options.orEmpty(), commandable)
    else -> null
  }
}

internal fun HistoryDto.toDomain(): HistoryPage =
  HistoryPage(BootId(bootId), samples.map { sample -> HistorySample(sample.uptimeMs, sample.values.mapKeys { EntityId(it.key) }) })

private fun primitive(value: JsonElement?): JsonPrimitive? = (value as? JsonPrimitive)?.takeIf { it !is JsonNull }

private fun sensorValue(value: JsonElement?): SensorValue {
  val primitive = primitive(value) ?: return SensorValue.Unavailable
  if (primitive.isString) return SensorValue.Text(primitive.content)
  return primitive.doubleOrNull?.let { SensorValue.Number(it) } ?: SensorValue.Unavailable
}

private fun flag(value: JsonElement?): Boolean? = primitive(value)?.takeIf { !it.isString }?.booleanOrNull

private fun text(value: JsonElement?): String? = primitive(value)?.takeIf { it.isString }?.content
