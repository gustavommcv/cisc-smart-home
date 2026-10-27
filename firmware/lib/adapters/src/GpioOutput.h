#pragma once

#include <cstdint>

#include "Ports.h"

namespace adapters {

// A digital output: the LED, the buzzer, the relay module. `activeHigh` says
// which level turns the load on (a relay module that switches on with LOW has
// it false). begin() leaves the output off.
class GpioOutput : public cisc::Output {
 public:
  GpioOutput(std::uint8_t pin, bool activeHigh) : pin_(pin), activeHigh_(activeHigh) {}

  void begin();
  void set(bool on) override;

 private:
  std::uint8_t pin_;
  bool activeHigh_;
};

}  // namespace adapters
