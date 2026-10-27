package io.github.gustavommcv.ciscsmarthome.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.gustavommcv.ciscsmarthome.ui.home.HomePlaceholder
import io.github.gustavommcv.ciscsmarthome.ui.theme.CiscTheme

/** The only activity: everything the user sees is a Compose screen inside it. */
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      CiscTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { HomePlaceholder() }
      }
    }
  }
}
