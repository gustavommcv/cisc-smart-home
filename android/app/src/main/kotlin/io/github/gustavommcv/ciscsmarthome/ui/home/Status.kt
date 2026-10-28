package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.annotation.DrawableRes
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityIds
import io.github.gustavommcv.ciscsmarthome.domain.GasStates
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.SensorValue
import io.github.gustavommcv.ciscsmarthome.ui.components.Tone

/**
 * How a room, or the house, is, from the worst to the best. Fine is said only when it is known: a gas sensor that is
 * warming up, or a reading that did not arrive, is never "all is well".
 */
internal sealed interface Status {
  /** The board says gas is leaking. */
  data object Emergency : Status

  /** The gas sensor is warming up, so no leak can be told yet. */
  data object Warming : Status

  /** [count] readings did not arrive (or the gas sensor said something the app does not understand). */
  data class Unavailable(val count: Int) : Status

  data object Fine : Status
}

/** How this room is. A leak is told by the state of the gas alone: it does not wait for the count of leaks the alarm needs. */
internal fun Area.status(): Status {
  val entities = devices.flatMap { it.entities }
  val gas = ((entities.firstOrNull { it.id == EntityIds.GasState } as? Entity.Sensor)?.value as? SensorValue.Text)?.value
  return when (gas) {
    GasStates.Emergency -> Status.Emergency
    GasStates.Starting -> Status.Warming
    else -> entities.count { !it.hasReading() }.let { missing -> if (missing == 0) Status.Fine else Status.Unavailable(missing) }
  }
}

/** How the house is: as bad as its worst room, and the readings missing in all of them added up. */
internal fun Home.status(): Status {
  val rooms = areas.map { it.status() }
  return when {
    rooms.any { it == Status.Emergency } -> Status.Emergency
    rooms.any { it == Status.Warming } -> Status.Warming
    else -> rooms.filterIsInstance<Status.Unavailable>().sumOf { it.count }.let { missing -> if (missing == 0) Status.Fine else Status.Unavailable(missing) }
  }
}

/** The room where gas is leaking, or null when there is none. */
internal fun Home.emergencyArea(): Area? = areas.firstOrNull { it.status() == Status.Emergency }

internal fun Status.tone(): Tone =
  when (this) {
    Status.Fine -> Tone.Primary
    Status.Emergency -> Tone.Alert
    Status.Warming, is Status.Unavailable -> Tone.Neutral
  }

/** Only what is fine gets the reassuring icon; everything else is a warning, in its own tone. */
@DrawableRes
internal fun Status.iconRes(): Int = if (this == Status.Fine) R.drawable.ic_verified_user else R.drawable.ic_warning

// The gas state is a reading only when it says something the app understands: "normal", here, since the others were told apart before.
private fun Entity.hasReading(): Boolean =
  when (this) {
    is Entity.Sensor -> if (id == EntityIds.GasState) (value as? SensorValue.Text)?.value == GasStates.Normal else value !is SensorValue.Unavailable
    is Entity.BinarySensor -> value != null
    is Entity.Switch -> on != null
    is Entity.Select -> selected != null
  }
