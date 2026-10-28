#include "HumidityController.h"

namespace cisc {

void HumidityController::update(float humidityPercent, bool readingValid) {
  if (mode_ == ControlMode::Manual) return;

  if (!readingValid) {
    on_ = false;
  } else if (humidityPercent >= settings_.onAtPercent) {
    on_ = true;
  } else if (humidityPercent <= settings_.offAtPercent) {
    on_ = false;
  }
}

bool HumidityController::setLimits(float onAtPercent, float offAtPercent) {
  const HumiditySettings candidate{onAtPercent, offAtPercent};
  if (!isValid(candidate)) return false;
  settings_ = candidate;
  return true;
}

void HumidityController::setManual(bool on) {
  mode_ = ControlMode::Manual;
  on_ = on;
}

}  // namespace cisc
