package io.github.gustavommcv.ciscsmarthome.domain

/** Identifies one start of the board; it changes every time the board restarts. */
@JvmInline value class BootId(val value: String)

@JvmInline value class AreaId(val value: String)

@JvmInline value class DeviceId(val value: String)

/** Unique across the whole house, so an entity can be found by it alone. */
@JvmInline value class EntityId(val value: String)

/**
 * The house as the board describes it: areas hold devices, and devices hold entities.
 * This is the model of docs/api.md, and nothing in it depends on how it reached the phone.
 */
data class Home(val bootId: BootId, val uptimeMs: Long, val areas: List<Area>) {
  /** Where an entity is, or null when the house has no entity with that id. */
  fun locate(id: EntityId): Placement? {
    for (area in areas) {
      for (device in area.devices) {
        val entity = device.entities.firstOrNull { it.id == id }
        if (entity != null) return Placement(area, device, entity)
      }
    }
    return null
  }

  fun findEntity(id: EntityId): Entity? = locate(id)?.entity
}

data class Placement(val area: Area, val device: Device, val entity: Entity)

data class Area(val id: AreaId, val name: String, val devices: List<Device>)

data class Device(val id: DeviceId, val name: String, val entities: List<Entity>)

/** One piece of information, or one control, of a device. A null value means "no valid reading". */
sealed interface Entity {
  val id: EntityId
  val name: String

  /** Whether the app may send a command to it. Only switches and selects can say yes. */
  val commandable: Boolean

  data class Sensor(override val id: EntityId, override val name: String, val value: SensorValue, val unit: String? = null) : Entity {
    override val commandable: Boolean get() = false
  }

  data class BinarySensor(override val id: EntityId, override val name: String, val value: Boolean?) : Entity {
    override val commandable: Boolean get() = false
  }

  data class Switch(override val id: EntityId, override val name: String, val on: Boolean?, override val commandable: Boolean) : Entity

  data class Select(
    override val id: EntityId,
    override val name: String,
    val selected: String?,
    val options: List<String>,
    override val commandable: Boolean,
  ) : Entity
}

/** What a sensor reads: a number, a text, or nothing valid. */
sealed interface SensorValue {
  data object Unavailable : SensorValue

  data class Number(val value: Double) : SensorValue

  data class Text(val value: String) : SensorValue
}
