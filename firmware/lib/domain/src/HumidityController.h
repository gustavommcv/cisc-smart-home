#pragma once

#include "Settings.h"

namespace cisc {

enum class ControlMode { Automatic, Manual };

// Decides whether the dehumidifier is on.
//
// Automatic: follows the humidity, with two limits so the relay does not chatter.
// Manual:    the last command wins and the humidity is ignored.
class HumidityController {
 public:
  explicit HumidityController(const HumiditySettings& settings = HumiditySettings{})
      : settings_(settings) {}

  // Feeds a reading. Only has an effect in Automatic mode.
  void update(float humidityPercent, bool readingValid);

  // Hands control back to the rules. The dehumidifier keeps its current state
  // until the next reading arrives.
  void setAutomatic() { mode_ = ControlMode::Automatic; }

  // Takes control and turns the dehumidifier on or off.
  void setManual(bool on);

  // Changes the two limits of the automatic mode, when they are allowed (see isValid in Settings.h).
  // Says whether they were; if not, nothing changes. The mode and the state are left alone: the
  // new limits are the ones the next reading is judged by.
  bool setLimits(float onAtPercent, float offAtPercent);
  const HumiditySettings& limits() const { return settings_; }

  bool dehumidifierOn() const { return on_; }
  ControlMode mode() const { return mode_; }

 private:
  HumiditySettings settings_;
  ControlMode mode_ = ControlMode::Automatic;
  bool on_ = false;
};

}  // namespace cisc
