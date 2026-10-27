#pragma once

#include <cstdint>

#include "Ports.h"

namespace adapters {

// An output that only ever pulls the line to GND, or lets it go. "On" pulls the
// pin LOW; "off" releases it, so it floats and the load's own pull-up decides.
//
// This is how a relay module that switches on with LOW must be driven from a
// 3.3 V board: driving the pin HIGH leaves the module's 5 V input stage half
// open, and a relay that was on can stay on. begin() leaves the output off.
class OpenDrainOutput : public cisc::Output {
 public:
  explicit OpenDrainOutput(std::uint8_t pin) : pin_(pin) {}

  void begin();
  void set(bool on) override;

 private:
  std::uint8_t pin_;
};

}  // namespace adapters
