package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.domain.StateReading
import java.time.Duration
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/** When something was on, as stretches of the window: from the first sample that said so to the moment a sample said otherwise. */
class StateSpansTest {
  // A window of 100 minutes, so a minute is 0.01 of it and the numbers read as percents.
  private val since = Instant.parse("2026-10-27T10:00:00Z")
  private val until = since.plus(Duration.ofMinutes(100))
  private val maxGap = Duration.ofMinutes(2)

  private fun at(minute: Long, state: String) = StateReading(since.plus(Duration.ofMinutes(minute)), state)

  private fun spans(vararg states: StateReading, active: String = "on") = spansOf(states.toList(), active, since, until, maxGap)

  private fun span(fromMinute: Int, toMinute: Int) = StateSpan(fromMinute / 100f, toMinute / 100f)

  @Test
  fun `a run of samples that say it was on ends where a sample says it was not`() {
    assertEquals(listOf(span(10, 12)), spans(at(10, "on"), at(11, "on"), at(12, "off")))
  }

  @Test
  fun `a run that nothing follows ends at its last sample`() {
    assertEquals(listOf(span(10, 11)), spans(at(10, "on"), at(11, "on")))
  }

  @Test
  fun `a single sample is a span with no width, for the screen to give a width of its own`() {
    assertEquals(listOf(span(10, 10)), spans(at(10, "on")))
  }

  @Test
  fun `two runs with a stretch of off between them are two spans`() {
    assertEquals(listOf(span(10, 11), span(20, 21)), spans(at(10, "on"), at(11, "off"), at(20, "on"), at(21, "off")))
  }

  @Test
  fun `samples that never say it was on have no span`() {
    assertEquals(emptyList<StateSpan>(), spans(at(10, "off"), at(11, "off")))
    assertEquals(emptyList<StateSpan>(), spans())
  }

  @Test
  fun `a silence longer than the gap is not drawn as on, and cuts the run in two`() {
    assertEquals(listOf(span(10, 11), span(30, 31)), spans(at(10, "on"), at(11, "on"), at(30, "on"), at(31, "on")))
  }

  @Test
  fun `a run that ends before a silence ends at its last sample and not across the silence`() {
    assertEquals(listOf(span(10, 11)), spans(at(10, "on"), at(11, "on"), at(40, "off")))
  }

  @Test
  fun `a silence of exactly the gap does not cut the run`() {
    assertEquals(listOf(span(10, 12)), spans(at(10, "on"), at(12, "on")))
  }

  @Test
  fun `the order of the samples does not matter`() {
    assertEquals(listOf(span(10, 12)), spans(at(12, "off"), at(10, "on"), at(11, "on")))
  }

  @Test
  fun `only the state asked for counts`() {
    assertEquals(listOf(span(10, 11)), spans(at(10, "emergency"), at(11, "normal"), active = "emergency"))
    assertEquals(emptyList<StateSpan>(), spans(at(10, "normal"), at(11, "normal"), active = "emergency"))
  }

  @Test
  fun `a span is cut to the window`() {
    val states = listOf(StateReading(since.minus(Duration.ofMinutes(1)), "on"), at(0, "on"), at(99, "on"), StateReading(until.plus(Duration.ofMinutes(1)), "on"))
    assertEquals(listOf(span(0, 100)), spansOf(states, "on", since, until, Duration.ofMinutes(200)))
  }

  @Test
  fun `a state outside the window is not drawn`() {
    val states = listOf(StateReading(since.minus(Duration.ofMinutes(10)), "on"), StateReading(until.plus(Duration.ofMinutes(10)), "on"))
    assertEquals(emptyList<StateSpan>(), spansOf(states, "on", since, until, maxGap))
  }
}
