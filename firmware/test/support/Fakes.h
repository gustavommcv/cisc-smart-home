#pragma once

#include <cstdio>

#include "HouseController.h"
#include "Ports.h"

// Fake hardware for the application tests. Each fake records what the
// controller did to it, so a test can assert on behavior instead of internals.
namespace fakes {

struct Clock : cisc::Clock {
  cisc::Millis time = 0;
  cisc::Millis now() override { return time; }
};

struct GasSensor : cisc::GasSensor {
  int value = 50;
  int reads = 0;
  int read() override {
    ++reads;
    return value;
  }
};

struct ClimateSensor : cisc::ClimateSensor {
  cisc::ClimateReading reading{true, 21.5f, 60.0f};
  int reads = 0;
  cisc::ClimateReading read() override {
    ++reads;
    return reading;
  }
};

struct Button : cisc::Button {
  bool pressed = false;
  bool isPressed() override { return pressed; }
};

struct Output : cisc::Output {
  bool value = false;
  int writes = 0;
  void set(bool on) override {
    value = on;
    ++writes;
  }
};

struct Display : cisc::Display {
  char top[17] = "";
  char bottom[17] = "";
  int shows = 0;
  void show(const char* topLine, const char* bottomLine) override {
    std::snprintf(top, sizeof(top), "%s", topLine);
    std::snprintf(bottom, sizeof(bottom), "%s", bottomLine);
    ++shows;
  }
};

// The settings the user changed, kept in memory: what was saved, and what is there when the house starts.
struct SettingsStore : cisc::SettingsStore {
  bool has = false;
  cisc::HumiditySettings humidity;
  int saves = 0;
  bool loadHumidity(cisc::HumiditySettings& out) override {
    if (has) out = humidity;
    return has;
  }
  void saveHumidity(const cisc::HumiditySettings& settings) override {
    humidity = settings;
    has = true;
    ++saves;
  }
};

// Small numbers keep the timelines in the tests readable.
inline cisc::HouseSettings testSettings() {
  cisc::HouseSettings settings;
  settings.gasIntervalMs = 50;
  settings.climateIntervalMs = 500;
  settings.historyIntervalMs = 1000;
  settings.gas = cisc::GasSettings{/*threshold=*/100, /*warmUpMs=*/1000, /*clearAirMs=*/200};
  settings.humidity = cisc::HumiditySettings{/*onAtPercent=*/80.0f, /*offAtPercent=*/70.0f};
  settings.button = cisc::ButtonSettings{/*debounceMs=*/20};
  return settings;
}

// A whole house wired to fakes. run() advances the clock in 10 ms steps and
// ticks the controller, like the main loop would.
struct Rig {
  Clock clock;
  GasSensor gas;
  ClimateSensor climate;
  Button button;
  Output valve;
  Output alarmLed;
  Output buzzer;
  Output dehumidifier;
  SettingsStore store;
  cisc::HouseController controller;

  explicit Rig(const cisc::HouseSettings& settings = testSettings())
      : controller(cisc::Hardware{clock, gas, climate, button, valve, alarmLed, buzzer, dehumidifier, store},
                   settings) {}

  void run(cisc::Millis durationMs) {
    for (cisc::Millis elapsed = 0; elapsed < durationMs; elapsed += 10) {
      clock.time += 10;
      controller.tick();
    }
  }

  // Boots the house and lets it reach NORMAL in clean air.
  void bootToNormal() {
    controller.begin();
    run(1000);
  }
};

}  // namespace fakes
