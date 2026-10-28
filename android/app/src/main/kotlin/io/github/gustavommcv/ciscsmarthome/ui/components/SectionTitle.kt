package io.github.gustavommcv.ciscsmarthome.ui.components

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

/** The small title that opens a group of cards ("Cômodos", "Dispositivos", "Tema"). */
@Composable
internal fun SectionTitle(@StringRes text: Int, modifier: Modifier = Modifier) {
  Text(stringResource(text), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}
