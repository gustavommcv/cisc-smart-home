package io.github.gustavommcv.ciscsmarthome.domain

/**
 * The readings of the board at one moment, by entity; null means the entity had no valid reading. [states] is what the board was doing then, by
 * entity, as the text the entity has in the home ("emergency", "true"); a board that does not say has none.
 */
data class HistorySample(val uptimeMs: Long, val values: Map<EntityId, Double?>, val states: Map<EntityId, String> = emptyMap())

/** Some stored samples of one boot, oldest first. */
data class HistoryPage(val bootId: BootId, val samples: List<HistorySample>)
