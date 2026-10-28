package io.github.gustavommcv.ciscsmarthome.ui.history

import androidx.annotation.StringRes
import io.github.gustavommcv.ciscsmarthome.R
import java.time.Duration

/**
 * How far back the chart goes. The last hours end now and are made of the readings. The month and the year (no [duration]) are those of the
 * calendar, whole, and are made of the hours the app kept: they have pages, the month or the year before.
 */
internal enum class ChartPeriod(val duration: Duration?, @StringRes val labelRes: Int) {
  OneHour(Duration.ofHours(1), R.string.period_1h),
  SixHours(Duration.ofHours(6), R.string.period_6h),
  TwentyFourHours(Duration.ofHours(24), R.string.period_24h),
  Month(null, R.string.period_month),
  Year(null, R.string.period_year);

  /** Goes by the calendar, is made of hours and has pages. */
  val isCalendar: Boolean get() = duration == null

  companion object {
    val Default = SixHours
  }
}
