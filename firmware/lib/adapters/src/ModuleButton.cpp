#include "ModuleButton.h"

#include <Arduino.h>

namespace adapters {

void ModuleButton::begin() {
  pinMode(pin_, activeHigh_ ? INPUT_PULLDOWN : INPUT_PULLUP);
}

bool ModuleButton::isPressed() {
  return digitalRead(pin_) == (activeHigh_ ? HIGH : LOW);
}

}  // namespace adapters
