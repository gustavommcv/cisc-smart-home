#pragma once

#include <cmath>

#include "Time.h"

// Every threshold and delay of the domain lives here, so that calibrating the
// real sensors never means hunting through the rules.
namespace cisc {

struct GasSettings {
  // MQ-2 reading on the 0-1023 scale (not ppm). Strictly greater means gas.
  int threshold = 160;
  // After boot the sensor needs time to heat up; the valve stays closed.
  Millis warmUpMs = 10000;
  // Time without gas required before the button is accepted to rearm.
  Millis clearAirMs = 2000;
};

struct HumiditySettings {
  // The dehumidifier turns on at or above this value...
  float onAtPercent = 85.0f;
  // ...and turns off at or below this one. The gap avoids a chattering relay: the DHT11 reads whole
  // percents, so with a gap of one point the relay would follow every flicker between two readings.
  float offAtPercent = 83.0f;
};

// What the user may choose for the two limits above (they can be changed from the app).
namespace humidity_limits {
constexpr float kMinPercent = 40.0f;
constexpr float kMaxPercent = 95.0f;
constexpr float kStepPercent = 1.0f;
// The lower limit is at least this far below the upper one.
constexpr float kMinGapPercent = 2.0f;
}  // namespace humidity_limits

inline bool isWholeNumber(float value) {
  return std::isfinite(value) && std::floor(value) == value;
}

// Whole percents inside the range, with the lower limit far enough below the upper one.
inline bool isValid(const HumiditySettings& settings) {
  return isWholeNumber(settings.onAtPercent) && isWholeNumber(settings.offAtPercent) &&
         settings.onAtPercent <= humidity_limits::kMaxPercent && settings.offAtPercent >= humidity_limits::kMinPercent &&
         settings.onAtPercent - settings.offAtPercent >= humidity_limits::kMinGapPercent;
}

struct ButtonSettings {
  // A change must stay stable this long before it counts.
  Millis debounceMs = 35;
};

}  // namespace cisc
