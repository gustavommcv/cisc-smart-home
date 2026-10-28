package io.github.gustavommcv.ciscsmarthome.data.link

import io.github.gustavommcv.ciscsmarthome.data.remote.Backoff
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.takeWhile
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

/**
 * Makes the phone join the Wi-Fi the board creates (docs/decisoes/0001-esp32-como-ponto-de-acesso.md) and opens the route through it, so the user does not
 * have to change the Wi-Fi of the phone. It asks the phone for [ssid] with the password, which comes from outside and may change; a new password replaces the request.
 *
 * Only the user can fix a missing password, so without one the route says so and nothing is asked. A request that ends, because the phone gave up on it or because the
 * network it joined was lost, is made again after a wait that grows ([backoff]), once the one before it was let go: the phone is never asked for two. Asking again is
 * what brings a board back after it was off, for Android leaves the request of a network that was lost waiting, and does not join it again by itself.
 */
class WifiAccessPointLink(
  private val joiner: NetworkJoiner,
  private val ssid: String,
  private val baseUrl: HttpUrl,
  private val http: OkHttpClient,
  private val password: Flow<String?>,
  private val backoff: Backoff = DEFAULT_BACKOFF,
) : BoardLink {
  private val state = MutableStateFlow<BoardRoute>(BoardRoute.Joining)

  override val route: StateFlow<BoardRoute> = state

  override suspend fun keepUp(): Nothing {
    password.collectLatest { password ->
      if (password == null) {
        state.value = BoardRoute.Closed(ConnectionProblem.NetworkNotSet)
      } else {
        state.value = BoardRoute.Joining
        join(password)
      }
    }
    error("the password stopped coming, and it is meant to go on")
  }

  private suspend fun join(password: String): Nothing {
    var failures = 0
    while (true) {
      // The request is in force until the network is lost or the phone gives up on it: either ends it, and the collection ends with it.
      joiner
        .join(ssid, password)
        .takeWhile { it is JoinEvent.Available }
        .filterIsInstance<JoinEvent.Available>()
        .collect { event ->
          failures = 0
          state.value = BoardRoute.Open(baseUrl, event.network.carry(http))
        }
      state.value = BoardRoute.Closed(ConnectionProblem.NetworkNotJoined)
      delay(backoff.delayFor(failures))
      failures++
    }
  }

  companion object {
    /** Android gives up on a request by itself after a while, so the wait only keeps the phone from being asked over and over for a board that is off. */
    val DEFAULT_BACKOFF = Backoff(initial = 5.seconds, max = 30.seconds)
  }
}
