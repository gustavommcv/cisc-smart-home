package io.github.gustavommcv.ciscsmarthome.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.ui.theme.CiscTheme

/** A stand-in for the first screen, until the home with the rooms replaces it. */
@Composable
fun HomePlaceholder(modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineLarge)
    Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.home_under_construction), style = MaterialTheme.typography.bodyMedium)
  }
}

@Preview(showBackground = true)
@Composable
private fun HomePlaceholderPreview() {
  CiscTheme { HomePlaceholder() }
}
