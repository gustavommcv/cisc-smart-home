package io.github.gustavommcv.ciscsmarthome.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.gustavommcv.ciscsmarthome.domain.AlertMemory
import io.github.gustavommcv.ciscsmarthome.domain.BootId

/** What was announced, in a small preferences file: one number per boot of the board, so a database would be more than it needs. */
class PreferencesAlertMemory(private val preferences: SharedPreferences) : AlertMemory {
  override fun load(): Map<BootId, Long> = preferences.all.mapNotNull { (boot, count) -> (count as? Long)?.let { BootId(boot) to it } }.toMap()

  override fun save(announced: Map<BootId, Long>) {
    preferences.edit {
      clear()
      announced.forEach { (boot, count) -> putLong(boot.value, count) }
    }
  }
}
