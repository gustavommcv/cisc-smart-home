#include "GpioOutput.h"

#include <Arduino.h>

namespace adapters {

void GpioOutput::begin() {
  // The ESP32 core ignores digitalWrite() on a pin that is not an output yet,
  // so the pin is configured first and then put in its off level.
  pinMode(pin_, OUTPUT);
  digitalWrite(pin_, activeHigh_ ? LOW : HIGH);
}

void GpioOutput::set(bool on) {
  digitalWrite(pin_, on == activeHigh_ ? HIGH : LOW);
}

}  // namespace adapters
