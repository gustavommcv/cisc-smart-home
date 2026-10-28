package io.github.gustavommcv.ciscsmarthome.ui.history

import io.github.gustavommcv.ciscsmarthome.domain.HourlyReading
import io.github.gustavommcv.ciscsmarthome.domain.Reading
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

/** An hour that the app kept is drawn as two points, its least and its greatest, so a peak is never smoothed away. */
class HourlyPointsTest {
  private val hour = Instant.parse("2026-10-27T10:00:00Z")

  @Test
  fun `an hour is its least at the start and its greatest in the middle`() {
    assertEquals(listOf(Reading(hour, 5.0), Reading(hour.plusSeconds(1_800), 9.0)), HourlyReading(hour, 5.0, 9.0).points())
  }

  @Test
  fun `an hour that never changed is two points of the same value`() {
    assertEquals(listOf(Reading(hour, 7.0), Reading(hour.plusSeconds(1_800), 7.0)), HourlyReading(hour, 7.0, 7.0).points())
  }

  @Test
  fun `hours are points in the order of the hours`() {
    val next = hour.plusSeconds(3_600)
    val points = listOf(HourlyReading(hour, 1.0, 2.0), HourlyReading(next, 3.0, 4.0)).points()
    assertEquals(listOf(hour, hour.plusSeconds(1_800), next, next.plusSeconds(1_800)), points.map { it.at })
    assertEquals(listOf(1.0, 2.0, 3.0, 4.0), points.map { it.value })
  }

  @Test
  fun `no hours are no points`() {
    assertEquals(emptyList<Reading>(), emptyList<HourlyReading>().points())
  }

  @Test
  fun `the silence that breaks a line of hours is longer than the one inside an hour and than the one between two hours that follow each other`() {
    assertEquals(true, ChartRules.HourlyMaxGap.seconds >= 1_800)
    assertEquals(true, ChartRules.HourlyMaxGap.seconds < 5_400)
  }
}
