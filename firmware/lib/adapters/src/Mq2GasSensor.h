#pragma once

#include <cstdint>

#include "Mq2Scale.h"
#include "Ports.h"

namespace adapters {

// MQ-2 module read through an ADC pin. The reading is converted to the 0-1023
// scale the gas rules use, undoing the voltage divider in front of the pin.
class Mq2GasSensor : public cisc::GasSensor {
 public:
  Mq2GasSensor(std::uint8_t pin, const cisc::Mq2Wiring& wiring) : pin_(pin), wiring_(wiring) {}

  void begin();
  int read() override;

 private:
  std::uint8_t pin_;
  cisc::Mq2Wiring wiring_;
};

}  // namespace adapters
