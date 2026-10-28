package io.github.gustavommcv.ciscsmarthome.data.link

import io.github.gustavommcv.ciscsmarthome.domain.LinkKind
import io.github.gustavommcv.ciscsmarthome.domain.LinkSettings
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * The link the user chose. When the choice changes, the link in use is stopped (which gives back what it held) and the new one is built and kept up, so
 * changing how the board is reached does not need the app to restart. What each choice builds is [linkFor]'s to say, which is the one place a new way is added.
 */
class ChosenLink(private val settings: LinkSettings, private val linkFor: (LinkKind) -> BoardLink) : BoardLink {
  private val state = MutableStateFlow<BoardRoute>(BoardRoute.Joining)

  override val route: StateFlow<BoardRoute> = state

  override suspend fun keepUp(): Nothing {
    settings.kind.collectLatest { kind ->
      state.value = BoardRoute.Joining
      val link = linkFor(kind)
      coroutineScope {
        launch { link.route.collect { state.value = it } }
        link.keepUp()
      }
    }
    error("the choice stopped coming, and it is meant to go on")
  }
}
