package io.github.gustavommcv.ciscsmarthome.data.local

import android.content.SharedPreferences
import androidx.core.content.edit
import io.github.gustavommcv.ciscsmarthome.domain.AreaIcons
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.NameBook
import io.github.gustavommcv.ciscsmarthome.domain.Names
import io.github.gustavommcv.ciscsmarthome.domain.RoomIcon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * The names in a small preferences file: one entry for each, under the kind and the id ("area:kitchen", "device:gas-monitor"), so a room and a
 * device that happen to share an id keep each its own. The icons of the rooms are in the same file, under the key of the icon of that room
 * ("area-icon:kitchen" is "weekend"). What the file has that is not a name or an icon (another kind of value, a blank, a key of no kind, an icon
 * the app does not know) is ignored, so a damaged file, or one from a later version, never keeps the app from opening.
 */
class PreferencesNameBook(private val preferences: SharedPreferences) : NameBook {
  private val state = MutableStateFlow(load())
  private val iconState = MutableStateFlow(loadIcons())

  override val names: StateFlow<Names> = state.asStateFlow()

  override val icons: StateFlow<AreaIcons> = iconState.asStateFlow()

  override fun chooseAreaIcon(id: AreaId, icon: RoomIcon?) {
    preferences.edit { if (icon == null) remove(AREA_ICON + id.value) else putString(AREA_ICON + id.value, icon.key) }
    iconState.update { AreaIcons(if (icon == null) it.areas - id else it.areas + (id to icon)) }
  }

  override fun renameArea(id: AreaId, name: String?) {
    val clean = keep(AREA + id.value, name)
    state.update { it.copy(areas = if (clean == null) it.areas - id else it.areas + (id to clean)) }
  }

  override fun renameDevice(id: DeviceId, name: String?) {
    val clean = keep(DEVICE + id.value, name)
    state.update { it.copy(devices = if (clean == null) it.devices - id else it.devices + (id to clean)) }
  }

  private fun keep(key: String, name: String?): String? {
    val clean = name?.let(Names::clean)
    preferences.edit { if (clean == null) remove(key) else putString(key, clean) }
    return clean
  }

  private fun load(): Names {
    val areas = mutableMapOf<AreaId, String>()
    val devices = mutableMapOf<DeviceId, String>()
    preferences.all.forEach { (key, value) ->
      val name = (value as? String)?.let(Names::clean) ?: return@forEach
      when {
        key.startsWith(AREA) && key.length > AREA.length -> areas[AreaId(key.removePrefix(AREA))] = name
        key.startsWith(DEVICE) && key.length > DEVICE.length -> devices[DeviceId(key.removePrefix(DEVICE))] = name
      }
    }
    return Names(areas, devices)
  }

  private fun loadIcons(): AreaIcons {
    val areas = mutableMapOf<AreaId, RoomIcon>()
    preferences.all.forEach { (key, value) ->
      if (!key.startsWith(AREA_ICON) || key.length <= AREA_ICON.length) return@forEach
      val icon = (value as? String)?.let(RoomIcon::fromKey) ?: return@forEach
      areas[AreaId(key.removePrefix(AREA_ICON))] = icon
    }
    return AreaIcons(areas)
  }

  private companion object {
    const val AREA_ICON = "area-icon:"
    const val AREA = "area:"
    const val DEVICE = "device:"
  }
}
