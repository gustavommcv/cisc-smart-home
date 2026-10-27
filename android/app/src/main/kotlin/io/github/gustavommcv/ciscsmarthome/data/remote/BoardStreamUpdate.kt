package io.github.gustavommcv.ciscsmarthome.data.remote

import io.github.gustavommcv.ciscsmarthome.domain.HomeEvent

/** What the event stream tells whoever listens to it. */
sealed interface BoardStreamUpdate {
  /** The board accepted the connection. */
  data object Connected : BoardStreamUpdate

  /** The board announced something. */
  data class Received(val event: HomeEvent) : BoardStreamUpdate

  /** The connection is gone, and why. The stream tries again by itself. */
  data class Lost(val error: BoardError) : BoardStreamUpdate
}
