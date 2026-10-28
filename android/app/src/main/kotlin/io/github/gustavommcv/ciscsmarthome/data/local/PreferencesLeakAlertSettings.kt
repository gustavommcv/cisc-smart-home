package io.github.gustavommcv.ciscsmarthome.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertMode
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertModes
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The ways of warning in a small preferences file: one entry for each room that has a way of its own, under the room ("area:kitchen" is "off"). What the file has that is not
 * that (another kind of value, a way the app does not know, a key of no room) is ignored, so a damaged file, or one from a later version, never keeps the app from opening.
 */
class PreferencesLeakAlertSettings(private val preferences: SharedPreferences) : LeakAlertSettings {
  private val state = MutableStateFlow(load())

  override val modes: StateFlow<LeakAlertModes> = state.asStateFlow()

  override fun choose(areaId: AreaId, mode: LeakAlertMode) {
    val own = mode != LeakAlertMode.Default
    preferences.edit { if (own) putString(AREA + areaId.value, mode.key) else remove(AREA + areaId.value) }
    state.update { LeakAlertModes(if (own) it.areas + (areaId to mode) else it.areas - areaId) }
  }

  private fun load(): LeakAlertModes {
    val areas = mutableMapOf<AreaId, LeakAlertMode>()
    preferences.all.forEach { (key, value) ->
      if (!key.startsWith(AREA) || key.length <= AREA.length) return@forEach
      val mode = (value as? String)?.let(LeakAlertMode::fromKey) ?: return@forEach
      areas[AreaId(key.removePrefix(AREA))] = mode
    }
    return LeakAlertModes(areas)
  }

  private companion object {
    const val AREA = "area:"
  }
}
