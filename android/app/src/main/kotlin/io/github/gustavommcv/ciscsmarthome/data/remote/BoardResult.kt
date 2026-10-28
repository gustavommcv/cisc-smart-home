package io.github.gustavommcv.ciscsmarthome.data.remote

import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import java.io.IOException

/** What a call to the board came to: its answer, or why there is none. */
sealed interface BoardResult<out T> {
  data class Success<T>(val value: T) : BoardResult<T>

  data class Failure(val error: BoardError) : BoardResult<Nothing>
}

sealed interface BoardError {
  /** The board did not answer: no network, wrong network, too slow. */
  data class Unreachable(val cause: IOException) : BoardError

  /** The board answered with an error of the contract. */
  data class Rejected(val status: Int, val code: ErrorCode, val message: String) : BoardError

  /** The board answered with something the app cannot make sense of. */
  data class Unexpected(val detail: String) : BoardError

  /** There is no way to the board to try: the link that makes one is not up, and [problem] is why. */
  data class NoRoute(val problem: ConnectionProblem) : BoardError
}

/** The error codes of docs/api.md. A code the app does not know is [Unknown]. */
enum class ErrorCode(val wire: String?) {
  InvalidBody("invalid-body"),
  InvalidValue("invalid-value"),
  InvalidQuery("invalid-query"),
  NotCommandable("not-commandable"),
  NotFound("not-found"),
  MethodNotAllowed("method-not-allowed"),
  NotAcceptable("not-acceptable"),
  TooManyClients("too-many-clients"),
  Unknown(null);

  companion object {
    fun fromWire(code: String): ErrorCode = entries.firstOrNull { it.wire == code } ?: Unknown
  }
}
