package io.github.gustavommcv.ciscsmarthome.data.link

import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient

/** The phone, asked to join a Wi-Fi network for the app. [AndroidNetworkJoiner] is the real one; the tests of whoever uses it give a fake. */
interface NetworkJoiner {
  /**
   * Asks the phone to join [ssid] with [password], and says what comes of it for as long as the flow is collected. The request is in force
   * while it is collected, and ends when the collection does.
   */
  fun join(ssid: String, password: String): Flow<JoinEvent>
}

sealed interface JoinEvent {
  /** The phone joined the network. The request stays in force until the network is [Lost]. */
  data class Available(val network: JoinedNetwork) : JoinEvent

  /** The phone left the network (the board was turned off, it went out of reach). The request is still there, but Android does not join again by itself: it has to be made again. */
  data object Lost : JoinEvent

  /** The request is over without a network: it was not found, the password was wrong, the user refused, or the time ran out. */
  data object Unavailable : JoinEvent
}

/** A network the phone joined for the app. */
fun interface JoinedNetwork {
  /** A client like [http] whose connections go through this network, whatever other network the phone is on. */
  fun carry(http: OkHttpClient): OkHttpClient
}
