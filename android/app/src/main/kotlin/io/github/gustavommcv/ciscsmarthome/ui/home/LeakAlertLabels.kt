package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.annotation.StringRes
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertMode

@StringRes
internal fun LeakAlertMode.labelRes(): Int =
  when (this) {
    LeakAlertMode.Alarm -> R.string.leak_mode_alarm
    LeakAlertMode.Notice -> R.string.leak_mode_notice
    LeakAlertMode.Off -> R.string.leak_mode_off
  }

/** A line that says what the way does, shown under the choice. */
@StringRes
internal fun LeakAlertMode.hintRes(): Int =
  when (this) {
    LeakAlertMode.Alarm -> R.string.leak_mode_alarm_hint
    LeakAlertMode.Notice -> R.string.leak_mode_notice_hint
    LeakAlertMode.Off -> R.string.leak_mode_off_hint
  }

/** Turning the warning off asks first: it leaves the phone silent in a leak, which is not a thing to do with one stray touch. */
internal val LeakAlertMode.needsConfirmation: Boolean get() = this == LeakAlertMode.Off
