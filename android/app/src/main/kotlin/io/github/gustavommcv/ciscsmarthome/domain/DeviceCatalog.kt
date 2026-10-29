package io.github.gustavommcv.ciscsmarthome.domain

/** What a sensor measures. The charts are chosen by it, not by the device the sensor is in, so a new device with a temperature sensor is charted as the others are. */
enum class SensorKind { Gas, Temperature, Humidity, Generic }

/** The icon a device is shown with. The screen draws it; the catalog only says which. */
enum class DeviceIcon { Flame, Drop, Sensor }

/** What a line over a chart is: the limit of the alarm, or what the dehumidifier turns on or off at. The screen has the words for each. */
enum class LimitKind { Alarm, DehumidifierOn, DehumidifierOff }

/** A line drawn over a chart: a value that is [Fixed], or one that is the value of an entity of the house, such as a limit the user can change. */
sealed interface LimitSpec {
  val kind: LimitKind

  data class Fixed(override val kind: LimitKind, val value: Double) : LimitSpec

  data class OfEntity(override val kind: LimitKind, val entity: EntityId) : LimitSpec
}

/** What a chart of a month or a year draws: the least and the greatest of each hour as a line, or the peak of each day as a bar. */
enum class LongPeriods { Envelope, DailyPeaks }

/** The range a chart shows: [atLeast] is always in it and grows if the readings go outside it, and it is never narrower than [minSpan]. */
data class ChartScale(val atLeast: ClosedFloatingPointRange<Double>? = null, val minSpan: Double? = null) {
  /** The range for readings from [dataMin] to [dataMax], with the lines of [limits] in it. */
  fun rangeOf(dataMin: Double, dataMax: Double, limits: List<Double> = emptyList()): ChartRange {
    var low = minOf(dataMin, atLeast?.start ?: dataMin)
    var high = maxOf(dataMax, atLeast?.endInclusive ?: dataMax)
    limits.forEach { limit ->
      low = minOf(low, limit)
      high = maxOf(high, limit)
    }
    if (minSpan != null && high - low < minSpan) {
      val middle = (low + high) / 2
      low = middle - minSpan / 2
      high = middle + minSpan / 2
    }
    return ChartRange(low, high)
  }
}

/** The values from [low] at the bottom of a chart to [high] at the top. */
data class ChartRange(val low: Double, val high: Double) {
  /** Where [value] is, from 0 at the bottom to 1 at the top; a range of one value has it in the middle. */
  fun fractionOf(value: Double): Float = if (high == low) 0.5f else ((value - low) / (high - low)).toFloat()
}

/** How the chart of a sensor looks: its [scale], the [limits] drawn over it, whether it is [fillBelow] the line, and what it draws for a month or a year. */
data class ChartProfile(
  val scale: ChartScale = ChartScale(),
  val limits: List<LimitSpec> = emptyList(),
  val fillBelow: Boolean = false,
  val longPeriods: LongPeriods = LongPeriods.Envelope,
)

/** A sensor the history is kept of: what it measures, what it reads in, and how it is charted. */
data class SensorProfile(val id: EntityId, val kind: SensorKind, val unit: String, val chart: ChartProfile)

/** What a device says under its name: an entity of it, and whether it says the word for it too ("Desumidificador desligado") or only its value ("Normal"). */
data class HeadlineSpec(val entity: EntityId, val withLabel: Boolean)

/** What the app knows of a device: the room it is in, its icon, what it says under its name, and the sensors it is charted by. */
data class DeviceProfile(val id: DeviceId, val areaId: AreaId, val icon: DeviceIcon, val headline: HeadlineSpec?, val sensors: List<SensorProfile>)

/**
 * Everything the app knows about the devices of the maquette, in one place. A device nobody knows is shown by what it says itself (its name, its
 * entities and their units) and charted as a line, so a new device works at once and only gets a better presentation when it is added here.
 * A new device is one entry of [devices], with the words for it in the resources of the screen.
 */
object DeviceCatalog {
  // The limit of the alarm of the firmware (Settings.h, GasSettings.threshold), on the same scale of 0 to 1023 that the sensor is read in.
  private const val GAS_ALARM_ADC = 160.0

  val devices: List<DeviceProfile> =
    listOf(
      DeviceProfile(
        id = DeviceIds.GasMonitor,
        areaId = AreaIds.Kitchen,
        icon = DeviceIcon.Flame,
        headline = HeadlineSpec(EntityIds.GasState, withLabel = false),
        sensors =
          listOf(
            SensorProfile(
              EntityIds.GasLevel,
              SensorKind.Gas,
              "adc",
              ChartProfile(
                scale = ChartScale(atLeast = 0.0..400.0),
                limits = listOf(LimitSpec.Fixed(LimitKind.Alarm, GAS_ALARM_ADC)),
                fillBelow = true,
                longPeriods = LongPeriods.DailyPeaks,
              ),
            ),
          ),
      ),
      DeviceProfile(
        id = DeviceIds.ClosetMonitor,
        areaId = AreaIds.Bedroom,
        icon = DeviceIcon.Drop,
        headline = HeadlineSpec(EntityIds.DehumidifierPower, withLabel = true),
        sensors =
          listOf(
            SensorProfile(EntityIds.Temperature, SensorKind.Temperature, "°C", ChartProfile(scale = ChartScale(minSpan = 10.0))),
            SensorProfile(
              EntityIds.Humidity,
              SensorKind.Humidity,
              "%",
              ChartProfile(
                scale = ChartScale(atLeast = 40.0..100.0),
                limits = listOf(LimitSpec.OfEntity(LimitKind.DehumidifierOn, EntityIds.DehumidifierOnAt), LimitSpec.OfEntity(LimitKind.DehumidifierOff, EntityIds.DehumidifierOffAt)),
              ),
            ),
          ),
      ),
    )

  fun of(id: DeviceId): DeviceProfile? = devices.firstOrNull { it.id == id }

  private fun sensor(id: EntityId): SensorProfile? = devices.firstNotNullOfOrNull { device -> device.sensors.firstOrNull { it.id == id } }

  /** How the chart of a sensor looks: the one of the catalog, or a plain line for a sensor nobody knows. */
  fun chartOf(id: EntityId): ChartProfile = sensor(id)?.chart ?: ChartProfile()

  fun kindOf(id: EntityId): SensorKind = sensor(id)?.kind ?: SensorKind.Generic

  /** The entities whose values are the limits drawn over some chart: they are the numbers the screen has to follow. */
  val limitEntities: Set<EntityId> = devices.flatMap { it.sensors }.flatMap { it.chart.limits }.filterIsInstance<LimitSpec.OfEntity>().map { it.entity }.toSet()
}
