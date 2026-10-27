#include "GpioOutput.h"

#include <Arduino.h>

namespace adapters {

void GpioOutput::begin() {
  // Write the off level first, so the load never blinks on at boot.
  digitalWrite(pin_, activeHigh_ ? LOW : HIGH);
  pinMode(pin_, OUTPUT);
}

void GpioOutput::set(bool on) {
  digitalWrite(pin_, on == activeHigh_ ? HIGH : LOW);
}

}  // namespace adapters
