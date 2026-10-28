package io.github.gustavommcv.ciscsmarthome.ui.home

import android.content.res.Resources
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.Area

/** What a room card says under the name: how many devices, and how the room is. A room without devices does not say all is well. */
internal fun Resources.roomSubtitle(area: Area, status: Status): String {
  if (area.devices.isEmpty()) return getString(R.string.room_no_devices)
  val devices = area.devices.size
  return getString(R.string.room_subtitle, getQuantityString(R.plurals.devices_count, devices, devices), statusWords(status))
}

internal fun Resources.summaryTitle(status: Status): String =
  getString(
    when (status) {
      Status.Fine -> R.string.summary_fine_title
      Status.Emergency -> R.string.summary_emergency_title
      Status.Warming -> R.string.summary_warming_title
      is Status.Unavailable -> R.string.summary_unavailable_title
    },
  )

/** The line under the title of the summary. For a leak it says where and what to do, with the same words as the warning of the notification. */
internal fun Resources.summaryText(status: Status, areaName: String?): String =
  when (status) {
    Status.Fine -> getString(R.string.summary_fine_text)
    Status.Emergency -> getString(R.string.alert_gas_text, areaName.orEmpty())
    Status.Warming -> getString(R.string.summary_warming_text)
    is Status.Unavailable -> getQuantityString(R.plurals.summary_unavailable_text, status.count, status.count)
  }

private fun Resources.statusWords(status: Status): String =
  when (status) {
    Status.Fine -> getString(R.string.status_fine)
    Status.Emergency -> getString(R.string.status_emergency)
    Status.Warming -> getString(R.string.status_warming)
    is Status.Unavailable -> getQuantityString(R.plurals.status_unavailable, status.count, status.count)
  }
