package io.github.gustavommcv.ciscsmarthome.data.link

import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import kotlinx.coroutines.flow.StateFlow
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

/**
 * How the app gets to the board. It is the one place that knows about the network: [BoardClient][io.github.gustavommcv.ciscsmarthome.data.remote.BoardClient]
 * and [BoardEventStream][io.github.gustavommcv.ciscsmarthome.data.remote.BoardEventStream] only speak the contract of docs/api.md to the address and through the client
 * they are given. To reach the board another way (the cloud, a VPN) is to write another link, and to add it to the choices of the user.
 */
interface BoardLink {
  /** Where the board can be reached now, or why it cannot. */
  val route: StateFlow<BoardRoute>

  /** Does what the link needs doing to have a route, and keeps doing it, until the coroutine is cancelled. Cancelling gives back whatever it held. */
  suspend fun keepUp(): Nothing
}

sealed interface BoardRoute {
  /** The link is working on it, and has nothing to say yet. */
  data object Joining : BoardRoute

  /** The board is to be asked at [baseUrl], with [http]: a client that already goes the way the link made. */
  data class Open(val baseUrl: HttpUrl, val http: OkHttpClient) : BoardRoute

  /** There is no way to the board now. The link goes on trying, unless the problem is one only the user can solve. */
  data class Closed(val problem: ConnectionProblem) : BoardRoute
}
