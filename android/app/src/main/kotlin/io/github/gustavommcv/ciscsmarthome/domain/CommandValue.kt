package io.github.gustavommcv.ciscsmarthome.domain

/** What the user asks an entity to become. */
sealed interface CommandValue {
  /** For a switch: on or off. */
  data class Flag(val on: Boolean) : CommandValue

  /** For a select: one of its options. */
  data class Choice(val option: String) : CommandValue
}
