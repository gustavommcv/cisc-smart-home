#include "I2cLcdDisplay.h"

namespace adapters {

bool I2cLcdDisplay::begin() {
  Wire.begin(sdaPin_, sclPin_);
  // A bad cable must not hold up the control loop: give up on a transfer after 25 ms.
  Wire.setTimeOut(25);
  available_ = lcd_.begin(columns_, rows_) == 0;
  return available_;
}

void I2cLcdDisplay::show(const char* topLine, const char* bottomLine) {
  if (!available_) return;
  writeLine(0, topLine);
  if (rows_ > 1) writeLine(1, bottomLine);
}

// Always writes the full width, so a shorter message wipes the previous one.
void I2cLcdDisplay::writeLine(std::uint8_t row, const char* text) {
  lcd_.setCursor(0, row);
  std::uint8_t column = 0;
  for (; column < columns_ && text[column] != '\0'; ++column) lcd_.write(text[column]);
  for (; column < columns_; ++column) lcd_.write(' ');
}

}  // namespace adapters
