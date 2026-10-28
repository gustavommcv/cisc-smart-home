package io.github.gustavommcv.ciscsmarthome.ui.home

import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaIcons
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
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertMode
import io.github.gustavommcv.ciscsmarthome.domain.LeakAlertModes
import io.github.gustavommcv.ciscsmarthome.domain.Names
import io.github.gustavommcv.ciscsmarthome.domain.RoomIcon
import io.github.gustavommcv.ciscsmarthome.testing.FakeHomeRepository
import io.github.gustavommcv.ciscsmarthome.testing.FakeLeakAlertSettings
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
  private val leakModes = FakeLeakAlertSettings()
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
    val state = HomeViewModel(repository, names, leakModes).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    assertEquals(HomeState(home = null, connection = ConnectionState.Connecting), state.value)
  }

  @Test
  fun `the state follows the home of the repository`() = runTest {
    val state = HomeViewModel(repository, names, leakModes).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    repository.home.value = house
    assertEquals(house, state.value.home)
  }

  @Test
  fun `the state follows the connection of the repository`() = runTest {
    val state = HomeViewModel(repository, names, leakModes).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    repository.connection.value = ConnectionState.Connected
    assertEquals(ConnectionState.Connected, state.value.connection)
    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
    assertEquals(ConnectionState.Disconnected(ConnectionProblem.Unreachable), state.value.connection)
  }

  @Test
  fun `the last home stays while the connection is down`() = runTest {
    val state = HomeViewModel(repository, names, leakModes).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    repository.home.value = house
    repository.connection.value = ConnectionState.Disconnected(ConnectionProblem.Unreachable)
    assertEquals(house, state.value.home)
  }

  @Test
  fun `a screen that opens after the repository already knows starts with what it knows`() = runTest {
    repository.home.value = house
    repository.connection.value = ConnectionState.Connected
    val state = HomeViewModel(repository, names, leakModes).state
    assertEquals(HomeState(house, ConnectionState.Connected), state.value)
  }

  // ---- which screen is showing ---------------------------------------------------------------

  private val twoRooms =
    Home(BootId("aaaa1111"), 1_000, listOf(Area(AreaId("kitchen"), "Kitchen", emptyList()), Area(AreaId("bedroom"), "Bedroom", emptyList())))

  @Test
  fun `it starts on the list of rooms`() = runTest {
    val viewModel = HomeViewModel(repository, names, leakModes)
    assertEquals(Screen.Rooms, viewModel.state.value.screen)
  }

  @Test
  fun `opening a room shows it, and going back returns to the list`() = runTest {
    repository.home.value = twoRooms
    val viewModel = HomeViewModel(repository, names, leakModes)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }

    viewModel.openRoom(AreaId("bedroom"))
    assertEquals(Screen.Room(AreaId("bedroom")), viewModel.state.value.screen)

    assertTrue(viewModel.back())
    assertEquals(Screen.Rooms, viewModel.state.value.screen)
  }

  @Test
  fun `going back from the list is not handled, so Android may close the app`() = runTest {
    val viewModel = HomeViewModel(repository, names, leakModes)
    assertFalse(viewModel.back())
  }

  @Test
  fun `a room that is no longer in the house sends the screen back to the list`() = runTest {
    repository.home.value = twoRooms
    val viewModel = HomeViewModel(repository, names, leakModes)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    viewModel.openRoom(AreaId("bedroom"))

    repository.home.value = Home(BootId("bbbb2222"), 500, listOf(Area(AreaId("kitchen"), "Kitchen", emptyList())))

    assertEquals(Screen.Rooms, viewModel.state.value.screen)
  }

  @Test
  fun `a room that is still in the house stays open when the house changes`() = runTest {
    repository.home.value = twoRooms
    val viewModel = HomeViewModel(repository, names, leakModes)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    viewModel.openRoom(AreaId("bedroom"))

    repository.home.value = twoRooms.copy(uptimeMs = 2_000)

    assertEquals(Screen.Room(AreaId("bedroom")), viewModel.state.value.screen)
  }

  @Test
  fun `the room that is open is found in the state`() = runTest {
    repository.home.value = twoRooms
    val viewModel = HomeViewModel(repository, names, leakModes)
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
    val viewModel = HomeViewModel(repository, names, leakModes)
    following(viewModel)
    viewModel.send(power, CommandValue.Flag(true))
    assertEquals(listOf(power to CommandValue.Flag(true)), repository.sent)
  }

  @Test
  fun `the entity waits for the answer, and is free again when it comes`() = runTest {
    val answer = CompletableDeferred<CommandResult>()
    repository.sendHandler = { _, _ -> answer.await() }
    val viewModel = HomeViewModel(repository, names, leakModes)
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
    val viewModel = HomeViewModel(repository, names, leakModes)
    following(viewModel)

    viewModel.send(power, CommandValue.Flag(true))
    viewModel.send(power, CommandValue.Flag(false))

    assertEquals(1, repository.sent.size)
  }

  @Test
  fun `a command for another entity is not held by the one that waits`() = runTest {
    val answer = CompletableDeferred<CommandResult>()
    repository.sendHandler = { _, _ -> answer.await() }
    val viewModel = HomeViewModel(repository, names, leakModes)
    following(viewModel)

    viewModel.send(power, CommandValue.Flag(true))
    viewModel.send(mode, CommandValue.Choice("manual"))

    assertEquals(2, repository.sent.size)
    assertEquals(setOf(power, mode), viewModel.state.value.pending)
  }

  @Test
  fun `an entity can be commanded again once the answer came`() = runTest {
    repository.sendHandler = { _, _ -> CommandResult.Applied(powerOn) }
    val viewModel = HomeViewModel(repository, names, leakModes)
    following(viewModel)
    viewModel.send(power, CommandValue.Flag(true))
    viewModel.send(power, CommandValue.Flag(false))
    assertEquals(2, repository.sent.size)
  }

  @Test
  fun `a command that was done says nothing`() = runTest {
    repository.sendHandler = { _, _ -> CommandResult.Applied(powerOn) }
    val viewModel = HomeViewModel(repository, names, leakModes)
    following(viewModel)
    viewModel.send(power, CommandValue.Flag(true))
    assertNull(viewModel.state.value.notice)
  }

  @Test
  fun `a command the board refused says so`() = runTest {
    repository.sendHandler = { _, _ -> CommandResult.Refused }
    val viewModel = HomeViewModel(repository, names, leakModes)
    following(viewModel)
    viewModel.send(power, CommandValue.Flag(true))
    assertEquals(CommandNotice.Refused, viewModel.state.value.notice)
  }

  @Test
  fun `a command that could not be sent says why`() = runTest {
    repository.sendHandler = { _, _ -> CommandResult.Failed(ConnectionProblem.Unreachable) }
    val viewModel = HomeViewModel(repository, names, leakModes)
    following(viewModel)
    viewModel.send(power, CommandValue.Flag(true))
    assertEquals(CommandNotice.Failed(ConnectionProblem.Unreachable), viewModel.state.value.notice)
  }

  @Test
  fun `the notice goes away when it has been seen`() = runTest {
    repository.sendHandler = { _, _ -> CommandResult.Refused }
    val viewModel = HomeViewModel(repository, names, leakModes)
    following(viewModel)
    viewModel.send(power, CommandValue.Flag(true))
    viewModel.dismissNotice()
    assertNull(viewModel.state.value.notice)
  }

  @Test
  fun `a command that works takes an old notice away`() = runTest {
    var answer: CommandResult = CommandResult.Refused
    repository.sendHandler = { _, _ -> answer }
    val viewModel = HomeViewModel(repository, names, leakModes)
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
    val state = HomeViewModel(repository, names, leakModes).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    assertEquals(Names(areas = mapOf(AreaId("kitchen") to "Cozinha da vó")), state.value.names)
  }

  @Test
  fun `before anyone watches, the state already has the names, so a screen does not flash the defaults`() {
    names.renameDevice(DeviceId("gas-monitor"), "Detector da pia")
    assertEquals(mapOf(DeviceId("gas-monitor") to "Detector da pia"), HomeViewModel(repository, names, leakModes).state.value.names.devices)
  }

  @Test
  fun `a name given while the screen is open shows at once`() = runTest {
    val state = HomeViewModel(repository, names, leakModes).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    names.renameDevice(DeviceId("gas-monitor"), "Detector da pia")
    assertEquals(mapOf(DeviceId("gas-monitor") to "Detector da pia"), state.value.names.devices)
  }

  @Test
  fun `renaming a room hands the name to the book`() {
    HomeViewModel(repository, names, leakModes).renameRoom(AreaId("kitchen"), "Copa")
    assertEquals(listOf<Pair<AreaId, String?>>(AreaId("kitchen") to "Copa"), names.areaRenames)
  }

  @Test
  fun `renaming a device hands the name to the book`() {
    HomeViewModel(repository, names, leakModes).renameDevice(DeviceId("gas-monitor"), "Detector")
    assertEquals(listOf<Pair<DeviceId, String?>>(DeviceId("gas-monitor") to "Detector"), names.deviceRenames)
  }

  @Test
  fun `going back to the default name hands over no name`() {
    val viewModel = HomeViewModel(repository, names, leakModes)
    viewModel.renameRoom(AreaId("kitchen"), null)
    viewModel.renameDevice(DeviceId("gas-monitor"), null)
    assertEquals(listOf<Pair<AreaId, String?>>(AreaId("kitchen") to null), names.areaRenames)
    assertEquals(listOf<Pair<DeviceId, String?>>(DeviceId("gas-monitor") to null), names.deviceRenames)
  }

  // ---- the icons the user gave to the rooms ---------------------------------------------

  @Test
  fun `the state carries the icons the user gave`() = runTest {
    names.chooseAreaIcon(AreaId("kitchen"), RoomIcon.Dining)
    val state = HomeViewModel(repository, names, leakModes).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    assertEquals(AreaIcons(mapOf(AreaId("kitchen") to RoomIcon.Dining)), state.value.icons)
  }

  @Test
  fun `before anyone watches, the state already has the icons, so a screen does not flash the defaults`() {
    names.chooseAreaIcon(AreaId("kitchen"), RoomIcon.Yard)
    assertEquals(RoomIcon.Yard, HomeViewModel(repository, names, leakModes).state.value.icons.of(AreaId("kitchen")))
  }

  @Test
  fun `an icon given while the screen is open shows at once`() = runTest {
    val state = HomeViewModel(repository, names, leakModes).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    names.chooseAreaIcon(AreaId("kitchen"), RoomIcon.Garage)
    assertEquals(RoomIcon.Garage, state.value.icons.of(AreaId("kitchen")))
  }

  @Test
  fun `choosing the icon of a room hands it to the book`() {
    HomeViewModel(repository, names, leakModes).chooseRoomIcon(AreaId("kitchen"), RoomIcon.Dining)
    assertEquals(listOf<Pair<AreaId, RoomIcon?>>(AreaId("kitchen") to RoomIcon.Dining), names.iconChoices)
  }

  @Test
  fun `going back to the default icon hands over no icon`() {
    HomeViewModel(repository, names, leakModes).chooseRoomIcon(AreaId("kitchen"), null)
    assertEquals(listOf<Pair<AreaId, RoomIcon?>>(AreaId("kitchen") to null), names.iconChoices)
  }

  // ---- commands that go one after the other -----------------------------------------------

  private val onAt = EntityId("dehumidifier-on-at")
  private val offAt = EntityId("dehumidifier-off-at")
  private val applied = CommandResult.Applied(powerOn)

  @Test
  fun `commands in order go one after the other, each only when the one before was answered`() = runTest {
    val first = CompletableDeferred<CommandResult>()
    repository.sendHandler = { id, _ -> if (id == offAt) first.await() else applied }
    val viewModel = HomeViewModel(repository, names, leakModes)
    following(viewModel)

    viewModel.sendInOrder(listOf(offAt to CommandValue.Number(83.0), onAt to CommandValue.Number(85.0)))
    assertEquals(listOf(offAt to CommandValue.Number(83.0)), repository.sent)

    first.complete(applied)
    assertEquals(listOf(offAt to CommandValue.Number(83.0), onAt to CommandValue.Number(85.0)), repository.sent)
  }

  @Test
  fun `all the entities wait while the commands go, and are free when the last was answered`() = runTest {
    val first = CompletableDeferred<CommandResult>()
    repository.sendHandler = { id, _ -> if (id == offAt) first.await() else applied }
    val viewModel = HomeViewModel(repository, names, leakModes)
    following(viewModel)

    viewModel.sendInOrder(listOf(offAt to CommandValue.Number(83.0), onAt to CommandValue.Number(85.0)))
    assertEquals(setOf(offAt, onAt), viewModel.state.value.pending)

    first.complete(applied)
    assertEquals(emptySet<EntityId>(), viewModel.state.value.pending)
  }

  @Test
  fun `a command the board refuses stops the ones after it, and says so`() = runTest {
    repository.sendHandler = { _, _ -> CommandResult.Refused }
    val viewModel = HomeViewModel(repository, names, leakModes)
    following(viewModel)

    viewModel.sendInOrder(listOf(offAt to CommandValue.Number(83.0), onAt to CommandValue.Number(85.0)))

    assertEquals(listOf(offAt to CommandValue.Number(83.0)), repository.sent)
    assertEquals(CommandNotice.Refused, viewModel.state.value.notice)
    assertEquals(emptySet<EntityId>(), viewModel.state.value.pending)
  }

  @Test
  fun `a command that could not be sent stops the ones after it, and says why`() = runTest {
    repository.sendHandler = { _, _ -> CommandResult.Failed(ConnectionProblem.Unreachable) }
    val viewModel = HomeViewModel(repository, names, leakModes)
    following(viewModel)

    viewModel.sendInOrder(listOf(offAt to CommandValue.Number(83.0), onAt to CommandValue.Number(85.0)))

    assertEquals(1, repository.sent.size)
    assertEquals(CommandNotice.Failed(ConnectionProblem.Unreachable), viewModel.state.value.notice)
  }

  @Test
  fun `commands that were all done say nothing`() = runTest {
    repository.sendHandler = { _, _ -> applied }
    val viewModel = HomeViewModel(repository, names, leakModes)
    following(viewModel)
    viewModel.sendInOrder(listOf(offAt to CommandValue.Number(83.0), onAt to CommandValue.Number(85.0)))
    assertNull(viewModel.state.value.notice)
  }

  @Test
  fun `nothing is sent when one of the entities is still waiting for an answer`() = runTest {
    val answer = CompletableDeferred<CommandResult>()
    repository.sendHandler = { _, _ -> answer.await() }
    val viewModel = HomeViewModel(repository, names, leakModes)
    following(viewModel)
    viewModel.send(onAt, CommandValue.Number(90.0))

    viewModel.sendInOrder(listOf(offAt to CommandValue.Number(83.0), onAt to CommandValue.Number(85.0)))

    assertEquals(listOf(onAt to CommandValue.Number(90.0)), repository.sent)
  }

  @Test
  fun `no commands is nothing sent`() = runTest {
    val viewModel = HomeViewModel(repository, names, leakModes)
    following(viewModel)
    viewModel.sendInOrder(emptyList())
    assertEquals(emptyList<Pair<EntityId, CommandValue>>(), repository.sent)
    assertEquals(emptySet<EntityId>(), viewModel.state.value.pending)
  }

  // ---- the way the user chose to be warned ------------------------------------------------

  @Test
  fun `the state carries the ways the user chose`() = runTest {
    leakModes.choose(AreaId("kitchen"), LeakAlertMode.Notice)
    val state = HomeViewModel(repository, names, leakModes).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    assertEquals(LeakAlertModes(mapOf(AreaId("kitchen") to LeakAlertMode.Notice)), state.value.leakModes)
  }

  @Test
  fun `before anyone watches, the state already has the ways, so a screen does not flash the default`() {
    leakModes.choose(AreaId("kitchen"), LeakAlertMode.Off)
    assertEquals(LeakAlertMode.Off, HomeViewModel(repository, names, leakModes).state.value.leakModes.of(AreaId("kitchen")))
  }

  @Test
  fun `a way chosen while the screen is open shows at once`() = runTest {
    val state = HomeViewModel(repository, names, leakModes).state
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
    leakModes.choose(AreaId("kitchen"), LeakAlertMode.Off)
    assertEquals(LeakAlertMode.Off, state.value.leakModes.of(AreaId("kitchen")))
  }

  @Test
  fun `choosing the way of a room hands it to the settings`() {
    HomeViewModel(repository, names, leakModes).chooseLeakAlertMode(AreaId("kitchen"), LeakAlertMode.Notice)
    assertEquals(listOf(AreaId("kitchen") to LeakAlertMode.Notice), leakModes.chosen)
  }
}
