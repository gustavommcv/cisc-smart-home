package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.HourlyReading
import io.github.gustavommcv.ciscsmarthome.domain.LimitKind
import io.github.gustavommcv.ciscsmarthome.domain.LimitSpec
import io.github.gustavommcv.ciscsmarthome.domain.LongPeriods
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** A line over a chart: what it is and the value it has now. */
internal data class ResolvedLimit(val kind: LimitKind, val value: Double)

/**
 * A band of a chart: what a [sensor] read, drawn as a line ([chart]) or, for a month or a year of the gas, as a bar for each day ([bars]); null when it read
 * nothing in the window. The [limits] are the lines drawn over it.
 */
internal data class Lane(val sensor: ChartSensor, val chart: Chart?, val limits: List<ResolvedLimit> = emptyList(), val bars: Bars? = null)

/** A chart of the screen: the bands of its [spec], one above the other. Each has the scale of its own sensor and all of them the same window. */
internal data class ChartPanel(val spec: ChartSpec, val lanes: List<Lane>) {
  /** Nothing to draw in any of the bands. */
  val isEmpty: Boolean get() = lanes.all { it.chart == null && it.bars == null }
}

/**
 * The chart of [spec] made of the [readings] of its sensors, in the window from [since] to [until], a line breaking where nothing was heard for longer
 * than [maxGap]. Each band is drawn as the catalog says of its sensor: on its scale, with its limits ([limitValues] are the values the house has now of the
 * entities the limits are read from), and, when the [hours] are given (a month or a year) and the sensor is drawn by the day, as bars of the days of [zone].
 */
internal fun panelOf(
  spec: ChartSpec,
  readings: Map<EntityId, List<Reading>>,
  since: Instant,
  until: Instant,
  maxGap: Duration = ChartRules.MaxGap,
  limitValues: Map<EntityId, Double> = emptyMap(),
  hours: Map<EntityId, List<HourlyReading>> = emptyMap(),
  zone: ZoneId = ZoneOffset.UTC,
): ChartPanel =
  ChartPanel(
    spec,
    spec.sensors.map { sensor ->
      val profile = sensor.profile
      val limits =
        profile.limits.mapNotNull { limit ->
          when (limit) {
            is LimitSpec.Fixed -> ResolvedLimit(limit.kind, limit.value)
            is LimitSpec.OfEntity -> limitValues[limit.entity]?.let { ResolvedLimit(limit.kind, it) }
          }
        }
      val values = limits.map { it.value }
      val ofTheHours = hours[sensor.id]
      if (profile.longPeriods == LongPeriods.DailyPeaks && ofTheHours != null) {
        Lane(sensor, null, limits, barsOf(ofTheHours, since, until, zone, profile.scale, values))
      } else {
        Lane(sensor, chartOf(readings[sensor.id].orEmpty(), since, until, maxGap, profile.scale, values), limits)
      }
    },
  )
