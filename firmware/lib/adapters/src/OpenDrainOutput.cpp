#include "OpenDrainOutput.h"

#include <Arduino.h>

namespace adapters {

void OpenDrainOutput::begin() {
  pinMode(pin_, OUTPUT_OPEN_DRAIN);
  digitalWrite(pin_, HIGH);  // in open-drain mode, HIGH means "released"
}

void OpenDrainOutput::set(bool on) {
  digitalWrite(pin_, on ? LOW : HIGH);
}

}  // namespace adapters
