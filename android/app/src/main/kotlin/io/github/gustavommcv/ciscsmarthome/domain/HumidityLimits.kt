package io.github.gustavommcv.ciscsmarthome.domain

/**
 * The two limits of the dehumidifier, "turn on at" and "turn off at", as one thing. The board decides what it accepts (whole numbers in the range of each, the
 * lower limit at least [MIN_GAP] below the upper one) and refuses the rest; this says it beforehand, so the controls offer only the steps that will be accepted,
 * and gives the way back to the default in an order that never breaks the gap on the way.
 */
class HumidityLimits private constructor(private val on: Entity.Number, private val off: Entity.Number, private val onValue: Double, private val offValue: Double) {
  /** The value each limit will have after a step up or down, or null when the board would not accept it. */
  fun raiseOn(): Double? = (onValue + on.step).takeIf { it <= on.max }

  fun lowerOn(): Double? = (onValue - on.step).takeIf { it >= maxOf(on.min, offValue + MIN_GAP) }

  fun raiseOff(): Double? = (offValue + off.step).takeIf { it <= minOf(off.max, onValue - MIN_GAP) }

  fun lowerOff(): Double? = (offValue - off.step).takeIf { it >= off.min }

  /** Whether both are what the board has when nobody changed them (and, when the board does not say, they are: there is nothing to go back to). */
  val isDefault: Boolean get() = restore().isEmpty()

  /**
   * The commands that take both limits back to the default, in an order the board accepts at each step. Each limit that is already there is left out. When both
   * have to move, the upper one goes first if the new upper limit is far enough from the lower limit as it is now; otherwise the lower one goes first, which then
   * always works.
   */
  fun restore(): List<Pair<EntityId, Double>> {
    val targetOn = on.default?.takeIf { it != onValue }
    val targetOff = off.default?.takeIf { it != offValue }
    return when {
      targetOn != null && targetOff != null ->
        if (targetOn >= offValue + MIN_GAP) listOf(on.id to targetOn, off.id to targetOff) else listOf(off.id to targetOff, on.id to targetOn)
      targetOn != null -> listOf(on.id to targetOn)
      targetOff != null -> listOf(off.id to targetOff)
      else -> emptyList()
    }
  }

  companion object {
    /** How far the lower limit must stay below the upper one: the rule of docs/api.md, "Regras do desumidificador". */
    const val MIN_GAP = 2.0

    /** The limits of a device, or null when it does not have both, with a value each. */
    fun of(entities: List<Entity>): HumidityLimits? {
      val on = entities.firstOrNull { it.id == EntityIds.DehumidifierOnAt } as? Entity.Number ?: return null
      val off = entities.firstOrNull { it.id == EntityIds.DehumidifierOffAt } as? Entity.Number ?: return null
      return HumidityLimits(on, off, on.value ?: return null, off.value ?: return null)
    }
  }
}
