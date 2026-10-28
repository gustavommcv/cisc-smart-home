package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gustavommcv.ciscsmarthome.domain.AreaId
import io.github.gustavommcv.ciscsmarthome.domain.CommandValue
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.EntityId

/** Where the app is: the list of rooms, or one room, or, before the board is heard, a screen that says why there is nothing to show. */
@Composable
fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  // From a room, the system's back goes to the list; from the list it closes the app, as it should.
  BackHandler(enabled = state.openRoom != null) { viewModel.back() }
  HomeContent(state, onOpenRoom = viewModel::openRoom, onBack = { viewModel.back() }, onCommand = viewModel::send, onNoticeShown = viewModel::dismissNotice, onRenameRoom = viewModel::renameRoom, onRenameDevice = viewModel::renameDevice, modifier = modifier)
}

@Composable
internal fun HomeContent(
  state: HomeState,
  onOpenRoom: (AreaId) -> Unit,
  onBack: () -> Unit,
  onCommand: (EntityId, CommandValue) -> Unit,
  onNoticeShown: () -> Unit,
  onRenameRoom: (AreaId, String?) -> Unit,
  onRenameDevice: (DeviceId, String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  val room = state.openRoom
  val home = state.home
  when {
    room != null -> RoomScreen(room, state.connection, state.names, state.pending, state.notice, onBack, onCommand, onNoticeShown, { onRenameRoom(room.id, it) }, onRenameDevice, modifier)
    home == null -> NoHomeScreen(state.connection, modifier)
    else -> RoomsScreen(home, state.connection, state.names, onOpenRoom, modifier)
  }
}
