package io.github.gustavommcv.ciscsmarthome.ui.settings

import androidx.lifecycle.ViewModel
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.LinkKind
import io.github.gustavommcv.ciscsmarthome.domain.LinkSettings
import io.github.gustavommcv.ciscsmarthome.domain.ThemeMode
import io.github.gustavommcv.ciscsmarthome.domain.ThemeSettings
import io.github.gustavommcv.ciscsmarthome.domain.WifiPassword
import kotlinx.coroutines.flow.StateFlow

/** What the settings tab shows and changes: the theme, and how the app reaches the board and whether it does. */
class SettingsViewModel(private val theme: ThemeSettings, private val link: LinkSettings, val connection: StateFlow<ConnectionState>) : ViewModel() {
  /** The theme in force, which is the one the tab shows as chosen. */
  val themeMode: StateFlow<ThemeMode> get() = theme.mode

  /** The way of reaching the board in force, which is the one the tab shows as chosen. */
  val linkKind: StateFlow<LinkKind> get() = link.kind

  /** The password of the board's Wi-Fi that was saved, or null. The tab only says whether there is one. */
  val boardPassword: StateFlow<String?> get() = link.boardPassword

  fun forgetBoardPassword() {
    link.forgetBoardPassword()
  }

  fun chooseTheme(mode: ThemeMode) {
    theme.choose(mode)
  }

  fun chooseLink(kind: LinkKind) {
    link.choose(kind)
  }

  /** Saves [password] for joining the board's Wi-Fi. Says false, and saves nothing, when it cannot be a Wi-Fi password. */
  fun saveBoardPassword(password: String): Boolean {
    if (!WifiPassword.isValid(password)) return false
    link.saveBoardPassword(password)
    return true
  }
}
