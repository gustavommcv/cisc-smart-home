#pragma once

#include <Wire.h>
#include <hd44780.h>
#include <hd44780ioClass/hd44780_I2Cexp.h>

#include <cstdint>

#include "Ports.h"

namespace adapters {

// A character LCD behind an I2C backpack (PCF8574). The library finds the
// backpack's address by itself. If no display answers, begin() says so and the
// rest of the house keeps working: show() simply does nothing.
class I2cLcdDisplay : public cisc::Display {
 public:
  I2cLcdDisplay(std::uint8_t sdaPin, std::uint8_t sclPin, std::uint8_t columns, std::uint8_t rows)
      : sdaPin_(sdaPin), sclPin_(sclPin), columns_(columns), rows_(rows) {}

  // Returns true when a display was found.
  bool begin();
  void show(const char* topLine, const char* bottomLine) override;

 private:
  void writeLine(std::uint8_t row, const char* text);

  hd44780_I2Cexp lcd_;
  std::uint8_t sdaPin_;
  std::uint8_t sclPin_;
  std::uint8_t columns_;
  std::uint8_t rows_;
  bool available_ = false;
};

}  // namespace adapters
