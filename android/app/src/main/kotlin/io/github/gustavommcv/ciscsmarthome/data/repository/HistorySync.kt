package io.github.gustavommcv.ciscsmarthome.data.repository

import io.github.gustavommcv.ciscsmarthome.data.remote.BoardApi
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardResult
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.DeviceClock
import io.github.gustavommcv.ciscsmarthome.domain.HistoryStore
import io.github.gustavommcv.ciscsmarthome.domain.StoredSample

/**
 * Brings the history the phone keeps up to date with the one the board holds. Each sample is dated
 * with [deviceClock], which must already have heard from the boot (see [DeviceClock]); one it cannot
 * place in time is not kept.
 */
class HistorySync(private val api: BoardApi, private val store: HistoryStore, private val deviceClock: DeviceClock) {
  /** Fetches what the board has that the phone does not, a page at a time, from where the phone's history of [bootId] ends. */
  suspend fun catchUp(bootId: BootId) {
    var after = store.latestUptime(bootId)
    while (true) {
      val page = (api.getHistory(after, PAGE_SIZE) as? BoardResult.Success)?.value ?: return
      // The board restarted in between: the connection drops with it, and the next one catches up on the new boot.
      if (page.bootId != bootId || page.samples.isEmpty()) return
      store.save(page.samples.mapNotNull { sample -> deviceClock.toInstant(bootId, sample.uptimeMs)?.let { StoredSample(bootId, sample.uptimeMs, it, sample.values, sample.states) } })
      val last = page.samples.last().uptimeMs
      // A short page is the end. So is a page that does not go past where we are, which a board that works never sends.
      if (page.samples.size < PAGE_SIZE || last <= (after ?: Long.MIN_VALUE)) return
      after = last
    }
  }

  private companion object {
    // The most the contract lets one request ask for, and what the board keeps: a board that has all of it answers in one page.
    const val PAGE_SIZE = 360
  }
}
