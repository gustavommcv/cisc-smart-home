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

  HouseSnapshot snapshot() const;
  const History& history() const { return history_; }

 private:
  // Remembers what an output was last told, so hardware is touched only on a change.
  struct OutputCache {
    bool known = false;
    bool value = false;
  };

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

  OutputCache valveOut_;
  OutputCache alarmLedOut_;
  OutputCache buzzerOut_;
  OutputCache dehumidifierOut_;
};

}  // namespace cisc
