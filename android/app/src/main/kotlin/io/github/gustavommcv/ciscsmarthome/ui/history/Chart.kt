package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.domain.Reading
import java.time.Duration
import java.time.Instant

/** A point of the chart in the box it is drawn in: [x] from 0 (the start of the window) to 1 (its end), [y] from 0 (the least) to 1 (the greatest), up. */
internal data class ChartPoint(val x: Float, val y: Float)

/** What the screen draws: its lines (a line breaks where the app heard nothing), and the least and the greatest value that was read. */
internal data class Chart(val lines: List<List<ChartPoint>>, val min: Double, val max: Double)

internal object ChartRules {
  /**
   * The board makes a reading every ten seconds and the app asks for what it missed, so a longer silence is time the app did not hear the
   * board: the line breaks there, and the empty time stays empty, instead of a straight line that says nothing happened.
   */
  val MaxGap: Duration = Duration.ofMinutes(2)

  /** A day of readings is 8 640 points, more than a screen has pixels to tell apart: each stretch is cut in this many parts. */
  const val Buckets = 150
}

private data class Sample(val at: Instant, val value: Double)

/**
 * The chart of [readings] in the window from [since] to [until], or null when there is nothing valid to draw. A reading with no value is
 * time without a reading. A stretch with more readings than the screen can show is reduced keeping, for each part of it, the least and the
 * greatest, so a peak (a leak lasts seconds) is never smoothed away.
 */
internal fun chartOf(readings: List<Reading>, since: Instant, until: Instant): Chart? {
  val samples =
    readings
      .mapNotNull { reading -> reading.value?.takeIf { !reading.at.isBefore(since) && !reading.at.isAfter(until) }?.let { Sample(reading.at, it) } }
      .sortedBy { it.at }
  if (samples.isEmpty()) return null
  val min = samples.minOf { it.value }
  val max = samples.maxOf { it.value }
  val window = Duration.between(since, until).toMillis().coerceAtLeast(1)
  val lines = stretches(samples).map { stretch -> reduced(stretch, since, window).map { point(it, since, window, min, max) } }
  return Chart(lines, min, max)
}

/** The samples in runs with no silence longer than [ChartRules.MaxGap] inside. */
private fun stretches(samples: List<Sample>): List<List<Sample>> {
  val runs = mutableListOf(mutableListOf(samples.first()))
  samples.zipWithNext().forEach { (before, after) ->
    if (Duration.between(before.at, after.at) > ChartRules.MaxGap) runs += mutableListOf(after) else runs.last() += after
  }
  return runs
}

private fun reduced(stretch: List<Sample>, since: Instant, windowMillis: Long): List<Sample> {
  if (stretch.size <= 2 * ChartRules.Buckets) return stretch
  return stretch
    .groupBy { sample -> (Duration.between(since, sample.at).toMillis() * ChartRules.Buckets / windowMillis).toInt().coerceIn(0, ChartRules.Buckets - 1) }
    .toSortedMap()
    .values
    .flatMap { bucket -> listOf(bucket.minBy { it.value }, bucket.maxBy { it.value }).distinct().sortedBy { it.at } }
}

private fun point(sample: Sample, since: Instant, windowMillis: Long, min: Double, max: Double): ChartPoint {
  val x = Duration.between(since, sample.at).toMillis().toFloat() / windowMillis
  // A series that never changes has no scale to draw: it goes through the middle.
  val y = if (max == min) 0.5f else ((sample.value - min) / (max - min)).toFloat()
  return ChartPoint(x, y)
}
