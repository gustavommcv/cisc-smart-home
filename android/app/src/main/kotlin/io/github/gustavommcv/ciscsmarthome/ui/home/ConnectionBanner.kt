package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.messageRes
import io.github.gustavommcv.ciscsmarthome.ui.components.StatusPill
import io.github.gustavommcv.ciscsmarthome.ui.components.Tone
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing

/**
 * Says whether the board is being heard, in a few words. When it is not, it says why below, and, if the screen
 * still shows what the board last read, that too.
 */
@Composable
internal fun ConnectionBanner(connection: ConnectionState, showingLastReading: Boolean, modifier: Modifier = Modifier) {
  Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
    StatusPill(stringResource(connection.pillRes()), connection.tone())
    if (connection is ConnectionState.Disconnected) {
      Text(stringResource(connection.messageRes()), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
      if (showingLastReading) Text(stringResource(R.string.stale_note), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
  }
}

internal fun ConnectionState.tone(): Tone =
  when (this) {
    ConnectionState.Connecting -> Tone.Neutral
    ConnectionState.Connected -> Tone.Primary
    is ConnectionState.Disconnected -> Tone.Alert
  }

/** The few words for the pill. What went wrong, when something did, is [messageRes], shown under it. */
@StringRes
internal fun ConnectionState.pillRes(): Int =
  when (this) {
    ConnectionState.Connecting -> R.string.connection_connecting
    ConnectionState.Connected -> R.string.connection_connected
    is ConnectionState.Disconnected -> R.string.no_connection_title
  }
