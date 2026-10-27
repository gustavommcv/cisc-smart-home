package io.github.gustavommcv.ciscsmarthome.domain

/**
 * Decides when a gas leak is news. The board counts its leaks (`gas-alerts`), and the pair (boot id,
 * count) names each one, so the same leak is announced once however it reaches the app: by its
 * alert event, by the state of the house on reconnecting, or both.
 *
 * [announced] is what was already announced, per boot; give it back what [announced] returned
 * before the app was restarted, so a leak is not announced twice across a restart.
 */
class AlertTracker(announced: Map<BootId, Long> = emptyMap()) {
  private val known = announced.toMutableMap()

  /** An alert event arrived. True when this leak has not been announced yet. */
  fun onAlertEvent(bootId: BootId, count: Long): Boolean = announceIfNew(bootId, count)

  /**
   * The state of the house arrived. A leak that is still active and not announced yet is news. Leaks
   * that already ended are not: they are only remembered, so they never announce later.
   */
  fun onHome(bootId: BootId, alertCount: Long, leakActive: Boolean): Boolean {
    if (leakActive) return announceIfNew(bootId, alertCount)
    known[bootId] = maxOf(known[bootId] ?: 0L, alertCount)
    return false
  }

  /** What is known, per boot, to be kept across restarts of the app. */
  fun announced(): Map<BootId, Long> = known.toMap()

  private fun announceIfNew(bootId: BootId, count: Long): Boolean {
    if (count <= (known[bootId] ?: 0L)) return false
    known[bootId] = count
    return true
  }
}
