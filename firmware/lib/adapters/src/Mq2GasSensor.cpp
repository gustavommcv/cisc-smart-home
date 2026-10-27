#include "Mq2GasSensor.h"

#include <Arduino.h>

namespace adapters {

void Mq2GasSensor::begin() {
  pinMode(pin_, INPUT);
  // 0-3.3 V range: the widest the divider lets through.
  analogSetPinAttenuation(pin_, ADC_11db);
}

int Mq2GasSensor::read() {
  return cisc::mq2ReadingFromPinMillivolts(static_cast<float>(analogReadMilliVolts(pin_)), wiring_);
}

}  // namespace adapters
