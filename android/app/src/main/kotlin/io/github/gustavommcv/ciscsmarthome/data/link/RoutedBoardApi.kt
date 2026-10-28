package io.github.gustavommcv.ciscsmarthome.data.link

import io.github.gustavommcv.ciscsmarthome.data.remote.BoardApi
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardError
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardResult
import io.github.gustavommcv.ciscsmarthome.domain.CommandValue
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.HistoryPage
import io.github.gustavommcv.ciscsmarthome.domain.Home

/** The REST side of the board by the route the link has at the moment of each call. With no route the call fails without trying, saying why. */
class RoutedBoardApi(private val link: BoardLink, private val apiFor: (BoardRoute.Open) -> BoardApi) : BoardApi {
  override suspend fun getHome(): BoardResult<Home> = through { getHome() }

  override suspend fun sendCommand(entityId: EntityId, value: CommandValue): BoardResult<Entity> = through { sendCommand(entityId, value) }

  override suspend fun getHistory(after: Long?, limit: Int?): BoardResult<HistoryPage> = through { getHistory(after, limit) }

  private suspend fun <T> through(call: suspend BoardApi.() -> BoardResult<T>): BoardResult<T> =
    when (val route = link.route.value) {
      is BoardRoute.Open -> apiFor(route).call()
      BoardRoute.Joining -> BoardResult.Failure(BoardError.NoRoute(ConnectionProblem.Unreachable))
      is BoardRoute.Closed -> BoardResult.Failure(BoardError.NoRoute(route.problem))
    }
}
