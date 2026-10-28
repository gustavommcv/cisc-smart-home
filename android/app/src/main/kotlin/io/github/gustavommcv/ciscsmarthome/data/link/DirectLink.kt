package io.github.gustavommcv.ciscsmarthome.data.link

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

/** The link that does nothing: the phone is already where the board can be reached (its own network, a VPN), so the route is open from the start. */
class DirectLink(baseUrl: HttpUrl, http: OkHttpClient) : BoardLink {
  override val route: StateFlow<BoardRoute> = MutableStateFlow(BoardRoute.Open(baseUrl, http))

  override suspend fun keepUp(): Nothing = awaitCancellation()
}
