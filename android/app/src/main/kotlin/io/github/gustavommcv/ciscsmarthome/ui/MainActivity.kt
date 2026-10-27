package io.github.gustavommcv.ciscsmarthome.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.gustavommcv.ciscsmarthome.AppDependencies
import io.github.gustavommcv.ciscsmarthome.ui.home.HomeScreen
import io.github.gustavommcv.ciscsmarthome.ui.home.HomeViewModel
import io.github.gustavommcv.ciscsmarthome.ui.theme.CiscTheme

/** The only activity: everything the user sees is a Compose screen inside it. */
class MainActivity : ComponentActivity() {
  private val homeViewModel: HomeViewModel by viewModels {
    viewModelFactory { initializer { HomeViewModel((application as AppDependencies).homeRepository) } }
  }

  // Without this, Android (13 and later) hides every notification of the app, the warning of a leak included.
  private val askForNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    askForNotificationsIfNeeded()
    // From the screen, which is in front, Android lets a foreground service start; from the background it would not.
    (application as AppDependencies).startMonitoring()
    setContent {
      CiscTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { HomeScreen(homeViewModel) }
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
}
