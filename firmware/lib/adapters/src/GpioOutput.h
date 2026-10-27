#pragma once

#include <cstdint>

#include "Ports.h"

namespace adapters {

// A digital output that drives the pin both ways: the LED and the buzzer.
// `activeHigh` says which level turns the load on. begin() leaves the output off.
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
