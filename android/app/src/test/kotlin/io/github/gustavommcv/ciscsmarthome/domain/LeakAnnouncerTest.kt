package io.github.gustavommcv.ciscsmarthome.domain

import io.github.gustavommcv.ciscsmarthome.testing.FakeAlertMemory
import io.github.gustavommcv.ciscsmarthome.testing.houseOfTheMaquete
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** Turning what the board says about gas leaks into news, once each, and remembering what was said. */
@OptIn(ExperimentalCoroutinesApi::class)
class LeakAnnouncerTest {
  private val memory = FakeAlertMemory()

  private fun announcer(memory: FakeAlertMemory = this.memory) = LeakAnnouncer(memory)

  private fun TestScope.collect(announcer: LeakAnnouncer): List<GasAlert> {
    val alerts = mutableListOf<GasAlert>()
    backgroundScope.launch { announcer.alerts.toList(alerts) }
    runCurrent()
    return alerts
  }

  private fun leak(count: Long, boot: BootId = A) = HomeEvent.GasLeak(boot, KITCHEN, EntityId("gas-state"), count, uptimeMs = 120_000)

  private fun house(boot: BootId = A, state: String = "normal", alerts: Long = 0) = houseOfTheMaquete(boot, uptimeMs = 120_000, gasState = state, gasAlerts = alerts)

  @Test
  fun `a leak event is news the first time`() = runTest {
    val announcer = announcer()
    val alerts = collect(announcer)
    announcer.onLeakEvent(leak(count = 1))
    runCurrent()
    assertEquals(listOf(GasAlert(A, KITCHEN, 1)), alerts)
  }

  @Test
  fun `the same leak event again is not news`() = runTest {
    val announcer = announcer()
    val alerts = collect(announcer)
    announcer.onLeakEvent(leak(count = 1))
    announcer.onLeakEvent(leak(count = 1))
    runCurrent()
    assertEquals(1, alerts.size)
  }

  @Test
  fun `a second leak is news of its own`() = runTest {
    val announcer = announcer()
    val alerts = collect(announcer)
    announcer.onLeakEvent(leak(count = 1))
    announcer.onLeakEvent(leak(count = 2))
    runCurrent()
    assertEquals(listOf(GasAlert(A, KITCHEN, 1), GasAlert(A, KITCHEN, 2)), alerts)
  }

  @Test
  fun `the first leak of another boot is news`() = runTest {
    val announcer = announcer()
    val alerts = collect(announcer)
    announcer.onLeakEvent(leak(count = 1))
    announcer.onLeakEvent(leak(count = 1, boot = B))
    runCurrent()
    assertEquals(listOf(GasAlert(A, KITCHEN, 1), GasAlert(B, KITCHEN, 1)), alerts)
  }

  @Test
  fun `a leak that is active in the home is news even if no event came`() = runTest {
    val announcer = announcer()
    val alerts = collect(announcer)
    announcer.onHome(house(state = "emergency", alerts = 1))
    runCurrent()
    assertEquals(listOf(GasAlert(A, KITCHEN, 1)), alerts)
  }

  @Test
  fun `the same leak is not news again when the home says it after the event`() = runTest {
    val announcer = announcer()
    val alerts = collect(announcer)
    announcer.onLeakEvent(leak(count = 1))
    announcer.onHome(house(state = "emergency", alerts = 1))
    runCurrent()
    assertEquals(1, alerts.size)
  }

  @Test
  fun `the same leak is not news again when the event comes after the home`() = runTest {
    val announcer = announcer()
    val alerts = collect(announcer)
    announcer.onHome(house(state = "emergency", alerts = 1))
    announcer.onLeakEvent(leak(count = 1))
    runCurrent()
    assertEquals(1, alerts.size)
  }

  @Test
  fun `the home saying the same active leak again is not news again`() = runTest {
    val announcer = announcer()
    val alerts = collect(announcer)
    announcer.onHome(house(state = "emergency", alerts = 1))
    announcer.onHome(house(state = "emergency", alerts = 1))
    runCurrent()
    assertEquals(1, alerts.size)
  }

