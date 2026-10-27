package io.github.gustavommcv.ciscsmarthome.ui.home

import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.testing.FakeHomeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
  private val repository = FakeHomeRepository()
  private val house = Home(BootId("aaaa1111"), 1_000, listOf(Area(AreaId("kitchen"), "Kitchen", emptyList())))

  @Before
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `before the board is heard from, it is connecting and has no home`() = runTest {
    val state = HomeViewModel(repository).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    assertEquals(HomeState(home = null, connection = ConnectionState.Connecting), state.value)
  }

  @Test
  fun `the state follows the home of the repository`() = runTest {
    val state = HomeViewModel(repository).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    repository.home.value = house
    assertEquals(house, state.value.home)
  }

  @Test
  fun `the state follows the connection of the repository`() = runTest {
    val state = HomeViewModel(repository).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    repository.connection.value = ConnectionState.Connected
    assertEquals(ConnectionState.Connected, state.value.connection)
    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
    assertEquals(ConnectionState.Disconnected(ConnectionProblem.Unreachable), state.value.connection)
  }

  @Test
  fun `the last home stays while the connection is down`() = runTest {
    val state = HomeViewModel(repository).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    repository.home.value = house
    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
    assertEquals(house, state.value.home)
  }

  @Test
  fun `a screen that opens after the repository already knows starts with what it knows`() = runTest {
    repository.home.value = house
    repository.connection.value = ConnectionState.Connected
    val state = HomeViewModel(repository).state
    assertEquals(HomeState(house, ConnectionState.Connected), state.value)
  }
}
