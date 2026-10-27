package io.github.gustavommcv.ciscsmarthome.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeviceClockTest {
  private val boot = BootId("5f3a9c21")
  private val other = BootId("deadbeef")

  private fun at(seconds: Long): Instant = Instant.ofEpochSecond(seconds)

  @Test
  fun `a boot never heard from has no time`() {
    assertNull(DeviceClock().toInstant(boot, uptimeMs = 5_000))
  }

  @Test
  fun `the moment of a response is the uptime it carried`() {
    val clock = DeviceClock()
    clock.observe(boot, uptimeMs = 183_450, receivedAt = at(1_000_000))
    assertEquals(at(1_000_000), clock.toInstant(boot, 183_450))
  }

  @Test
  fun `a sample is placed relative to the response, before or after it`() {
    val clock = DeviceClock()
    clock.observe(boot, uptimeMs = 100_000, receivedAt = at(1_000_000))
    assertEquals(at(999_940), clock.toInstant(boot, 40_000))
    assertEquals(at(1_000_030), clock.toInstant(boot, 130_000))
  }

  @Test
  fun `the latest response sets the offset`() {
    val clock = DeviceClock()
    clock.observe(boot, uptimeMs = 10_000, receivedAt = at(1_000))  // offset 990 s
    clock.observe(boot, uptimeMs = 70_000, receivedAt = at(1_061))  // offset 991 s: the phone's clock drifted by 1 s
    assertEquals(at(1_061), clock.toInstant(boot, 70_000))
    assertEquals(at(1_001), clock.toInstant(boot, 10_000))  // the new offset applies to every uptime
  }

  @Test
  fun `each boot has its own offset`() {
    val clock = DeviceClock()
    clock.observe(boot, uptimeMs = 100_000, receivedAt = at(2_000))
    clock.observe(other, uptimeMs = 5_000, receivedAt = at(9_000))
    assertEquals(at(2_000), clock.toInstant(boot, 100_000))
    assertEquals(at(9_000), clock.toInstant(other, 5_000))
  }

  @Test
  fun `a boot restarted with the same id after the uptime counter wrapped is followed from the new response`() {
    val clock = DeviceClock()
    clock.observe(boot, uptimeMs = 4_294_000_000, receivedAt = at(5_000_000))
    clock.observe(boot, uptimeMs = 3_000, receivedAt = at(5_000_700))  // the counter went back to zero
    assertEquals(at(5_000_700), clock.toInstant(boot, 3_000))
  }
}
