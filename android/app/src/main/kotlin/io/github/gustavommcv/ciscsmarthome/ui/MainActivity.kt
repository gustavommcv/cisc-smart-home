package io.github.gustavommcv.ciscsmarthome.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.time.Clock
import io.github.gustavommcv.ciscsmarthome.AppDependencies
import io.github.gustavommcv.ciscsmarthome.ui.history.HistoryViewModel
import io.github.gustavommcv.ciscsmarthome.ui.home.HomeViewModel
import io.github.gustavommcv.ciscsmarthome.ui.settings.SettingsViewModel
import io.github.gustavommcv.ciscsmarthome.ui.theme.CiscTheme

/** The only activity: everything the user sees is a Compose screen inside it. */
class MainActivity : ComponentActivity() {
  private val homeViewModel: HomeViewModel by viewModels {
    viewModelFactory { initializer { HomeViewModel((application as AppDependencies).homeRepository) } }
  }

  private val historyViewModel: HistoryViewModel by viewModels {
    viewModelFactory { initializer { HistoryViewModel((application as AppDependencies).homeRepository, Clock.systemUTC()) } }
  }

  private val settingsViewModel: SettingsViewModel by viewModels {
    viewModelFactory { initializer { SettingsViewModel((application as AppDependencies).themeSettings) } }
  }

  // Without this, Android (13 and later) hides every notification of the app, the warning of a leak included.
  private val askForNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    askForNotificationsIfNeeded()
    // From the screen, which is in front, Android lets a foreground service start; from the background it would not.
    (application as AppDependencies).startMonitoring()
    val themeSettings = (application as AppDependencies).themeSettings
    setContent {
      val mode by themeSettings.mode.collectAsStateWithLifecycle()
      val dark = mode.isDark(isSystemInDarkTheme())
      // The bars of the system follow the theme the user chose, not the phone's: with the app light on a phone set to dark,
      // the icons of the status bar would otherwise be light on a light bar.
      DisposableEffect(dark) {
        enableEdgeToEdge(
          statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
          navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
        )
        onDispose {}
      }
      CiscTheme(darkTheme = dark) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { CiscApp(homeViewModel, historyViewModel, settingsViewModel) }
      }
    }
  }

  private fun askForNotificationsIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) {
      askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  private companion object {
    // The scrim under a navigation bar with buttons, which edge to edge would leave unreadable over the screen; with gestures there is none.
    val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
    val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
  }
}
