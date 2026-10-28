package io.github.gustavommcv.ciscsmarthome.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.ThemeMode
import io.github.gustavommcv.ciscsmarthome.ui.components.CiscCard
import io.github.gustavommcv.ciscsmarthome.ui.components.IconBadge
import io.github.gustavommcv.ciscsmarthome.ui.components.SectionTitle
import io.github.gustavommcv.ciscsmarthome.ui.components.Tone
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing

/** The settings tab. */
@Composable
internal fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
  val mode by viewModel.themeMode.collectAsStateWithLifecycle()
  SettingsContent(mode, onChooseTheme = viewModel::chooseTheme, modifier = modifier)
}

@Composable
internal fun SettingsContent(mode: ThemeMode, onChooseTheme: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.Xl), verticalArrangement = Arrangement.spacedBy(Spacing.L)) {
    Text(stringResource(R.string.tab_settings), style = MaterialTheme.typography.headlineLarge)
    SectionTitle(R.string.settings_theme)
    // The rows carry their own padding, so the whole row, edge to edge, is the target of the touch.
    CiscCard(contentPadding = PaddingValues()) {
      ThemeMode.entries.forEachIndexed { index, option ->
        if (index > 0) HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.colorScheme.outlineVariant)
        ThemeRow(option, selected = option == mode, onClick = { onChooseTheme(option) })
      }
    }
  }
}

@Composable
private fun ThemeRow(option: ThemeMode, selected: Boolean, onClick: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick, role = Role.RadioButton).padding(horizontal = Spacing.L, vertical = Spacing.M),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Spacing.M),
  ) {
    IconBadge(option.iconRes(), if (selected) Tone.Primary else Tone.Neutral)
    Column(modifier = Modifier.weight(1f)) {
      Text(stringResource(option.labelRes()), style = MaterialTheme.typography.titleMedium)
      option.hintRes()?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
    // The row is the control; the button is only how it shows what is chosen.
    RadioButton(selected = selected, onClick = null)
  }
}
