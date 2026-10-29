#pragma once

#include <cstdint>

#include "Settings.h"
#include "Time.h"

namespace cisc {

// One byte, so a history sample that carries it does not grow.
enum class GasState : std::uint8_t { Starting, Normal, Emergency };

// Decides what the gas valve, alarm LED and buzzer do. Pure logic: it is fed
// readings and the time, and knows nothing about pins.
//
// STARTING  -> NORMAL     after the warm-up and a stretch of clean air
// any state -> EMERGENCY  as soon as a reading is above the threshold
// EMERGENCY -> NORMAL     only when the button is pressed after enough clean air
//                         (or STARTING, if the warm-up has not finished yet)
class GasGuard {
 public:
  explicit GasGuard(const GasSettings& settings = GasSettings{}) : settings_(settings) {}

  void begin(Millis now);
  void update(int reading, bool rearmPressed, Millis now);

  GasState state() const { return state_; }
  bool gasDetected() const { return gasDetected_; }
  bool valveOpen() const { return state_ == GasState::Normal; }
  bool alarmLedOn() const { return state_ == GasState::Emergency; }
  // Sounds only while gas is actually present, not while the alarm is latched.
  bool buzzerOn() const { return state_ == GasState::Emergency && gasDetected_; }
  // True once the air has been clean long enough for the button to be accepted.
  bool canRearm(Millis now) const;

 private:
  bool warmUpDone(Millis now) const { return hasElapsed(now, startedAt_, settings_.warmUpMs); }

  GasSettings settings_;
  GasState state_ = GasState::Starting;
  bool gasDetected_ = false;
  bool countingCleanAir_ = false;
  Millis startedAt_ = 0;
  Millis cleanSince_ = 0;
};

}  // namespace cisc
