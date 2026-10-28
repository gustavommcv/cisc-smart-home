package io.github.gustavommcv.ciscsmarthome.ui.history

import android.content.Context
import android.content.res.Resources
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** What the chart says in words: the least, the greatest, how far back it goes and what it shows to a screen reader. */
@RunWith(RobolectricTestRunner::class)
class HistoryTextTest {
  private val resources: Resources = ApplicationProvider.getApplicationContext<Context>().resources

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
    assertEquals("Gráfico de Gás nas últimas 6 h, de 45 adc a 260 adc", resources.chartDescription("Gás", ChartPeriod.SixHours, 45.0, 260.0, "adc"))
  }
}
