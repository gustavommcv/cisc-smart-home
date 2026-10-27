package io.github.gustavommcv.ciscsmarthome.testing

import io.github.gustavommcv.ciscsmarthome.domain.AlertMemory
import io.github.gustavommcv.ciscsmarthome.domain.BootId

/** What was announced, kept in memory, with how many times it was saved. */
internal class FakeAlertMemory(initial: Map<BootId, Long> = emptyMap()) : AlertMemory {
  var saved: Map<BootId, Long> = initial
    private set
  var saves = 0
    private set

  override fun load(): Map<BootId, Long> = saved

  override fun save(announced: Map<BootId, Long>) {
    saved = announced
    saves++
  }
}
