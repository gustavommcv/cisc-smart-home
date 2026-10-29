#include "HouseController.h"

namespace cisc {

HouseController::HouseController(const Hardware& hardware, const HouseSettings& settings)
    : hardware_(hardware),
      settings_(settings),
      gasGuard_(settings.gas),
      humidity_(settings.humidity),
      button_(settings.button) {}

void HouseController::begin() {
  now_ = hardware_.clock.now();
  lastGasAt_ = now_;
  lastClimateAt_ = now_;
  lastHistoryAt_ = now_;

  // What the user saved comes before what the board was built with; a saved value that is no longer allowed is left out.
  HumiditySettings saved;
  if (hardware_.settingsStore.loadHumidity(saved)) humidity_.setLimits(saved.onAtPercent, saved.offAtPercent);

  gasGuard_.begin(now_);
  button_.begin(hardware_.button.isPressed(), now_);

  gasReading_ = hardware_.gasSensor.read();
  gasGuard_.update(gasReading_, false, now_);
  trackAlert(GasState::Starting);

  applyOutputs();
}

void HouseController::tick() {
  now_ = hardware_.clock.now();

  if (hasElapsed(now_, lastGasAt_, settings_.gasIntervalMs)) {
    lastGasAt_ = now_;
    gasReading_ = hardware_.gasSensor.read();
  }

  const bool buttonClicked = button_.update(hardware_.button.isPressed(), now_);
  const GasState before = gasGuard_.state();
  gasGuard_.update(gasReading_, buttonClicked, now_);
  trackAlert(before);

  if (hasElapsed(now_, lastClimateAt_, settings_.climateIntervalMs)) {
    lastClimateAt_ = now_;
    readClimate();
  }

  applyOutputs();

  if (hasElapsed(now_, lastHistoryAt_, settings_.historyIntervalMs)) {
    lastHistoryAt_ = now_;
    recordSample(now_);
  }
}

void HouseController::turnDehumidifier(bool on) {
  humidity_.setManual(on);
  applyOutputs();
}

void HouseController::setDehumidifierMode(ControlMode mode) {
  if (mode == ControlMode::Automatic) {
    humidity_.setAutomatic();
  } else {
    humidity_.setManual(humidity_.dehumidifierOn());
  }
}

void HouseController::setBuzzerEnabled(bool enabled) {
  buzzerEnabled_ = enabled;
  applyOutputs();
}

bool HouseController::setDehumidifierOnAt(float percent) {
  return changeLimits(percent, humidity_.limits().offAtPercent);
}

bool HouseController::setDehumidifierOffAt(float percent) {
  return changeLimits(humidity_.limits().onAtPercent, percent);
}

bool HouseController::changeLimits(float onAtPercent, float offAtPercent) {
  if (!humidity_.setLimits(onAtPercent, offAtPercent)) return false;
  hardware_.settingsStore.saveHumidity(humidity_.limits());
  return true;
}

HouseSnapshot HouseController::snapshot() const {
  return HouseSnapshot{
      now_,
      gasReading_,
      gasGuard_.state(),
      gasGuard_.gasDetected(),
      gasGuard_.valveOpen(),
      gasGuard_.canRearm(now_),
      gasAlertCount_,
      climateSampled_,
      climate_.valid,
      climate_.temperatureC,
      climate_.humidityPercent,
      humidity_.dehumidifierOn(),
      humidity_.mode(),
      humidity_.limits().onAtPercent,
      humidity_.limits().offAtPercent,
      buzzerEnabled_,
      settings_.gas.threshold,
  };
}

void HouseController::readClimate() {
  climate_ = hardware_.climateSensor.read();
  climateSampled_ = true;
  humidity_.update(climate_.humidityPercent, climate_.valid);
}

// Every way into an emergency is a leak, so each entry counts as one alert.
void HouseController::trackAlert(GasState before) {
  if (before != GasState::Emergency && gasGuard_.state() == GasState::Emergency) {
    ++gasAlertCount_;
  }
}

void HouseController::applyOutputs() {
  drive(hardware_.valve, valveOut_, gasGuard_.valveOpen());
  drive(hardware_.alarmLed, alarmLedOut_, gasGuard_.alarmLedOn());
  drive(hardware_.buzzer, buzzerOut_, gasGuard_.buzzerOn() && buzzerEnabled_);
  drive(hardware_.dehumidifier, dehumidifierOut_, humidity_.dehumidifierOn());
}

void HouseController::drive(Output& output, OutputCache& cache, bool value) {
  if (cache.known && cache.value == value) return;
  output.set(value);
  cache.known = true;
  cache.value = value;
}

void HouseController::recordSample(Millis now) {
  history_.push(Sample{now, gasReading_, climate_.temperatureC, climate_.humidityPercent, climate_.valid});
}

}  // namespace cisc
