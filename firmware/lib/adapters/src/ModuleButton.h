#pragma once

#include <cstdint>

#include "Ports.h"

namespace adapters {

// A push button, or a button module, on a digital pin. `activeHigh` says which
// level means "pressed"; the pin gets the matching pull resistor so that it
// never floats if the wire comes loose.
class ModuleButton : public cisc::Button {
 public:
  ModuleButton(std::uint8_t pin, bool activeHigh) : pin_(pin), activeHigh_(activeHigh) {}

  void begin();
  bool isPressed() override;

 private:
  std::uint8_t pin_;
  bool activeHigh_;
};

}  // namespace adapters
