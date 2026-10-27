package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.messageRes

/** Says whether the board is being heard. When it is not, and the screen still shows what it last read, it says that too. */
@Composable
internal fun ConnectionBanner(connection: ConnectionState, showingLastReading: Boolean, modifier: Modifier = Modifier) {
  val color =
    when (connection) {
      ConnectionState.Connecting -> MaterialTheme.colorScheme.onSurfaceVariant
      ConnectionState.Connected -> MaterialTheme.colorScheme.primary
      is ConnectionState.Disconnected -> MaterialTheme.colorScheme.error
    }
  Column(modifier) {
    Text(stringResource(connection.messageRes()), color = color, style = MaterialTheme.typography.bodyLarge)
    if (connection is ConnectionState.Disconnected && showingLastReading) {
      Text(stringResource(R.string.stale_note), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
  }
}
