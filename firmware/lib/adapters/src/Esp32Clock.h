#pragma once

#include <Arduino.h>

#include "Ports.h"

namespace adapters {

class Esp32Clock : public cisc::Clock {
 public:
  cisc::Millis now() override { return millis(); }
};

}  // namespace adapters
