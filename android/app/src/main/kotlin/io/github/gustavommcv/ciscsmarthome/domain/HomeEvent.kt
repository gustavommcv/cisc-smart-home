package io.github.gustavommcv.ciscsmarthome.domain

/** Something the board announces on its own, without being asked. */
sealed interface HomeEvent {
  /** The house as it is now. */
  data class HomeUpdated(val home: Home) : HomeEvent

  /**
   * A gas leak started. [count] is the board's number for it: together with [bootId] it names this
   * leak, so it can be told apart from the ones already announced (see [AlertTracker]).
   */
  data class GasLeak(val bootId: BootId, val areaId: AreaId, val entityId: EntityId, val count: Long, val uptimeMs: Long) : HomeEvent
}
