package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gustavommcv.ciscsmarthome.domain.Area
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.HomeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** The screens of the app: the list of rooms, and one room. */
sealed interface Screen {
  data object Rooms : Screen

  data class Room(val areaId: AreaId) : Screen
}

/** What the home screen shows: the house as last heard, whether the board is being heard now, and which screen is open. */
data class HomeState(val home: Home?, val connection: ConnectionState, val screen: Screen = Screen.Rooms) {
  /** The room that is open, or null on the list. */
  val openRoom: Area?
    get() = (screen as? Screen.Room)?.let { room -> home?.areas?.firstOrNull { it.id == room.areaId } }
}

class HomeViewModel(repository: HomeRepository) : ViewModel() {
  private val screen = MutableStateFlow<Screen>(Screen.Rooms)

  val state: StateFlow<HomeState> =
    combine(repository.home, repository.connection, screen) { home, connection, screen -> HomeState(home, connection, visible(screen, home)) }
      // Stops following the repository a few seconds after the screen is gone, so a rotation does not restart anything.
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeState(repository.home.value, repository.connection.value))

  fun openRoom(areaId: AreaId) {
    screen.value = Screen.Room(areaId)
  }

  /** Goes back to the list. False when it is already there, so the system's back button may close the app. */
  fun back(): Boolean {
    if (screen.value == Screen.Rooms) return false
    screen.value = Screen.Rooms
    return true
  }

  // A room that is no longer in the house cannot stay open.
  private fun visible(screen: Screen, home: Home?): Screen =
    if (screen is Screen.Room && home != null && home.areas.none { it.id == screen.areaId }) Screen.Rooms else screen

  private companion object {
    const val STOP_TIMEOUT_MS = 5_000L
  }
}
