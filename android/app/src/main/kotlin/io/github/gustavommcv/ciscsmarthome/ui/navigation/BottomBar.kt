package io.github.gustavommcv.ciscsmarthome.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The bar at the bottom: where the app can go from anywhere. The tab that is open sits in a tinted pill, and its word is the strong one. */
@Composable
internal fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
  val scheme = MaterialTheme.colorScheme
  Column(modifier) {
    HorizontalDivider(thickness = Dp.Hairline, color = scheme.outlineVariant)
    NavigationBar(containerColor = scheme.surface, tonalElevation = 0.dp) {
      Tab.entries.forEach { tab ->
        NavigationBarItem(
          selected = tab == selected,
          onClick = { onSelect(tab) },
          icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
          label = { Text(stringResource(tab.labelRes)) },
          colors =
            NavigationBarItemDefaults.colors(
              selectedIconColor = scheme.onPrimaryContainer,
              selectedTextColor = scheme.onSurface,
              indicatorColor = scheme.primaryContainer,
              unselectedIconColor = scheme.onSurfaceVariant,
              unselectedTextColor = scheme.onSurfaceVariant,
            ),
        )
      }
    }
  }
}
