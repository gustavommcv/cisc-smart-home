package io.github.gustavommcv.ciscsmarthome.ui.home

import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.gasLeakStatus

// What matters most about a room, in the order it is shown. A card of the list has room for few things.
private val HIGHLIGHTS = listOf("gas-state", "gas-level", "temperature", "humidity", "dehumidifier-power").map(::EntityId)
private const val MOST_ON_A_CARD = 3

/**
 * What a card says about this room: the things of the maquete that matter, in that order, and for a room
 * made of things the app does not know, the first it has.
 */
internal fun Area.highlights(): List<Entity> {
  val all = devices.flatMap { it.entities }
  val known = HIGHLIGHTS.mapNotNull { id -> all.firstOrNull { it.id == id } }
  return known.ifEmpty { all }.take(MOST_ON_A_CARD)
}

/** Whether [area] is the room where the board says gas is leaking now. */
internal fun Home.isInEmergency(area: Area): Boolean = gasLeakStatus()?.let { it.active && it.areaId == area.id } == true
