package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.domain.StateKind
import io.github.gustavommcv.ciscsmarthome.domain.StateReading
import java.time.Duration
import java.time.Instant

/** A stretch of the window in which something was on, from [from] to [to], each from 0 (the start of the window) to 1 (its end). It may have no width: one sample. */
internal data class StateSpan(val from: Float, val to: Float)

/** The time a sensor was in a state the board says, as the screen draws it: what it is ([kind]) and when ([spans]). */
internal data class StateBand(val kind: StateKind, val spans: List<StateSpan>)

/**
 * The stretches of the window in which the state was [active]. A stretch starts at the first sample that says so and ends at the sample that says otherwise,
 * which is when the state changed; if nothing follows it, or what follows comes after more than [maxGap] of silence, it ends at its last sample, so the time
 * the app did not hear the board is not said to be on. A silence in the middle of a run cuts it in two. The stretches are cut to the window.
 */
internal fun spansOf(states: List<StateReading>, active: String, since: Instant, until: Instant, maxGap: Duration): List<StateSpan> {
  val samples = states.sortedBy { it.at }
  val window = Duration.between(since, until).toMillis().coerceAtLeast(1)
  fun fractionOf(at: Instant) = (Duration.between(since, at).toMillis().toFloat() / window).coerceIn(0f, 1f)
  fun silent(before: StateReading, after: StateReading) = Duration.between(before.at, after.at) > maxGap
  val spans = mutableListOf<StateSpan>()
  var index = 0
  while (index < samples.size) {
    if (samples[index].state != active) {
      index += 1
      continue
    }
    var last = index
    while (last + 1 < samples.size && samples[last + 1].state == active && !silent(samples[last], samples[last + 1])) last += 1
    val next = samples.getOrNull(last + 1)
    val end = if (next != null && !silent(samples[last], next)) next.at else samples[last].at
    val start = samples[index].at
    // What is wholly before or after the window is not in it.
    if (!end.isBefore(since) && !start.isAfter(until)) spans += StateSpan(fractionOf(start), fractionOf(end))
    index = last + 1
  }
  return spans
}
