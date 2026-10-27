package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.Home
import io.github.gustavommcv.ciscsmarthome.domain.HomeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** What the home screen shows: the house as last heard, and whether the board is being heard now. */
data class HomeState(val home: Home?, val connection: ConnectionState)

class HomeViewModel(repository: HomeRepository) : ViewModel() {
  val state: StateFlow<HomeState> =
    combine(repository.home, repository.connection, ::HomeState)
      // Stops following the repository a few seconds after the screen is gone, so a rotation does not restart anything.
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeState(repository.home.value, repository.connection.value))

  private companion object {
    const val STOP_TIMEOUT_MS = 5_000L
  }
}
