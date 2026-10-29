package io.github.gustavommcv.ciscsmarthome.ui.history

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import io.github.gustavommcv.ciscsmarthome.domain.LimitKind
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** What the chart says in words: the least, the greatest, how far back it goes and what it shows to a screen reader. */
@RunWith(RobolectricTestRunner::class)
class HistoryTextTest {
  private val resources: Resources = ApplicationProvider.getApplicationContext<Context>().resources
  private val zone = ZoneId.of("America/Sao_Paulo")
  private val now = Instant.parse("2026-10-27T12:00:00Z")
  private val recent = windowOf(ChartPeriod.SixHours, 0, now, zone)
  private val october = windowOf(ChartPeriod.Month, 0, now, zone)
  private val year2026 = windowOf(ChartPeriod.Year, 0, now, zone)

  @Test
  fun `the least and the greatest say the number the way the app writes numbers, with the unit`() {
    assertEquals("mín. 45 adc", resources.minText(45.0, "adc"))
    assertEquals("máx. 260 adc", resources.maxText(260.0, "adc"))
    assertEquals("mín. 19,4 °C", resources.minText(19.4, "°C"))
    assertEquals("máx. 64,2 %", resources.maxText(64.2, "%"))
  }

  @Test
  fun `without a unit the number stands alone`() {
    assertEquals("mín. 3", resources.minText(3.0, null))
  }

  @Test
  fun `the start of the window says how far back it goes`() {
    assertEquals("há 1 h", resources.sinceText(ChartPeriod.OneHour))
    assertEquals("há 6 h", resources.sinceText(ChartPeriod.SixHours))
    assertEquals("há 24 h", resources.sinceText(ChartPeriod.TwentyFourHours))
  }

  @Test
  fun `a screen reader is told what the chart is, how far back it goes and its range`() {
    assertEquals("Gráfico de Gás nas últimas 6 h, de 45 adc a 260 adc", resources.chartDescription("Gás", recent, zone, 45.0, 260.0, "adc"))
  }

  @Test
  fun `a screen reader is told which month or which year a chart is of`() {
    assertEquals("Gráfico de Temperatura em outubro de 2026, de 17,5 °C a 21,6 °C", resources.chartDescription("Temperatura", october, zone, 17.5, 21.6, "°C"))
    assertEquals("Gráfico de Umidade em 2026, de 40 % a 90 %", resources.chartDescription("Umidade", year2026, zone, 40.0, 90.0, "%"))
  }

  // ---- the ends of the window and its title ------------------------------------------------------

  @Test
  fun `the last hours say how far back they go and end now`() {
    assertEquals("há 6 h", resources.windowStartText(recent, zone))
    assertEquals("agora", resources.windowEndText(recent, zone))
  }

  @Test
  fun `a month begins on its first day and ends on its last`() {
    assertEquals("1 out.", resources.windowStartText(october, zone))
    assertEquals("31 out.", resources.windowEndText(october, zone))
    assertEquals("29 fev.", resources.windowEndText(windowOf(ChartPeriod.Month, 0, Instant.parse("2028-02-10T12:00:00Z"), zone), zone))
  }

  @Test
  fun `a year begins in january and ends in december`() {
    assertEquals("jan.", resources.windowStartText(year2026, zone))
    assertEquals("dez.", resources.windowEndText(year2026, zone))
  }

  @Test
  fun `the title of a month is its name and its year, and the one of a year is the year`() {
    assertEquals("Outubro de 2026", resources.windowTitle(october, zone))
    assertEquals("Fevereiro de 2028", resources.windowTitle(windowOf(ChartPeriod.Month, 0, Instant.parse("2028-02-10T12:00:00Z"), zone), zone))
    assertEquals("2026", resources.windowTitle(year2026, zone))
  }

  @Test
  fun `the title follows the zone, so the last evening of a month is still that month`() {
    assertEquals("Outubro de 2026", resources.windowTitle(windowOf(ChartPeriod.Month, 0, Instant.parse("2026-11-01T01:00:00Z"), zone), zone))
  }

  @Test
  fun `the caption of the hours says what each point is`() {
    assertEquals(
      "Cada hora mostra o menor e o maior valor que a placa leu. Os vazios são o tempo em que o app não ouviu a placa.",
      resources.getString(io.github.gustavommcv.ciscsmarthome.R.string.history_caption_hours),
    )
  }

  @Test
  fun `a device of the list says its room and how many charts it has`() {
    assertEquals("Cozinha · 1 gráfico", resources.deviceSubtitle("Cozinha", 1))
    assertEquals("Quarto · 3 gráficos", resources.deviceSubtitle("Quarto", 3))
  }

  @Test
  fun `the chart that compares is named by both sensors, the second one in lower case`() {
    assertEquals("Temperatura e umidade", resources.compareTitle("Temperatura", "Umidade"))
    assertEquals("Gás e vento", resources.compareTitle("Gás", "Vento"))
  }

  @Test
  fun `the limit of the alarm says what it is, and the ones of the dehumidifier say what it turns on and off at`() {
    assertEquals("limite de alarme", resources.limitText(ResolvedLimit(LimitKind.Alarm, 160.0)))
    assertEquals("liga em 85 %", resources.limitText(ResolvedLimit(LimitKind.DehumidifierOn, 85.0)))
    assertEquals("desliga em 83 %", resources.limitText(ResolvedLimit(LimitKind.DehumidifierOff, 83.0)))
  }

  @Test
  fun `the caption of the days says what a bar is`() {
    assertEquals(
      "Cada barra é o maior valor que a placa leu naquele dia. Os dias sem barra são o tempo em que o app não ouviu a placa. Em vermelho, os dias que passaram do limite de alarme.",
      resources.getString(io.github.gustavommcv.ciscsmarthome.R.string.history_caption_days),
    )
  }
}
