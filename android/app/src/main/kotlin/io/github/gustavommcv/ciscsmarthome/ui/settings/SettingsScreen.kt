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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.ConnectionState
import io.github.gustavommcv.ciscsmarthome.domain.LinkKind
import io.github.gustavommcv.ciscsmarthome.domain.ThemeMode
import io.github.gustavommcv.ciscsmarthome.domain.WifiPassword
import io.github.gustavommcv.ciscsmarthome.ui.components.CiscCard
import io.github.gustavommcv.ciscsmarthome.ui.components.IconBadge
import io.github.gustavommcv.ciscsmarthome.ui.components.LockedCard
import io.github.gustavommcv.ciscsmarthome.ui.components.SectionTitle
import io.github.gustavommcv.ciscsmarthome.ui.components.Tone
import io.github.gustavommcv.ciscsmarthome.ui.home.ConnectionBanner
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing

/** The settings tab. */
@Composable
internal fun SettingsScreen(viewModel: SettingsViewModel, modifier: Modifier = Modifier) {
  val mode by viewModel.themeMode.collectAsStateWithLifecycle()
  val link by viewModel.linkKind.collectAsStateWithLifecycle()
  val password by viewModel.boardPassword.collectAsStateWithLifecycle()
  val connection by viewModel.connection.collectAsStateWithLifecycle()
  SettingsContent(
    mode,
    link,
    passwordSaved = password != null,
    connection = connection,
    onChooseTheme = viewModel::chooseTheme,
    onChooseLink = viewModel::chooseLink,
    onSavePassword = viewModel::saveBoardPassword,
    onForgetPassword = viewModel::forgetBoardPassword,
    modifier = modifier,
  )
}

@Composable
internal fun SettingsContent(
  mode: ThemeMode,
  link: LinkKind,
  passwordSaved: Boolean,
  connection: ConnectionState,
  onChooseTheme: (ThemeMode) -> Unit,
  onChooseLink: (LinkKind) -> Unit,
  onSavePassword: (String) -> Boolean,
  onForgetPassword: () -> Unit,
  modifier: Modifier = Modifier,
) {
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
    SectionTitle(R.string.settings_connection)
    CiscCard(contentPadding = PaddingValues()) {
      LinkKind.entries.forEachIndexed { index, option ->
        if (index > 0) HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.colorScheme.outlineVariant)
        LinkRow(option, selected = option == link, onClick = { onChooseLink(option) })
        // Below the choice in force: whether it works, and, for the board's Wi-Fi, the password only when it is needed.
        if (option == link) {
          Column(modifier = Modifier.fillMaxWidth().padding(start = Spacing.L, end = Spacing.L, bottom = Spacing.M), verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
            ConnectionBanner(connection, showingLastReading = false)
            if (option == LinkKind.BoardWifi) {
              val panel = wifiPanel(passwordSaved, connection)
              if (panel.showsPasswordField) PasswordEditor(saved = passwordSaved, joinFailed = panel.canForgetPassword, onSave = onSavePassword)
              if (panel.canForgetPassword) TextButton(onClick = onForgetPassword) { Text(stringResource(R.string.link_password_forget)) }
            }
          }
        }
      }
    }
    // The ways to come: shown, so it is clear where they go.
    LockedCard(R.string.link_cloud)
    LockedCard(R.string.link_vpn)
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

@Composable
private fun LinkRow(option: LinkKind, selected: Boolean, onClick: () -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().selectable(selected = selected, onClick = onClick, role = Role.RadioButton).padding(horizontal = Spacing.L, vertical = Spacing.M),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(Spacing.M),
  ) {
    IconBadge(option.iconRes(), if (selected) Tone.Primary else Tone.Neutral)
    Column(modifier = Modifier.weight(1f)) {
      Text(stringResource(option.labelRes()), style = MaterialTheme.typography.titleMedium)
      Text(stringResource(option.hintRes()), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    RadioButton(selected = selected, onClick = null)
  }
}

/** The field for the password of the board's Wi-Fi. What is typed is not kept in the screen once it is saved, and the password is never shown unless the user asks. */
@Composable
private fun PasswordEditor(saved: Boolean, joinFailed: Boolean, onSave: (String) -> Boolean) {
  var text by rememberSaveable { mutableStateOf("") }
  var shown by rememberSaveable { mutableStateOf(false) }
  var invalid by rememberSaveable { mutableStateOf(false) }
  val keyboard = LocalSoftwareKeyboardController.current
  val focus = LocalFocusManager.current
  val save = {
    if (onSave(text)) {
      text = ""
      invalid = false
      shown = false
      keyboard?.hide()
      focus.clearFocus()
    } else {
      invalid = true
    }
  }
  Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
    OutlinedTextField(
      value = text,
      onValueChange = {
        text = it.take(WifiPassword.MAX_LENGTH)
        invalid = false
      },
      label = { Text(stringResource(R.string.link_password_label)) },
      supportingText = {
        Text(
          stringResource(
            when {
              invalid -> R.string.link_password_invalid
              joinFailed -> R.string.link_password_check
              saved -> R.string.link_password_saved
              else -> R.string.link_password_hint
            },
          ),
        )
      },
      isError = invalid,
      singleLine = true,
      visualTransformation = if (shown) VisualTransformation.None else PasswordVisualTransformation(),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { save() }),
      trailingIcon = { TextButton(onClick = { shown = !shown }) { Text(stringResource(if (shown) R.string.link_password_hide else R.string.link_password_show)) } },
      modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = { save() }, enabled = text.isNotEmpty()) { Text(stringResource(R.string.link_password_save)) }
  }
}
