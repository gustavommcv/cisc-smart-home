package io.github.gustavommcv.ciscsmarthome.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.DeviceCatalog
import io.github.gustavommcv.ciscsmarthome.domain.DeviceId
import io.github.gustavommcv.ciscsmarthome.domain.LimitKind
import io.github.gustavommcv.ciscsmarthome.ui.components.CiscCard
import io.github.gustavommcv.ciscsmarthome.ui.components.IconBadge
import io.github.gustavommcv.ciscsmarthome.ui.components.Tone
import io.github.gustavommcv.ciscsmarthome.ui.home.deviceName
import io.github.gustavommcv.ciscsmarthome.ui.home.iconRes
import io.github.gustavommcv.ciscsmarthome.ui.home.labelRes
import io.github.gustavommcv.ciscsmarthome.ui.home.roomName
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing
import java.time.ZoneId

/**
 * The history tab: first the devices that have a history, then, in the one that was touched, a chart for each of its sensors, and for the sensors
 * that are looked at together, one over the other. The history is on the phone, so it is here also when the board is not.
 */
@Composable
internal fun HistoryScreen(viewModel: HistoryViewModel, modifier: Modifier = Modifier) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  // Back closes the device and shows the list again; from the list it is the app's, which goes home.
  BackHandler(enabled = state.open != null) { viewModel.back() }
  HistoryContent(
    state,
    onOpenDevice = viewModel::openDevice,
    onBack = { viewModel.back() },
    onChoosePeriod = viewModel::choosePeriod,
    onEarlier = viewModel::earlier,
    onLater = viewModel::later,
    modifier = modifier,
  )
}

@Composable
internal fun HistoryContent(
  state: HistoryState,
  onOpenDevice: (DeviceId) -> Unit,
  onBack: () -> Unit,
  onChoosePeriod: (ChartPeriod) -> Unit,
  onEarlier: () -> Unit,
  onLater: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val open = state.open
  if (open == null) DeviceList(state, onOpenDevice, modifier) else DeviceCharts(state, open, onBack, onChoosePeriod, onEarlier, onLater, modifier)
}

