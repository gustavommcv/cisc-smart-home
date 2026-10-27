package io.github.gustavommcv.ciscsmarthome.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.gustavommcv.ciscsmarthome.CiscApplication
import io.github.gustavommcv.ciscsmarthome.ui.home.HomeScreen
import io.github.gustavommcv.ciscsmarthome.ui.home.HomeViewModel
import io.github.gustavommcv.ciscsmarthome.ui.theme.CiscTheme

/** The only activity: everything the user sees is a Compose screen inside it. */
class MainActivity : ComponentActivity() {
  private val homeViewModel: HomeViewModel by viewModels {
    viewModelFactory { initializer { HomeViewModel((application as CiscApplication).container.homeRepository) } }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CiscTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { HomeScreen(homeViewModel) }
      }
    }
  }
}
