#pragma once

#include "GasGuard.h"
#include "Time.h"

namespace cisc {

// One reading of the house, as kept in the history.
struct Sample {
  Millis timestampMs;     // time since boot; the app converts it to wall-clock time
  int gasReading;         // MQ-2 reading, 0-1023 scale
  float temperatureC;     // meaningless when !climateValid
  float humidityPercent;  // meaningless when !climateValid
  bool climateValid;      // false when the DHT11 failed to answer
  GasState gasState;      // what the gas guard was doing when the sample was taken
  bool dehumidifierOn;    // whether the dehumidifier was running
};

}  // namespace cisc
