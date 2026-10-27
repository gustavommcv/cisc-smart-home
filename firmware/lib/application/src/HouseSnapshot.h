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
  bool valveOpen;
  std::uint32_t gasAlertCount;  // leaks since boot; one per emergency

  bool climateValid;  // false until the first good reading
  float temperatureC;
  float humidityPercent;

  bool dehumidifierOn;
  ControlMode dehumidifierMode;
};

}  // namespace cisc
