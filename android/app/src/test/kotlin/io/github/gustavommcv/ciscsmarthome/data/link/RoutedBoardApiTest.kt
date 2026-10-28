package io.github.gustavommcv.ciscsmarthome.data.link

import io.github.gustavommcv.ciscsmarthome.data.remote.BoardError
import io.github.gustavommcv.ciscsmarthome.data.remote.BoardResult
import io.github.gustavommcv.ciscsmarthome.data.repository.FakeBoardApi
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.CommandValue
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.HistoryPage
import io.github.gustavommcv.ciscsmarthome.domain.Home
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test

/** The REST client that goes to the board by whatever route the link says, and says there is none when there is none. */
class RoutedBoardApiTest {
  private val link = FakeBoardLink()
  private val opened = mutableListOf<BoardRoute.Open>()
  private val apis = mutableMapOf<BoardRoute.Open, FakeBoardApi>()
  private val api =
    RoutedBoardApi(link) { route ->
      opened += route
      apis.getOrPut(route) { FakeBoardApi() }
    }

  private val first = BoardRoute.Open("http://192.168.4.1/".toHttpUrl(), OkHttpClient())
  private val second = BoardRoute.Open("http://10.0.0.5/".toHttpUrl(), OkHttpClient())
  private val home = Home(BootId("aaaa1111"), uptimeMs = 1_000, areas = emptyList())
  private val level = EntityId("gas-level")

  private fun noRoute(problem: ConnectionProblem) = BoardResult.Failure(BoardError.NoRoute(problem))

  @Test
  fun `the home is asked of the board by the open route`() = runTest {
    link.go(first)
    apis.getOrPut(first) { FakeBoardApi() }.home = { BoardResult.Success(home) }
    assertEquals(BoardResult.Success(home), api.getHome())
    assertEquals(listOf(first), opened)
  }

  @Test
  fun `a command goes to the board by the open route`() = runTest {
    link.go(first)
    val board = apis.getOrPut(first) { FakeBoardApi() }
    api.sendCommand(level, CommandValue.Flag(true))
    assertEquals(listOf(level to CommandValue.Flag(true)), board.commands)
  }

  @Test
  fun `the history is asked of the board by the open route, with what was asked`() = runTest {
    link.go(first)
    val board = apis.getOrPut(first) { FakeBoardApi() }
    board.history = { _, _ -> BoardResult.Success(HistoryPage(BootId("aaaa1111"), emptyList())) }
    api.getHistory(after = 70_000, limit = 360)
    assertEquals(listOf<Pair<Long?, Int?>>(70_000L to 360), board.historyRequests)
  }

  @Test
  fun `a route that changes is the route used for the next call`() = runTest {
    link.go(first)
    api.getHome()
    link.go(second)
    api.getHome()
    assertEquals(listOf(first, second), opened)
  }

  @Test
  fun `while the link is joining there is no way to the board`() = runTest {
    link.go(BoardRoute.Joining)
    assertEquals(noRoute(ConnectionProblem.Unreachable), api.getHome())
    assertEquals(noRoute(ConnectionProblem.Unreachable), api.sendCommand(level, CommandValue.Flag(true)))
    assertEquals(noRoute(ConnectionProblem.Unreachable), api.getHistory())
    assertEquals(emptyList<BoardRoute.Open>(), opened)
  }

  @Test
  fun `a closed route says why there is no way to the board`() = runTest {
    link.go(BoardRoute.Closed(ConnectionProblem.NetworkNotSet))
    assertEquals(noRoute(ConnectionProblem.NetworkNotSet), api.getHome())
    assertEquals(noRoute(ConnectionProblem.NetworkNotSet), api.sendCommand(level, CommandValue.Flag(true)))
    assertEquals(noRoute(ConnectionProblem.NetworkNotSet), api.getHistory())
    link.go(BoardRoute.Closed(ConnectionProblem.NetworkNotJoined))
    assertEquals(noRoute(ConnectionProblem.NetworkNotJoined), api.getHome())
  }
}
