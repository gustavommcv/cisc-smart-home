package io.github.gustavommcv.ciscsmarthome.domain

/** The readings of the board at one moment, by entity; null means the entity had no valid reading. */
data class HistorySample(val uptimeMs: Long, val values: Map<EntityId, Double?>)

/** Some stored samples of one boot, oldest first. */
data class HistoryPage(val bootId: BootId, val samples: List<HistorySample>)
