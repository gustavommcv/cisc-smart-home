package io.github.gustavommcv.ciscsmarthome.data.link

import io.github.gustavommcv.ciscsmarthome.data.remote.BoardError
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardEvents
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardStreamUpdate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * The events of the board by whatever route the link has. It keeps the link up for as long as it is collected, which is for as long as the app syncs. Each route
 * that opens gets a stream of its own, and the one before it is dropped. A route that closes is a loss like any other, with the reason there is no way. While the link is
 * joining there is nothing to say: the connection stays as it was, which is "connecting" at the start.
 */
class RoutedBoardEvents(private val link: BoardLink, private val eventsFor: (BoardRoute.Open) -> BoardEvents) : BoardEvents {
  @OptIn(ExperimentalCoroutinesApi::class)
  override fun updates(): Flow<BoardStreamUpdate> =
    channelFlow {
      launch { link.keepUp() }
      link.route
        .flatMapLatest { route ->
          when (route) {
            is BoardRoute.Open -> eventsFor(route).updates()
            BoardRoute.Joining -> emptyFlow()
            is BoardRoute.Closed -> flowOf(BoardStreamUpdate.Lost(BoardError.NoRoute(route.problem)))
          }
        }
        .collect { send(it) }
    }
}
