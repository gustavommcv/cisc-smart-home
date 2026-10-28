package io.github.gustavommcv.ciscsmarthome.testing

import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.NameBook
import io.github.gustavommcv.ciscsmarthome.domain.Names
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The names in memory, writing down what was asked and applying the same cleaning as the real one. */
class FakeNameBook(initial: Names = Names()) : NameBook {
  private val state = MutableStateFlow(initial)

  override val names: StateFlow<Names> = state

  val areaRenames = mutableListOf<Pair<AreaId, String?>>()
  val deviceRenames = mutableListOf<Pair<DeviceId, String?>>()

  override fun renameArea(id: AreaId, name: String?) {
    areaRenames += id to name
    val clean = Names.clean(name.orEmpty())
    state.value = state.value.copy(areas = if (clean == null) state.value.areas - id else state.value.areas + (id to clean))
  }

  override fun renameDevice(id: DeviceId, name: String?) {
    deviceRenames += id to name
    val clean = Names.clean(name.orEmpty())
    state.value = state.value.copy(devices = if (clean == null) state.value.devices - id else state.value.devices + (id to clean))
  }
}
