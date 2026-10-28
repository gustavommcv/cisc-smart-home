package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import java.time.Instant

/** A band of a chart: the line of one [sensor], or null when it read nothing in the window. */
internal data class Lane(val sensor: ChartSensor, val chart: Chart?)

/** A chart of the screen: the bands of its [spec], one above the other. Each has the scale of its own sensor and all of them the same window. */
internal data class ChartPanel(val spec: ChartSpec, val lanes: List<Lane>) {
  /** Nothing to draw in any of the bands. */
  val isEmpty: Boolean get() = lanes.all { it.chart == null }
}

/** The chart of [spec] made of the [readings] of its sensors, in the window from [since] to [until]. */
internal fun panelOf(spec: ChartSpec, readings: Map<EntityId, List<Reading>>, since: Instant, until: Instant): ChartPanel =
  ChartPanel(spec, spec.sensors.map { sensor -> Lane(sensor, chartOf(readings[sensor.id].orEmpty(), since, until)) })
