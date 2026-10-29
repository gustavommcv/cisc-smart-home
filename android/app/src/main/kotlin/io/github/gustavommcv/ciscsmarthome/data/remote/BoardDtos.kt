package io.github.gustavommcv.ciscsmarthome.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/** The JSON of docs/api.md, as it travels. They are mapped to the domain before anything else sees them. */
internal val BoardJson = Json { ignoreUnknownKeys = true }

@Serializable
internal data class HomeDto(val apiVersion: Int, val bootId: String, val uptimeMs: Long, val areas: List<AreaDto>)

@Serializable internal data class AreaDto(val id: String, val name: String, val devices: List<DeviceDto>)

@Serializable internal data class DeviceDto(val id: String, val name: String, val entities: List<EntityDto>)

@Serializable
internal data class EntityDto(
  val id: String,
  val kind: String,
  val name: String,
  // A number, a text, a boolean or null, depending on the kind.
  val value: JsonElement? = null,
  val unit: String? = null,
  val options: List<String>? = null,
  // What a number says about itself.
  val min: Double? = null,
  val max: Double? = null,
  val step: Double? = null,
  val default: Double? = null,
  val commandable: Boolean = false,
  // What a sensor says it is and the lines to draw over its chart (docs/api.md); a board that does not say, and an app that does not know, do without.
  @SerialName("class") val sensorClass: String? = null,
  val limits: List<LimitDto>? = null,
)

/** A line over the chart of a sensor: the value it has, or the entity whose value it is. */
@Serializable internal data class LimitDto(val kind: String, val value: Double? = null, val entity: String? = null)

@Serializable internal data class HistoryDto(val bootId: String, val samples: List<SampleDto>)

@Serializable
internal data class SampleDto(val uptimeMs: Long, val values: Map<String, Double?>, val states: Map<String, JsonPrimitive> = emptyMap())

@Serializable internal data class AlertDto(val type: String, val areaId: String, val entityId: String, val count: Long, val uptimeMs: Long)

@Serializable internal data class ErrorEnvelopeDto(val error: ErrorDto)

@Serializable internal data class ErrorDto(val code: String, val message: String = "")
