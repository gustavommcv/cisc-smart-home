package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaIcons
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.CommandResult
import io.github.gustavommcv.ciscsmarthome.domain.CommandValue
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.HomeRepository
import io.github.gustavommcv.ciscsmarthome.domain.NameBook
import io.github.gustavommcv.ciscsmarthome.domain.Names
import io.github.gustavommcv.ciscsmarthome.domain.RoomIcon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The screens of the app: the list of rooms, and one room. */
sealed interface Screen {
  data object Rooms : Screen

  data class Room(val areaId: AreaId) : Screen
}

/**
 * What the home screen shows: the house as last heard, whether the board is being heard now, which screen is open,
 * which entities are waiting for the answer to a command, and what to tell the user about a command that failed.
 */
data class HomeState(
  val home: Home?,
  val connection: ConnectionState,
  val screen: Screen = Screen.Rooms,
  val pending: Set<EntityId> = emptySet(),
  val notice: CommandNotice? = null,
  val names: Names = Names(),
  val icons: AreaIcons = AreaIcons(),
) {
  /** The room that is open, or null on the list. */
  val openRoom: Area?
    get() = (screen as? Screen.Room)?.let { room -> home?.areas?.firstOrNull { it.id == room.areaId } }
}

class HomeViewModel(private val repository: HomeRepository, private val nameBook: NameBook) : ViewModel() {
  private val screen = MutableStateFlow<Screen>(Screen.Rooms)
  private val pending = MutableStateFlow<Set<EntityId>>(emptySet())
  private val notice = MutableStateFlow<CommandNotice?>(null)

  val state: StateFlow<HomeState> =
    combine(
        combine(repository.home, repository.connection, screen) { home, connection, screen -> Triple(home, connection, screen) },
        combine(pending, notice, nameBook.names, nameBook.icons) { pending, notice, names, icons -> Pair(Triple(pending, notice, names), icons) },
      ) { (home, connection, screen), (own, icons) ->
        val (pending, notice, names) = own
        HomeState(home, connection, visible(screen, home), pending, notice, names, icons)
      }
      // Stops following the repository a few seconds after the screen is gone, so a rotation does not restart anything.
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeState(repository.home.value, repository.connection.value, names = nameBook.names.value, icons = nameBook.icons.value))

  fun openRoom(areaId: AreaId) {
    screen.value = Screen.Room(areaId)
  }

  /** Goes back to the list. False when it is already there, so the system's back button may close the app. */
  fun back(): Boolean {
    if (screen.value == Screen.Rooms) return false
    screen.value = Screen.Rooms
    return true
  }

  /** Asks the board to change an entity. One command at a time for each entity: while it waits for its answer, another is not sent. */
  fun send(entityId: EntityId, value: CommandValue) {
    if (entityId in pending.value) return
    pending.update { it + entityId }
    viewModelScope.launch {
      try {
        notice.value = repository.send(entityId, value).toNotice()
      } finally {
        pending.update { it - entityId }
      }
    }
  }

  /**
   * Asks the board for several changes, one after the other, each only when the one before was answered (the order matters to the board, as in going back to the default
   * limits). Every entity involved waits for its answer, so none is commanded meanwhile; the first the board does not do stops the rest, and says why.
   */
  fun sendInOrder(commands: List<Pair<EntityId, CommandValue>>) {
    val ids = commands.map { it.first }.toSet()
    if (ids.isEmpty() || ids.any { it in pending.value }) return
    pending.update { it + ids }
    viewModelScope.launch {
      try {
        for ((entityId, value) in commands) {
          val result = repository.send(entityId, value)
          notice.value = result.toNotice()
          if (result !is CommandResult.Applied) break
        }
      } finally {
        pending.update { it - ids }
      }
    }
  }

  private fun CommandResult.toNotice(): CommandNotice? =
    when (this) {
      is CommandResult.Applied -> null
      CommandResult.Refused -> CommandNotice.Refused
      is CommandResult.Failed -> CommandNotice.Failed(problem)
    }

  /** Names a room, or, with null, goes back to the default name. */
  fun renameRoom(areaId: AreaId, name: String?) {
    nameBook.renameArea(areaId, name)
  }

  fun chooseRoomIcon(areaId: AreaId, icon: RoomIcon?) {
    nameBook.chooseAreaIcon(areaId, icon)
  }

  fun renameDevice(deviceId: DeviceId, name: String?) {
    nameBook.renameDevice(deviceId, name)
  }

  /** The user has seen what the notice said. */
  fun dismissNotice() {
    notice.value = null
  }

  // A room that is no longer in the house cannot stay open.
  private fun visible(screen: Screen, home: Home?): Screen =
    if (screen is Screen.Room && home != null && home.areas.none { it.id == screen.areaId }) Screen.Rooms else screen

  private companion object {
    const val STOP_TIMEOUT_MS = 5_000L
  }
}
