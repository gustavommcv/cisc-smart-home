package io.github.gustavommcv.ciscsmarthome.data.link

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.OkHttpClient

/**
 * Asks Android to join a Wi-Fi network for the app alone ([WifiNetworkSpecifier]), the way Android means for a device like the board: the network has no
 * internet, the phone's own Wi-Fi is not touched for the other apps, and the app sends its traffic through it by binding its sockets to it.
 *
 * The first time, Android asks the user to allow the app to join that network. It remembers the answer for that app and that network, so the next requests
 * join without asking, until the user forgets the network. Android also gives up on a request that finds nothing ([timeout]).
 */
class AndroidNetworkJoiner(private val connectivity: ConnectivityManager, private val timeout: Duration = JOIN_TIMEOUT) : NetworkJoiner {
  override fun join(ssid: String, password: String): Flow<JoinEvent> =
    callbackFlow {
      val callback =
        object : ConnectivityManager.NetworkCallback() {
          override fun onAvailable(network: Network) {
            trySend(JoinEvent.Available(carriedBy(network)))
          }

          override fun onLost(network: Network) {
            trySend(JoinEvent.Lost)
          }

          override fun onUnavailable() {
            trySend(JoinEvent.Unavailable)
          }
        }
      try {
        connectivity.requestNetwork(request(ssid, password), callback, timeout.inWholeMilliseconds.toInt())
      } catch (e: RuntimeException) {
        // Android refuses a request it cannot take (too many, not allowed): for the link that is a request that came to nothing.
        trySend(JoinEvent.Unavailable)
        close()
        return@callbackFlow
      }
      awaitClose { connectivity.unregisterNetworkCallback(callback) }
    }

  private fun request(ssid: String, password: String): NetworkRequest =
    NetworkRequest.Builder()
      .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
      // The board has no internet, and the app does not pretend it has.
      .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .setNetworkSpecifier(WifiNetworkSpecifier.Builder().setSsid(ssid).setWpa2Passphrase(password).build())
      .build()

  private fun carriedBy(network: Network) =
    JoinedNetwork { http -> http.newBuilder().socketFactory(network.socketFactory).dns { host -> network.getAllByName(host).toList() }.build() }

  companion object {
    /**
     * A join that works takes up to half a minute on the phone of the tests (the board sometimes refuses the first handshake after the phone left it, and Android waits 10 s
     * before trying that network again), so this is longer than that, and short enough that an attempt that is stuck is made again soon.
     */
    val JOIN_TIMEOUT: Duration = 40.seconds
  }
}
