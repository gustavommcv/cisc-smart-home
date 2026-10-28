package io.github.gustavommcv.ciscsmarthome.ui.home

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.Device
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** What the cards say, in words: how many devices, and how the room is. */
@RunWith(RobolectricTestRunner::class)
class StatusTextTest {
  private val resources: Resources = ApplicationProvider.getApplicationContext<Context>().resources

  private fun room(devices: Int) = Area(AreaId("kitchen"), "Kitchen", List(devices) { Device(DeviceId("device-$it"), "Device", emptyList()) })

  @Test
  fun `a room with one device says so in the singular and that all is well`() {
    assertEquals("1 dispositivo · tudo certo", resources.roomSubtitle(room(1), Status.Fine))
  }

  @Test
  fun `a room with more devices says so in the plural`() {
    assertEquals("2 dispositivos · tudo certo", resources.roomSubtitle(room(2), Status.Fine))
  }

  @Test
  fun `a room says a leak is a leak`() {
    assertEquals("1 dispositivo · vazamento de gás", resources.roomSubtitle(room(1), Status.Emergency))
  }

  @Test
  fun `a room says its gas sensor is warming up`() {
    assertEquals("1 dispositivo · aquecendo o sensor", resources.roomSubtitle(room(1), Status.Warming))
  }

  @Test
  fun `a room says how many readings are missing`() {
    assertEquals("1 dispositivo · 1 sem leitura", resources.roomSubtitle(room(1), Status.Unavailable(1)))
    assertEquals("2 dispositivos · 3 sem leitura", resources.roomSubtitle(room(2), Status.Unavailable(3)))
  }

  @Test
  fun `a room without devices does not say all is well`() {
    assertEquals("Nenhum dispositivo", resources.roomSubtitle(room(0), Status.Fine))
  }

  @Test
  fun `the summary of a house that is fine says it`() {
    assertEquals("Tudo certo", resources.summaryTitle(Status.Fine))
    assertEquals("Nenhum vazamento detectado", resources.summaryText(Status.Fine, areaName = null))
  }

  @Test
  fun `the summary of a leak says where, and what to do`() {
    assertEquals("Vazamento de gás", resources.summaryTitle(Status.Emergency))
    assertEquals("Local: Cozinha. Ventile o ambiente e evite chamas ou faíscas.", resources.summaryText(Status.Emergency, areaName = "Cozinha"))
  }

  @Test
  fun `the summary of a gas sensor warming up does not say there is no leak`() {
    assertEquals("Aquecendo o sensor de gás", resources.summaryTitle(Status.Warming))
    assertEquals("Ainda não dá para dizer se há vazamento", resources.summaryText(Status.Warming, areaName = null))
  }

  @Test
  fun `the summary of missing readings says how many`() {
    assertEquals("Faltam leituras", resources.summaryTitle(Status.Unavailable(1)))
    assertEquals("1 leitura sem resposta da placa", resources.summaryText(Status.Unavailable(1), areaName = null))
    assertEquals("2 leituras sem resposta da placa", resources.summaryText(Status.Unavailable(2), areaName = null))
  }
}
