package io.github.gustavommcv.ciscsmarthome.domain

/**
 * How the phone warns of a gas leak in a room. The board does its part whatever this says (the buzzer, the LED and the valve are its own): this is only about the phone.
 * The [key] is what is kept on the phone, so it never changes once an app has shipped with it.
 */
enum class LeakAlertMode(val key: String) {
  /** The alarm: a sound of its own, loud, that repeats until the warning is opened. */
  Alarm("alarm"),

  /** An ordinary notification, with the sound of notifications, that does not repeat. */
  Notice("notice"),

  /** Nothing: the phone does not warn. */
  Off("off");

  companion object {
    /** What a room has until the user chooses: a leak is a thing to wake someone up for. */
    val Default = Alarm

    fun fromKey(key: String): LeakAlertMode? = entries.firstOrNull { it.key == key }
  }
}

/** The ways the user chose, by room. A room with none has [LeakAlertMode.Default]. */
data class LeakAlertModes(val areas: Map<AreaId, LeakAlertMode> = emptyMap()) {
  fun of(id: AreaId): LeakAlertMode = areas[id] ?: LeakAlertMode.Default
}
