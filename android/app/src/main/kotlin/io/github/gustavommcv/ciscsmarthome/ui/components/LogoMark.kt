package io.github.gustavommcv.ciscsmarthome.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.gustavommcv.ciscsmarthome.R

private val LogoSize = 48.dp

/** The logo of the app, the same drawing as its icon (a house with a shield and a spark, on a rounded square). It is decoration: the name is written beside it. */
@Composable
internal fun LogoMark(modifier: Modifier = Modifier) {
  Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = modifier.size(LogoSize))
}
