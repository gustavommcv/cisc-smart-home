#pragma once

#include <cstdint>

#include "ButtonDebouncer.h"
#include "GasGuard.h"
#include "HouseSettings.h"
#include "HouseSnapshot.h"
#include "HumidityController.h"
#include "Ports.h"
#include "RingBuffer.h"
#include "Sample.h"

namespace cisc {

// The ports the house is wired to. References: the hardware outlives the controller.
struct Hardware {
  Clock& clock;
  GasSensor& gasSensor;
  ClimateSensor& climateSensor;
  Button& button;
  Output& valve;
  Output& alarmLed;
  Output& buzzer;
  Output& dehumidifier;
  SettingsStore& settingsStore;
};

// Runs the house: reads the sensors at their own pace, hands the readings to the
// domain rules, drives the outputs and keeps the history. It also receives the
// commands from the app. It knows nothing about pins, Wi-Fi or HTTP.
//
// It holds the whole history (about 7 KB), so create it as a global, not on the stack.
class HouseController {
 public:
  using History = RingBuffer<Sample, kHistoryCapacity>;

  explicit HouseController(const Hardware& hardware, const HouseSettings& settings = HouseSettings{});

  // Call once at boot, then call tick() from the main loop as often as possible.
  void begin();
  void tick();

  // Commands from the app.
  // Turns the dehumidifier on or off and takes manual control.
  void turnDehumidifier(bool on);
  // Automatic hands control back to the humidity rule. Manual keeps the current state.
  void setDehumidifierMode(ControlMode mode);
  // Change the limits of the automatic mode. Each says whether the new value was allowed (a whole
  // percent in the range, the lower limit far enough below the upper one); if not, nothing
  // changes. A change is kept for the next time the board starts.
  bool setDehumidifierOnAt(float percent);
  bool setDehumidifierOffAt(float percent);
  // Silences the buzzer, or lets it sound again. Only the buzzer: the alarm LED, the valve and the alert of the
  // app are the same either way. It takes effect at once, in the middle of a leak too. It is not saved: after a
  // restart the buzzer is enabled, so a power cut never leaves the board mute without anyone knowing.
  void setBuzzerEnabled(bool enabled);

  HouseSnapshot snapshot() const;
  const History& history() const { return history_; }

 private:
  // Remembers what an output was last told, so hardware is touched only on a change.
  struct OutputCache {
    bool known = false;
    bool value = false;
  };

  bool changeLimits(float onAtPercent, float offAtPercent);
  void readClimate();
  void trackAlert(GasState before);
  void applyOutputs();
  static void drive(Output& output, OutputCache& cache, bool value);
  void recordSample(Millis now);

  Hardware hardware_;
  HouseSettings settings_;

  GasGuard gasGuard_;
  HumidityController humidity_;
  ButtonDebouncer button_;
  History history_;

  Millis now_ = 0;
  Millis lastGasAt_ = 0;
  Millis lastClimateAt_ = 0;
  Millis lastHistoryAt_ = 0;

  int gasReading_ = 0;
  ClimateReading climate_{false, 0.0f, 0.0f};
  bool climateSampled_ = false;
  std::uint32_t gasAlertCount_ = 0;
  bool buzzerEnabled_ = true;

  OutputCache valveOut_;
  OutputCache alarmLedOut_;
  OutputCache buzzerOut_;
  OutputCache dehumidifierOut_;
};

}  // namespace cisc
