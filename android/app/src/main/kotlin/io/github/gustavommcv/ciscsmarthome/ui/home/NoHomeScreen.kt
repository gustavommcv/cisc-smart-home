package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.messageRes
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing

/** The screen while the app has not heard the house yet: a wait, or, if the board cannot be reached, what to check. */
@Composable
internal fun NoHomeScreen(connection: ConnectionState, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxSize().safeDrawingPadding().padding(Spacing.Xl),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (connection is ConnectionState.Disconnected) {
      Icon(painterResource(R.drawable.ic_warning), contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(HERO_ICON))
      Spacer(Modifier.height(Spacing.L))
      Text(stringResource(R.string.no_connection_title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
      Spacer(Modifier.height(Spacing.S))
      Text(stringResource(connection.messageRes()), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    } else {
      CircularProgressIndicator()
      Spacer(Modifier.height(Spacing.L))
      Text(stringResource(R.string.connection_connecting), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

private val HERO_ICON = 56.dp
