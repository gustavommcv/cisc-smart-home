package io.github.gustavommcv.ciscsmarthome.data.remote

import io.github.gustavommcv.ciscsmarthome.domain.AlertTypes
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.HomeEvent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString

// What the REST client and the event stream both read from the board.

private const val SUPPORTED_API_VERSION = 1

/** The home in [text], or an [IllegalStateException] when the board speaks another version of the API. */
internal fun parseHome(text: String): Home {
  val home = BoardJson.decodeFromString<HomeDto>(text)
  check(home.apiVersion == SUPPORTED_API_VERSION) { "the board speaks version ${home.apiVersion} of the API" }
  return home.toDomain()
}

/** The error the board answered with: one of the contract, or [BoardError.Unexpected] when it is not. */
internal fun rejectionOf(status: Int, body: String): BoardError =
  try {
    val error = BoardJson.decodeFromString<ErrorEnvelopeDto>(body).error
    BoardError.Rejected(status, ErrorCode.fromWire(error.code), error.message)
  } catch (e: SerializationException) {
    BoardError.Unexpected("the board answered HTTP $status without an error of the contract")
  }

/** The event for a `home` of the stream, or null when the app cannot read it. */
internal fun homeEventOrNull(data: String): HomeEvent.HomeUpdated? =
  try {
    HomeEvent.HomeUpdated(parseHome(data))
  } catch (e: SerializationException) {
    null
  } catch (e: IllegalStateException) {
    null
  }

/** The event for an `alert` of the stream, or null when it is not a kind the app knows or cannot be read. */
internal fun alertEventOrNull(data: String, bootId: BootId): HomeEvent.GasLeak? =
  try {
    val alert = BoardJson.decodeFromString<AlertDto>(data)
    if (alert.type == AlertTypes.GasLeak) HomeEvent.GasLeak(bootId, AreaId(alert.areaId), EntityId(alert.entityId), alert.count, alert.uptimeMs) else null
  } catch (e: SerializationException) {
    null
  }
