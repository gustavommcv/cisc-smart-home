#pragma once

#include <cstdint>

#include "GasGuard.h"
#include "HumidityController.h"
#include "Time.h"

namespace cisc {

// Everything the outside world may want to know about the house, at one instant.
struct HouseSnapshot {
  Millis uptimeMs;

  int gasReading;  // 0-1023 scale
  GasState gasState;
  bool gasDetected;  // gas above the threshold right now
  bool valveOpen;
  bool rearmReady;  // the air has been clean long enough for the button to be accepted
  std::uint32_t gasAlertCount;  // leaks since boot; one per emergency

  bool climateSampled;  // false until the sensor has been read once, good or not
  bool climateValid;    // false when the last reading was missing or implausible
  float temperatureC;
  float humidityPercent;

  bool dehumidifierOn;
  ControlMode dehumidifierMode;
  float dehumidifierOnAt;   // the limits of the automatic mode, in percent
  float dehumidifierOffAt;

  bool buzzerEnabled;  // false while the user has silenced the buzzer; the LED, the valve and the alert do not depend on it
};

}  // namespace cisc
