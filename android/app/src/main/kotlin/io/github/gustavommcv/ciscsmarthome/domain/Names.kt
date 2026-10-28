package io.github.gustavommcv.ciscsmarthome.domain

/**
 * The names the user gave to rooms and devices, by id. What has no name here keeps its default: the name shows as what the user gave, else the
 * app's own in Portuguese (for the rooms and devices of the maquette), else what the board calls it.
 */
data class Names(val areas: Map<AreaId, String> = emptyMap(), val devices: Map<DeviceId, String> = emptyMap()) {
  fun nameOfArea(id: AreaId, portuguese: String?, board: String): String = areas[id] ?: portuguese ?: board

  fun nameOfDevice(id: DeviceId, portuguese: String?, board: String): String = devices[id] ?: portuguese ?: board

  companion object {
    /** Enough for a name and short enough for a card and a notification. */
    const val MAX_LENGTH = 30

    /** What was typed as a name, or null when it is no name: without the space around it, cut at [MAX_LENGTH], and nothing is no name. */
    fun clean(raw: String): String? = raw.trim().take(MAX_LENGTH).trim().ifEmpty { null }
  }
}
