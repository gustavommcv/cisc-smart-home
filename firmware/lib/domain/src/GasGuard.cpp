#include "GasGuard.h"

namespace cisc {

bool GasGuard::setThreshold(float value) {
  if (!isWholeNumber(value) || value < gas_limits::kMin || value > gas_limits::kMax) return false;
  settings_.threshold = static_cast<int>(value);
  // A different limit starts a new clean-air observation on the next update.
  countingCleanAir_ = false;
  return true;
}

void GasGuard::begin(Millis now) {
  state_ = GasState::Starting;
  gasDetected_ = false;
  countingCleanAir_ = false;
  startedAt_ = now;
  cleanSince_ = now;
}

bool GasGuard::canRearm(Millis now) const {
  return countingCleanAir_ && !gasDetected_ && hasElapsed(now, cleanSince_, settings_.clearAirMs);
}

void GasGuard::update(int reading, bool rearmPressed, Millis now) {
  gasDetected_ = reading > settings_.threshold;
  if (gasDetected_) {
    countingCleanAir_ = false;
    state_ = GasState::Emergency;
    return;
  }

  if (!countingCleanAir_) {
    countingCleanAir_ = true;
    cleanSince_ = now;
  }

  if (state_ == GasState::Emergency) {
    if (rearmPressed && canRearm(now)) {
      state_ = warmUpDone(now) ? GasState::Normal : GasState::Starting;
    }
  } else if (state_ == GasState::Starting && warmUpDone(now) && canRearm(now)) {
    state_ = GasState::Normal;
  }
}

}  // namespace cisc
