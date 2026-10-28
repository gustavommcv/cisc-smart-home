package io.github.gustavommcv.ciscsmarthome.ui.history

import androidx.annotation.StringRes
import io.github.gustavommcv.ciscsmarthome.R
import java.time.Duration

/** How far back the chart goes. */
internal enum class ChartPeriod(val duration: Duration, @StringRes val labelRes: Int) {
  OneHour(Duration.ofHours(1), R.string.period_1h),
  SixHours(Duration.ofHours(6), R.string.period_6h),
  TwentyFourHours(Duration.ofHours(24), R.string.period_24h);

  companion object {
    val Default = SixHours
  }
}
