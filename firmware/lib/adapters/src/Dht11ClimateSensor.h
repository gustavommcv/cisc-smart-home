#pragma once

#include <DHT.h>

#include <cstdint>

#include "Ports.h"

namespace adapters {

// DHT11 temperature and humidity sensor. The read blocks for a few
// milliseconds, so it must not be called more than every two seconds.
class Dht11ClimateSensor : public cisc::ClimateSensor {
 public:
  explicit Dht11ClimateSensor(std::uint8_t pin) : sensor_(pin, DHT11) {}

  void begin() { sensor_.begin(); }
  cisc::ClimateReading read() override;

 private:
  DHT sensor_;
};

}  // namespace adapters
