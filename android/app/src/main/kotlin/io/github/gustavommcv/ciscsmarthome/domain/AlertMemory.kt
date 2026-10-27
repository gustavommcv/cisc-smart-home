package io.github.gustavommcv.ciscsmarthome.domain

/** Where what was already announced is kept, so a restart of the app does not announce a leak twice (see [AlertTracker]). */
interface AlertMemory {
  fun load(): Map<BootId, Long>

  /** Keeps [announced], in place of what was kept before. */
  fun save(announced: Map<BootId, Long>)
}