@Composable
private fun DeviceList(state: HistoryState, onOpenDevice: (DeviceId) -> Unit, modifier: Modifier) {
  val resources = LocalResources.current
  Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.Xl), verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
    Text(stringResource(R.string.tab_history), style = MaterialTheme.typography.headlineLarge)
    Text(stringResource(R.string.history_pick), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (state.devices.isEmpty()) Text(stringResource(R.string.history_no_devices), style = MaterialTheme.typography.bodyMedium)
    state.devices.forEach { device ->
      CiscCard(onClick = { onOpenDevice(device.id) }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.M)) {
          IconBadge(state.icons.of(device.areaId).iconRes(), Tone.Primary)
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.Xs)) {
            Text(deviceName(device.id, device.name, state.names), style = MaterialTheme.typography.titleMedium)
            Text(
              resources.deviceSubtitle(roomName(device.areaId, device.areaName, state.names), device.charts.size),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DeviceCharts(state: HistoryState, device: HistoryDevice, onBack: () -> Unit, onChoosePeriod: (ChartPeriod) -> Unit, onEarlier: () -> Unit, onLater: () -> Unit, modifier: Modifier) {
  Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
    Row(modifier = Modifier.padding(horizontal = Spacing.S, vertical = Spacing.S), verticalAlignment = Alignment.CenterVertically) {
      IconButton(onClick = onBack) { Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back)) }
      Column {
        Text(deviceName(device.id, device.name, state.names), style = MaterialTheme.typography.headlineSmall)
        Text(roomName(device.areaId, device.areaName, state.names), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Column(modifier = Modifier.padding(horizontal = Spacing.Xl), verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
      FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
        ChartPeriod.entries.forEach { period -> FilterChip(selected = period == state.period, onClick = { onChoosePeriod(period) }, label = { Text(stringResource(period.labelRes)) }) }
      }
      // A month and a year have pages: the one before and the one after, with the name of the one that is shown between the arrows.
      if (state.period.isCalendar) PageBar(state, onEarlier, onLater)
      when (val charts = state.charts) {
        // A card of the height of a chart while the history is read, so nothing jumps when it comes.
        ChartsState.Loading -> device.charts.forEach { spec -> CiscCard { Box(Modifier.fillMaxWidth().height(if (spec is ChartSpec.Compare) BandHeight * 2 else ChartHeight)) } }
        is ChartsState.Ready -> charts.panels.forEach { panel -> PanelCard(panel, state.window, state.zone) }
      }
      Text(
        stringResource(
          when {
            !state.period.isCalendar -> R.string.history_caption
            (state.charts as? ChartsState.Ready)?.panels?.any { panel -> panel.lanes.any { it.bars != null } } == true -> R.string.history_caption_days
            else -> R.string.history_caption_hours
          },
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

/** The arrows to the page before and the page after, and the name of the month or the year that is shown. An arrow that has nowhere to go is faint and does nothing. */
@Composable
private fun PageBar(state: HistoryState, onEarlier: () -> Unit, onLater: () -> Unit) {
  val resources = LocalResources.current
  val year = state.period == ChartPeriod.Year
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    IconButton(onClick = onEarlier, enabled = state.canGoEarlier) {
      Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = stringResource(if (year) R.string.history_year_before else R.string.history_month_before), modifier = Modifier.rotate(180f))
    }
    Text(resources.windowTitle(state.window, state.zone), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
    IconButton(onClick = onLater, enabled = state.canGoLater) {
      Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = stringResource(if (year) R.string.history_year_after else R.string.history_month_after))
    }
  }
}

/** One chart: its title, a band for each sensor it draws, and under them the two ends of the window. */
@Composable
private fun PanelCard(panel: ChartPanel, window: ChartWindow, zone: ZoneId) {
  val resources = LocalResources.current
  val quiet = MaterialTheme.colorScheme.onSurfaceVariant
  val compare = panel.spec is ChartSpec.Compare
  CiscCard {
    Text(panelTitle(panel.spec), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = Spacing.S))
    panel.lanes.forEachIndexed { index, lane ->
      LaneBody(lane, laneColor(index), window, zone, showName = compare, height = if (compare) BandHeight else ChartHeight)
    }
    if (!panel.isEmpty) {
      Row(modifier = Modifier.fillMaxWidth().padding(top = Spacing.S), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(resources.windowStartText(window, zone), style = MaterialTheme.typography.bodySmall, color = quiet)
        Text(resources.windowEndText(window, zone), style = MaterialTheme.typography.bodySmall, color = quiet)
      }
    }
  }
}

@Composable
private fun LaneBody(lane: Lane, color: Color, window: ChartWindow, zone: ZoneId, showName: Boolean, height: Dp) {
  val resources = LocalResources.current
  val quiet = MaterialTheme.colorScheme.onSurfaceVariant
  val label = sensorLabel(lane.sensor)
  // The name of a band and the swatch of its line tell the two apart without the color alone.
  if (showName) {
    Row(modifier = Modifier.padding(top = Spacing.S), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
      Box(Modifier.size(Swatch).background(color, CircleShape))
      Text(label, style = MaterialTheme.typography.bodyMedium, color = quiet)
    }
  }
  val chart = lane.chart
  val bars = lane.bars
  val unit = lane.sensor.unit
  when {
    bars != null -> {
      val limits = lane.limits.map { LimitLine(bars.range.fractionOf(it.value), resources.limitText(it), it.kind) }
      BarChart(
        bars,
        resources.chartDescription(label, window, zone, bars.min, bars.max, unit),
        bar = color,
        height = height,
        limits = limits,
        limitValue = lane.limits.firstOrNull { it.kind == LimitKind.Alarm }?.value,
        unit = unit,
      )
      MinMaxRow(resources.minText(bars.min, unit), resources.maxText(bars.max, unit))
    }
    chart != null -> {
      val limits = lane.limits.map { LimitLine(chart.range.fractionOf(it.value), resources.limitText(it), it.kind) }
      LineChart(
        chart,
        resources.chartDescription(label, window, zone, chart.min, chart.max, unit),
        line = color,
        height = height,
        limits = limits,
        fillBelow = DeviceCatalog.chartOf(lane.sensor.id).fillBelow,
        unit = unit,
      )
      MinMaxRow(resources.minText(chart.min, unit), resources.maxText(chart.max, unit))
    }
    else ->
      Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyMedium, color = quiet, textAlign = TextAlign.Center)
      }
  }
}

@Composable
private fun MinMaxRow(least: String, greatest: String) {
  Row(modifier = Modifier.fillMaxWidth().padding(top = Spacing.Xs), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(least, style = MaterialTheme.typography.bodyMedium)
    Text(greatest, style = MaterialTheme.typography.bodyMedium)
  }
}

private val Swatch = 10.dp

// The first band is in the color of the app and the second in the amber, so the two lines of a comparison are not the same.
@Composable
private fun laneColor(index: Int): Color = if (index == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary

@Composable
private fun panelTitle(spec: ChartSpec): String =
  when (spec) {
    is ChartSpec.Single -> sensorLabel(spec.sensor)
    is ChartSpec.Compare -> LocalResources.current.compareTitle(sensorLabel(spec.upper), sensorLabel(spec.lower))
  }

/** The name of a sensor: ours in Portuguese for the ones we know, the board's otherwise. */
@Composable
private fun sensorLabel(sensor: ChartSensor): String = sensor.id.labelRes()?.let { stringResource(it) } ?: sensor.name
