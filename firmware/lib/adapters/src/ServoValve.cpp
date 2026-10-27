#include "ServoValve.h"

namespace adapters {

void ServoValve::begin() {
  // 16-bit PWM gives the same angle precision as the Arduino Servo library;
  // 544-2400 us is that library's default pulse range.
  servo_.setTimerWidth(16);
  servo_.attach(pin_, 544, 2400);
  servo_.write(closedDegrees_);
}

void ServoValve::set(bool open) {
  servo_.write(open ? openDegrees_ : closedDegrees_);
}

}  // namespace adapters
