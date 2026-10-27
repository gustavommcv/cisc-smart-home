#pragma once

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
  // ...and turns off at or below this one. The gap avoids a chattering relay.
  float offAtPercent = 84.0f;
};

struct ButtonSettings {
  // A change must stay stable this long before it counts.
  Millis debounceMs = 35;
};

}  // namespace cisc