  @Test
  fun `leaks that already ended in the home are not news`() = runTest {
    val announcer = announcer()
    val alerts = collect(announcer)
    announcer.onHome(house(state = "normal", alerts = 4))
    runCurrent()
    assertEquals(emptyList<GasAlert>(), alerts)
  }

  @Test
  fun `a home that cannot say about gas is not news and is not remembered`() = runTest {
    val announcer = announcer()
    val alerts = collect(announcer)
    announcer.onHome(Home(A, uptimeMs = 120_000, areas = emptyList()))
    runCurrent()
    assertEquals(emptyList<GasAlert>(), alerts)
    assertEquals(0, memory.saves)
  }

  @Test
  fun `a leak that was announced before the app was closed stays quiet`() = runTest {
    val announcer = announcer(FakeAlertMemory(mapOf(A to 2L)))
    val alerts = collect(announcer)
    announcer.onLeakEvent(leak(count = 2))
    announcer.onHome(house(state = "emergency", alerts = 2))
    runCurrent()
    assertEquals(emptyList<GasAlert>(), alerts)
  }

  @Test
  fun `a leak after the ones announced before is news`() = runTest {
    val announcer = announcer(FakeAlertMemory(mapOf(A to 2L)))
    val alerts = collect(announcer)
    announcer.onLeakEvent(leak(count = 3))
    runCurrent()
    assertEquals(listOf(GasAlert(A, KITCHEN, 3)), alerts)
  }

  @Test
  fun `news waits for whoever collects it`() = runTest {
    val announcer = announcer()
    announcer.onLeakEvent(leak(count = 1))
    val alerts = collect(announcer)
    assertEquals(listOf(GasAlert(A, KITCHEN, 1)), alerts)
  }

  @Test
  fun `leaks that nobody collected yet all wait, not only the last`() = runTest {
    val announcer = announcer()
    announcer.onLeakEvent(leak(count = 1))
    announcer.onLeakEvent(leak(count = 2))
    val alerts = collect(announcer)
    assertEquals(listOf(GasAlert(A, KITCHEN, 1), GasAlert(A, KITCHEN, 2)), alerts)
  }

  @Test
  fun `a leak announced by an event is remembered`() = runTest {
    announcer().onLeakEvent(leak(count = 1))
    assertEquals(mapOf(A to 1L), memory.saved)
  }

  @Test
  fun `a leak announced by the home is remembered`() = runTest {
    announcer().onHome(house(state = "emergency", alerts = 2))
    assertEquals(mapOf(A to 2L), memory.saved)
  }

  @Test
  fun `leaks that ended are remembered too, so they are not announced later`() = runTest {
    announcer().onHome(house(state = "normal", alerts = 4))
    assertEquals(mapOf(A to 4L), memory.saved)
  }

  @Test
  fun `it saves only when what was announced changes`() = runTest {
    val announcer = announcer()
    announcer.onHome(house(alerts = 1))
    val savesAfterTheFirstHome = memory.saves
    announcer.onHome(house(alerts = 1))
    announcer.onLeakEvent(leak(count = 1))
    assertEquals(savesAfterTheFirstHome, memory.saves)
  }

  @Test
  fun `it does not save what it just loaded`() = runTest {
    val kept = FakeAlertMemory(mapOf(A to 2L))
    announcer(kept).onHome(house(alerts = 2))
    assertEquals(0, kept.saves)
  }

  @Test
  fun `what was saved is what a restart needs to stay quiet about the same leak`() = runTest {
    announcer().onHome(house(state = "emergency", alerts = 1))

    val afterRestart = announcer(memory)
    val alerts = collect(afterRestart)
    afterRestart.onHome(house(state = "emergency", alerts = 1))
    runCurrent()
    assertEquals(emptyList<GasAlert>(), alerts)
  }

  private companion object {
    val A = BootId("aaaa1111")
    val B = BootId("bbbb2222")
    val KITCHEN = AreaId("kitchen")
  }
}
