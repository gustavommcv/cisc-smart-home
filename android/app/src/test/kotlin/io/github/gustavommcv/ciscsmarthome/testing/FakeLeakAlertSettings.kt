package io.github.gustavommcv.ciscsmarthome.testing

import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertMode
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertModes
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The ways of warning in memory, writing down what was chosen. */
class FakeLeakAlertSettings(initial: LeakAlertModes = LeakAlertModes()) : LeakAlertSettings {
  private val state = MutableStateFlow(initial)

  override val modes: StateFlow<LeakAlertModes> = state

  val chosen = mutableListOf<Pair<AreaId, LeakAlertMode>>()

  override fun choose(areaId: AreaId, mode: LeakAlertMode) {
    chosen += areaId to mode
    state.value = LeakAlertModes(if (mode == LeakAlertMode.Default) state.value.areas - areaId else state.value.areas + (areaId to mode))
  }
}
