package io.github.gustavommcv.ciscsmarthome.data.remote

import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.HomeEvent
import java.io.EOFException
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

/**
 * The event side of docs/api.md: listens to `GET /api/v1/events` and keeps listening. When the
 * connection is lost, for whatever reason, it says so and connects again, waiting longer after each
 * failure in a row ([backoff]).
 *
 * The contract says to reconnect after [silenceLimit] without receiving anything, not even a `ping`.
 * That is the read timeout of the connection, so the pings of the board are what keeps it open.
 *
 * Like [BoardClient], it does not choose the network or the address. It does take over the read
 * timeout of [http], which becomes [silenceLimit]. The call timeout of [http] still limits how long
 * the connection takes to open, and stops counting once it has (the library cancels it then).
 *
 * An event the app cannot read is skipped. That is safe: every `home` carries the whole state, and
 * a leak whose `alert` was lost still shows in it, through `gas-alerts`.
 */
class BoardEventStream(
  baseUrl: HttpUrl,
  http: OkHttpClient,
  silenceLimit: Duration = SILENCE_LIMIT,
  private val backoff: Backoff = Backoff(),
  private val pause: suspend (Duration) -> Unit = { delay(it) },
) : BoardEvents {
  private val request: Request =
    Request.Builder().url(baseUrl.newBuilder().addPathSegments("api/v1/events").build()).get().build()

  private val streaming: OkHttpClient = http.newBuilder().readTimeout(silenceLimit.inWholeMilliseconds, TimeUnit.MILLISECONDS).build()

  /**
   * Connects when collected and goes on until the collection is cancelled, which cancels the call to
   * the board. Each collection is a stream of its own.
   */
  override fun updates(): Flow<BoardStreamUpdate> = flow {
    var failures = 0
    while (true) {
      connection().collect { update ->
        if (update is BoardStreamUpdate.Connected) failures = 0
        emit(update)
      }
      pause(backoff.delayFor(failures))
      failures++
    }
  }

  /** One connection, from the moment it opens until it is lost. It always ends with a [BoardStreamUpdate.Lost]. */
  private fun connection(): Flow<BoardStreamUpdate> =
    callbackFlow {
      // The boot of this connection, known from its first `home`. The `alert` does not say which boot it is from.
      var bootId: BootId? = null

      val listener =
        object : EventSourceListener() {
          override fun onOpen(eventSource: EventSource, response: Response) {
            trySend(BoardStreamUpdate.Connected)
          }

          override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
            val event: HomeEvent? =
              when (type) {
                "home" -> homeEventOrNull(data)?.also { bootId = it.home.bootId }
                "alert" -> bootId?.let { alertEventOrNull(data, it) }
                else -> null
              }
            if (event != null) trySend(BoardStreamUpdate.Received(event))
          }

          override fun onClosed(eventSource: EventSource) {
            end(BoardError.Unreachable(EOFException("the board closed the event stream")))
          }

          override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
            end(failureOf(t, response))
          }

          private fun end(error: BoardError) {
            trySend(BoardStreamUpdate.Lost(error))
            close()
          }
        }

      val source = EventSources.createFactory(streaming).newEventSource(request, listener)
      awaitClose { source.cancel() }
    }
      // Events are few (a home at most once a second) and an alert must not be dropped, so a slow
      // collector makes them wait instead of making them disappear.
      .buffer(Channel.UNLIMITED)

  private fun failureOf(t: Throwable?, response: Response?): BoardError {
    if (response != null && !response.isSuccessful) {
      return try {
        rejectionOf(response.code, response.body.string())
      } catch (e: IOException) {
        BoardError.Unreachable(e)
      }
    }
    return when (t) {
      is IOException -> BoardError.Unreachable(t)
      else -> BoardError.Unexpected(t?.message ?: "the event stream cannot be used")
    }
  }

  companion object {
    /** The contract: a connection that receives nothing, not even a `ping`, for this long is reconnected. */
    val SILENCE_LIMIT: Duration = 20.seconds
  }
}
