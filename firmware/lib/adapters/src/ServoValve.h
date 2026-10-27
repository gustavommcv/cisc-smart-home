#pragma once

#include <ESP32Servo.h>

#include <cstdint>

#include "Ports.h"

namespace adapters {

// The gas valve, simulated by a hobby servo: "on" is open, "off" is closed.
class ServoValve : public cisc::Output {
 public:
  ServoValve(std::uint8_t pin, std::uint8_t openDegrees, std::uint8_t closedDegrees)
      : pin_(pin), openDegrees_(openDegrees), closedDegrees_(closedDegrees) {}

  void begin();
  void set(bool open) override;

 private:
  Servo servo_;
  std::uint8_t pin_;
  std::uint8_t openDegrees_;
  std::uint8_t closedDegrees_;
};

}  // namespace adapters
