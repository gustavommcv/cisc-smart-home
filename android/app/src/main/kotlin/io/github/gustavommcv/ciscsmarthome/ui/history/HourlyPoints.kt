package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.domain.HourlyReading
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import java.time.Duration

// Where inside its hour the greatest is drawn: the app does not know when in the hour it was read, and any place in it is as true as another.
private val MiddleOfTheHour: Duration = Duration.ofMinutes(30)

/** An hour that was kept as the points a chart draws: its least where it starts and its greatest in the middle, so neither is smoothed away. */
internal fun HourlyReading.points(): List<Reading> = listOf(Reading(hour, lowest), Reading(hour.plus(MiddleOfTheHour), highest))

internal fun List<HourlyReading>.points(): List<Reading> = flatMap { it.points() }
