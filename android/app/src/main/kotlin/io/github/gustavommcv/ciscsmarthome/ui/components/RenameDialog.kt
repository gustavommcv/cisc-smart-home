package io.github.gustavommcv.ciscsmarthome.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.Names

/**
 * Asks for the name of a room or a device, starting from the one it has now, all of it selected so typing replaces it. Saving a blank name is going back to the
 * default, and when the user has already given a name there is a button for that too. The field stops at the longest name there is.
 */
@Composable
internal fun RenameDialog(@StringRes title: Int, current: String, hasCustomName: Boolean, onSave: (String) -> Unit, onUseDefault: () -> Unit, onDismiss: () -> Unit) {
  var text by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(current, TextRange(0, current.length))) }
  val focus = remember { FocusRequester() }
  LaunchedEffect(Unit) { focus.requestFocus() }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(title)) },
    text = {
      OutlinedTextField(
        value = text,
        onValueChange = { text = it.copy(text = it.text.take(Names.MAX_LENGTH)) },
        label = { Text(stringResource(R.string.rename_label)) },
        supportingText = { Text("${text.text.length}/${Names.MAX_LENGTH}") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSave(text.text) }),
        modifier = Modifier.focusRequester(focus),
      )
    },
    confirmButton = { TextButton(onClick = { onSave(text.text) }) { Text(stringResource(R.string.rename_save)) } },
    dismissButton = {
      Row {
        if (hasCustomName) TextButton(onClick = onUseDefault) { Text(stringResource(R.string.rename_default)) }
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.rename_cancel)) }
      }
    },
  )
}
