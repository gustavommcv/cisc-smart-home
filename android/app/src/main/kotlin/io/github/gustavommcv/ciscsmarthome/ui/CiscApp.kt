package io.github.gustavommcv.ciscsmarthome.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.gustavommcv.ciscsmarthome.ui.history.HistoryScreen
import io.github.gustavommcv.ciscsmarthome.ui.home.HomeScreen
import io.github.gustavommcv.ciscsmarthome.ui.home.HomeViewModel
import io.github.gustavommcv.ciscsmarthome.ui.navigation.BottomBar
import io.github.gustavommcv.ciscsmarthome.ui.navigation.Tab
import io.github.gustavommcv.ciscsmarthome.ui.settings.SettingsScreen
import io.github.gustavommcv.ciscsmarthome.ui.settings.SettingsViewModel

/**
 * The app: the bar at the bottom and the tab it has open. Each tab keeps its own state in its own view model, so
 * leaving one and coming back finds it as it was (a room that was open is still open).
 */
@Composable
internal fun CiscApp(homeViewModel: HomeViewModel, settingsViewModel: SettingsViewModel) {
  var tab by rememberSaveable { mutableStateOf(Tab.Start) }
  // From history or settings, back goes home. From home it is the screen's, or the system's: a room closes, then the app does.
  BackHandler(enabled = tab.back() != null) { tab.back()?.let { tab = it } }
  Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = { BottomBar(tab, onSelect = { tab = it }) }) { padding ->
    val content = Modifier.padding(padding)
    when (tab) {
      Tab.Home -> HomeScreen(homeViewModel, content)
      Tab.History -> HistoryScreen(content)
      Tab.Settings -> SettingsScreen(settingsViewModel, content)
    }
  }
}
