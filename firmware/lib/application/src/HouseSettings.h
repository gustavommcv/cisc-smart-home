#pragma once

#include <cstddef>

#include "Settings.h"
#include "Time.h"

namespace cisc {

// How many samples the history keeps: one every 10 s for an hour.
constexpr std::size_t kHistoryCapacity = 360;

struct HouseSettings {
  Millis gasIntervalMs = 50;
  Millis climateIntervalMs = 2000;  // the DHT11 cannot be read faster than this
  Millis historyIntervalMs = 10000;

  GasSettings gas;
  HumiditySettings humidity;
  ButtonSettings button;
};

}  // namespace cisc
