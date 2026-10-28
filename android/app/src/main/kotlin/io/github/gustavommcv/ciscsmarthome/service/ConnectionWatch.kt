package io.github.gustavommcv.ciscsmarthome.service

import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/** Says what the connection is, and warns if the board stays out of reach: then nothing is watching the house. */
internal class ConnectionWatch(private val notifier: Notifier) {
  /** Follows [connections] for as long as it is collected; stopping it takes the countdown with it. */
  suspend fun watch(connections: Flow<ConnectionState>) {
    // Restarted by every change of the state, so a board that comes back in time is never warned about.
    connections.collectLatest { connection ->
      notifier.showConnection(connection)
      if (connection is ConnectionState.Disconnected) {
        delay(BOARD_LOST_AFTER)
        notifier.showBoardLost(connection)
      } else {
        notifier.clearBoardLost()
      }
    }
  }

  internal companion object {
    /** A board that cannot be reached for this long is more than a hiccup of the network or a restart of the board. */
    val BOARD_LOST_AFTER = 1.minutes
  }
}
