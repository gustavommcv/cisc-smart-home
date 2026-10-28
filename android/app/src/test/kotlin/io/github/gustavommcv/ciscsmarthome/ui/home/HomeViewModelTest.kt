package io.github.gustavommcv.ciscsmarthome.ui.home

import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.BootId
import io.github.gustavommcv.ciscsmarthome.domain.CommandResult
import io.github.gustavommcv.ciscsmarthome.domain.CommandValue
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionProblem
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.Entity
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.Names
import io.github.gustavommcv.ciscsmarthome.testing.FakeHomeRepository
import io.github.gustavommcv.ciscsmarthome.testing.FakeNameBook
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
  private val repository = FakeHomeRepository()
  private val names = FakeNameBook()
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
    val state = HomeViewModel(repository, names).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    assertEquals(HomeState(home = null, connection = ConnectionState.Connecting), state.value)
  }

  @Test
  fun `the state follows the home of the repository`() = runTest {
    val state = HomeViewModel(repository, names).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    repository.home.value = house
    assertEquals(house, state.value.home)
  }

  @Test
  fun `the state follows the connection of the repository`() = runTest {
    val state = HomeViewModel(repository, names).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    repository.connection.value = ConnectionState.Connected
    assertEquals(ConnectionState.Connected, state.value.connection)
    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
    assertEquals(ConnectionState.Disconnected(ConnectionProblem.Unreachable), state.value.connection)
  }

  @Test
  fun `the last home stays while the connection is down`() = runTest {
    val state = HomeViewModel(repository, names).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    repository.home.value = house
    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
    assertEquals(house, state.value.home)
  }

  @Test
  fun `a screen that opens after the repository already knows starts with what it knows`() = runTest {
    repository.home.value = house
    repository.connection.value = ConnectionState.Connected
    val state = HomeViewModel(repository, names).state
    assertEquals(HomeState(house, ConnectionState.Connected), state.value)
  }

  // ---- which screen is showing ---------------------------------------------------------------

  private val twoRooms =
    Home(BootId("aaaa1111"), 1_000, listOf(Area(AreaId("kitchen"), "Kitchen", emptyList()), Area(AreaId("bedroom"), "Bedroom", emptyList())))

  @Test
  fun `it starts on the list of rooms`() = runTest {
    val viewModel = HomeViewModel(repository, names)
    assertEquals(Screen.Rooms, viewModel.state.value.screen)
  }

  @Test
  fun `opening a room shows it, and going back returns to the list`() = runTest {
    repository.home.value = twoRooms
    val viewModel = HomeViewModel(repository, names)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

    viewModel.openRoom(AreaId("bedroom"))
    assertEquals(Screen.Room(AreaId("bedroom")), viewModel.state.value.screen)

    assertTrue(viewModel.back())
    assertEquals(Screen.Rooms, viewModel.state.value.screen)
  }

  @Test
  fun `going back from the list is not handled, so Android may close the app`() = runTest {
    val viewModel = HomeViewModel(repository, names)
    assertFalse(viewModel.back())
  }

  @Test
  fun `a room that is no longer in the house sends the screen back to the list`() = runTest {
    repository.home.value = twoRooms
    val viewModel = HomeViewModel(repository, names)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    viewModel.openRoom(AreaId("bedroom"))

    repository.home.value = Home(BootId("bbbb2222"), 500, listOf(Area(AreaId("kitchen"), "Kitchen", emptyList())))

    assertEquals(Screen.Rooms, viewModel.state.value.screen)
  }

  @Test
  fun `a room that is still in the house stays open when the house changes`() = runTest {
    repository.home.value = twoRooms
    val viewModel = HomeViewModel(repository, names)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    viewModel.openRoom(AreaId("bedroom"))

    repository.home.value = twoRooms.copy(uptimeMs = 2_000)

    assertEquals(Screen.Room(AreaId("bedroom")), viewModel.state.value.screen)
  }

  @Test
  fun `the room that is open is found in the state`() = runTest {
    repository.home.value = twoRooms
    val viewModel = HomeViewModel(repository, names)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    viewModel.openRoom(AreaId("bedroom"))
    assertEquals("Bedroom", viewModel.state.value.openRoom?.name)
    viewModel.back()
    assertNull(viewModel.state.value.openRoom)
  }

  // ---- commands ------------------------------------------------------------------------------

  private val power = EntityId("dehumidifier-power")
  private val mode = EntityId("dehumidifier-mode")
  private val powerOn = Entity.Switch(power, "Power", on = true, commandable = true)

  private fun TestScope.following(viewModel: HomeViewModel) = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

  @Test
  fun `a command goes to the repository`() = runTest {
    repository.sendHandler = { _, _ -> CommandResult.Applied(powerOn) }
    val viewModel = HomeViewModel(repository, names)
    following(viewModel)
    viewModel.send(power, CommandValue.Flag(true))
    assertEquals(listOf(power to CommandValue.Flag(true)), repository.sent)
  }

  @Test
  fun `the entity waits for the answer, and is free again when it comes`() = runTest {
    val answer = CompletableDeferred<CommandResult>()
    repository.sendHandler = { _, _ -> answer.await() }
    val viewModel = HomeViewModel(repository, names)
    following(viewModel)

    viewModel.send(power, CommandValue.Flag(true))
    assertEquals(setOf(power), viewModel.state.value.pending)

    answer.complete(CommandResult.Applied(powerOn))
    assertEquals(emptySet<EntityId>(), viewModel.state.value.pending)
  }

  @Test
  fun `a second command for an entity that is waiting is not sent`() = runTest {
    val answer = CompletableDeferred<CommandResult>()
    repository.sendHandler = { _, _ -> answer.await() }
    val viewModel = HomeViewModel(repository, names)
    following(viewModel)

    viewModel.send(power, CommandValue.Flag(true))
    viewModel.send(power, CommandValue.Flag(false))

    assertEquals(1, repository.sent.size)
  }

  @Test
  fun `a command for another entity is not held by the one that waits`() = runTest {
    val answer = CompletableDeferred<CommandResult>()
    repository.sendHandler = { _, _ -> answer.await() }
    val viewModel = HomeViewModel(repository, names)
    following(viewModel)

    viewModel.send(power, CommandValue.Flag(true))
    viewModel.send(mode, CommandValue.Choice("manual"))

    assertEquals(2, repository.sent.size)
    assertEquals(setOf(power, mode), viewModel.state.value.pending)
  }

  @Test
  fun `an entity can be commanded again once the answer came`() = runTest {
    repository.sendHandler = { _, _ -> CommandResult.Applied(powerOn) }
    val viewModel = HomeViewModel(repository, names)
    following(viewModel)
    viewModel.send(power, CommandValue.Flag(true))
    viewModel.send(power, CommandValue.Flag(false))
    assertEquals(2, repository.sent.size)
  }

  @Test
  fun `a command that was done says nothing`() = runTest {
    repository.sendHandler = { _, _ -> CommandResult.Applied(powerOn) }
    val viewModel = HomeViewModel(repository, names)
    following(viewModel)
    viewModel.send(power, CommandValue.Flag(true))
    assertNull(viewModel.state.value.notice)
  }

  @Test
  fun `a command the board refused says so`() = runTest {
    repository.sendHandler = { _, _ -> CommandResult.Refused }
    val viewModel = HomeViewModel(repository, names)
    following(viewModel)
    viewModel.send(power, CommandValue.Flag(true))
    assertEquals(CommandNotice.Refused, viewModel.state.value.notice)
  }

  @Test
  fun `a command that could not be sent says why`() = runTest {
    repository.sendHandler = { _, _ -> CommandResult.Failed(ConnectionProblem.Unreachable) }
    val viewModel = HomeViewModel(repository, names)
    following(viewModel)
    viewModel.send(power, CommandValue.Flag(true))
    assertEquals(CommandNotice.Failed(ConnectionProblem.Unreachable), viewModel.state.value.notice)
  }

  @Test
  fun `the notice goes away when it has been seen`() = runTest {
    repository.sendHandler = { _, _ -> CommandResult.Refused }
    val viewModel = HomeViewModel(repository, names)
    following(viewModel)
    viewModel.send(power, CommandValue.Flag(true))
    viewModel.dismissNotice()
    assertNull(viewModel.state.value.notice)
  }

  @Test
  fun `a command that works takes an old notice away`() = runTest {
    var answer: CommandResult = CommandResult.Refused
    repository.sendHandler = { _, _ -> answer }
    val viewModel = HomeViewModel(repository, names)
    following(viewModel)
    viewModel.send(power, CommandValue.Flag(true))
    answer = CommandResult.Applied(powerOn)
    viewModel.send(power, CommandValue.Flag(true))
    assertNull(viewModel.state.value.notice)
  }

  // ---- the names the user gave ----------------------------------------------------------

  @Test
  fun `the state carries the names the user gave`() = runTest {
    names.renameArea(AreaId("kitchen"), "Cozinha da vó")
    val state = HomeViewModel(repository, names).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    assertEquals(Names(areas = mapOf(AreaId("kitchen") to "Cozinha da vó")), state.value.names)
  }

  @Test
  fun `before anyone watches, the state already has the names, so a screen does not flash the defaults`() {
    names.renameDevice(DeviceId("gas-monitor"), "Detector da pia")
    assertEquals(mapOf(DeviceId("gas-monitor") to "Detector da pia"), HomeViewModel(repository, names).state.value.names.devices)
  }

  @Test
  fun `a name given while the screen is open shows at once`() = runTest {
    val state = HomeViewModel(repository, names).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    names.renameDevice(DeviceId("gas-monitor"), "Detector da pia")
    assertEquals(mapOf(DeviceId("gas-monitor") to "Detector da pia"), state.value.names.devices)
  }

  @Test
  fun `renaming a room hands the name to the book`() {
    HomeViewModel(repository, names).renameRoom(AreaId("kitchen"), "Copa")
    assertEquals(listOf<Pair<AreaId, String?>>(AreaId("kitchen") to "Copa"), names.areaRenames)
  }

  @Test
  fun `renaming a device hands the name to the book`() {
    HomeViewModel(repository, names).renameDevice(DeviceId("gas-monitor"), "Detector")
    assertEquals(listOf<Pair<DeviceId, String?>>(DeviceId("gas-monitor") to "Detector"), names.deviceRenames)
  }

  @Test
  fun `going back to the default name hands over no name`() {
    val viewModel = HomeViewModel(repository, names)
    viewModel.renameRoom(AreaId("kitchen"), null)
    viewModel.renameDevice(DeviceId("gas-monitor"), null)
    assertEquals(listOf<Pair<AreaId, String?>>(AreaId("kitchen") to null), names.areaRenames)
    assertEquals(listOf<Pair<DeviceId, String?>>(DeviceId("gas-monitor") to null), names.deviceRenames)
  }
}
