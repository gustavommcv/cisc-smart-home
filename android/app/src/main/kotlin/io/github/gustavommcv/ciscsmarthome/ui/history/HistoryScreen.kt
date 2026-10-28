package io.github.gustavommcv.ciscsmarthome.ui.history

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.gustavommcv.ciscsmarthome.R
import io.github.gustavommcv.ciscsmarthome.domain.EntityId
import io.github.gustavommcv.ciscsmarthome.ui.components.CiscCard
import io.github.gustavommcv.ciscsmarthome.ui.home.labelRes
import io.github.gustavommcv.ciscsmarthome.ui.theme.Spacing

/** The history tab: what the board read, as a chart. The history is on the phone, so it is here also when the board is not. */
@Composable
internal fun HistoryScreen(viewModel: HistoryViewModel, modifier: Modifier = Modifier) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  HistoryContent(state, onSelectSensor = viewModel::selectSensor, onChoosePeriod = viewModel::choosePeriod, modifier = modifier)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HistoryContent(state: HistoryState, onSelectSensor: (EntityId) -> Unit, onChoosePeriod: (ChartPeriod) -> Unit, modifier: Modifier = Modifier) {
  Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.Xl), verticalArrangement = Arrangement.spacedBy(Spacing.L)) {
    Text(stringResource(R.string.tab_history), style = MaterialTheme.typography.headlineLarge)
    // The chips wrap onto a second line when there are more than fit, so nothing scrolls sideways.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
      state.sensors.forEach { sensor -> FilterChip(selected = sensor.id == state.selected, onClick = { onSelectSensor(sensor.id) }, label = { Text(sensorLabel(sensor)) }) }
    }
    CiscCard { ChartBody(state) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.S)) {
      ChartPeriod.entries.forEach { period -> FilterChip(selected = period == state.period, onClick = { onChoosePeriod(period) }, label = { Text(stringResource(period.labelRes)) }) }
    }
    Text(stringResource(R.string.history_caption), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun ChartBody(state: HistoryState) {
  val resources = LocalResources.current
  val quiet = MaterialTheme.colorScheme.onSurfaceVariant
  when (val chart = state.chart) {
    ChartState.Loading -> Box(Modifier.fillMaxWidth().height(ChartHeight))
    ChartState.Empty ->
      Box(Modifier.fillMaxWidth().height(ChartHeight), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.bodyMedium, color = quiet, textAlign = TextAlign.Center)
      }
    is ChartState.Ready -> {
      val label = state.sensors.firstOrNull { it.id == state.selected }?.let { sensorLabel(it) }.orEmpty()
      LineChart(chart.chart, resources.chartDescription(label, state.period, chart.chart.min, chart.chart.max, chart.unit))
      Row(modifier = Modifier.fillMaxWidth().padding(top = Spacing.S), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(resources.sinceText(state.period), style = MaterialTheme.typography.bodySmall, color = quiet)
        Text(stringResource(R.string.history_now), style = MaterialTheme.typography.bodySmall, color = quiet)
      }
      Row(modifier = Modifier.fillMaxWidth().padding(top = Spacing.Xs), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(resources.minText(chart.chart.min, chart.unit), style = MaterialTheme.typography.bodyMedium)
        Text(resources.maxText(chart.chart.max, chart.unit), style = MaterialTheme.typography.bodyMedium)
      }
    }
  }
}

/** The name of a sensor: ours in Portuguese for the ones we know, the board's otherwise. */
@Composable
private fun sensorLabel(sensor: ChartSensor): String = sensor.id.labelRes()?.let { stringResource(it) } ?: sensor.name
