package io.github.gustavommcv.ciscsmarthome.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.ui.components.LockedCard
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing

/** The place of the charts of what the board has read. The app already keeps that history; the charts come next, and until then this says so. */
@Composable
internal fun HistoryScreen(modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxSize().padding(Spacing.Xl), verticalArrangement = Arrangement.spacedBy(Spacing.L)) {
    Text(stringResource(R.string.tab_history), style = MaterialTheme.typography.headlineLarge)
    LockedCard(R.string.history_charts)
  }
}
