#pragma once

#include <cmath>

namespace cisc {

// A DHT11 that fails to answer returns NaN, and a glitch can return nonsense.
// Only readings inside the sensor's physical range are trusted.
//
// Exactly 0 °C and 0 % together is rejected too: with a loose wire the sensor can
// answer all zeros with a matching checksum, and that looks like a valid reading.
inline bool isPlausibleClimate(float temperatureC, float humidityPercent) {
  if (std::isnan(temperatureC) || std::isnan(humidityPercent)) return false;
  if (temperatureC == 0.0f && humidityPercent == 0.0f) return false;
  return humidityPercent >= 0.0f && humidityPercent <= 100.0f && temperatureC >= -40.0f &&
         temperatureC <= 80.0f;
}

}  // namespace cisc
