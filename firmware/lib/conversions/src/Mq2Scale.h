#pragma once

namespace cisc {

// How the MQ-2 module's analog output reaches the board's pin.
//
// The module runs on 5 V and its output can reach 5 V, but the ESP32 pin accepts
// at most 3.3 V. A divider (upper resistor in series, lower resistor to GND)
// brings it down; this undoes it.
struct Mq2Wiring {
  float upperOhms = 10000.0f;
  float lowerOhms = 20000.0f;  // must not be zero
  float supplyMillivolts = 5000.0f;
};

// Turns the voltage measured at the pin into the 0-1023 scale the gas rules use,
// which is the scale of the original Arduino sketch. Out-of-range values are clamped.
inline int mq2ReadingFromPinMillivolts(float pinMillivolts, const Mq2Wiring& wiring = Mq2Wiring{}) {
  const float sensorMillivolts = pinMillivolts * (wiring.upperOhms + wiring.lowerOhms) / wiring.lowerOhms;
  const float reading = sensorMillivolts * 1023.0f / wiring.supplyMillivolts;
  if (reading <= 0.0f) return 0;
  if (reading >= 1023.0f) return 1023;
  return static_cast<int>(reading + 0.5f);
}

}  // namespace cisc
