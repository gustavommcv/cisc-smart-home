package io.github.gustavommcv.ciscsmarthome.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing

/** A dashed line around [shape], for what is drawn as a place and is not there yet. */
internal fun Modifier.dashedBorder(color: Color, shape: Shape, width: Dp = 1.dp, dash: Dp = Spacing.S): Modifier =
  drawBehind {
    val stroke = Stroke(width = width.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), dash.toPx())))
    drawOutline(shape.createOutline(size, layoutDirection, this), color, style = stroke)
  }
