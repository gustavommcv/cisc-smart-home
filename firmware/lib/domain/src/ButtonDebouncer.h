#pragma once

#include "Settings.h"
#include "Time.h"

namespace cisc {

// Turns a bouncing "pressed" signal into one clean event per press.
class ButtonDebouncer {
 public:
  explicit ButtonDebouncer(const ButtonSettings& settings = ButtonSettings{})
      : settings_(settings) {}

  // Starts from the current level, so a button held at boot is not a press.
  void begin(bool pressed, Millis now);

  // Returns true exactly once per press, when the new level has been stable
  // for the debounce time. Holding or releasing never returns true.
  bool update(bool pressed, Millis now);

 private:
  ButtonSettings settings_;
  bool raw_ = false;
  bool stable_ = false;
  Millis changedAt_ = 0;
};

}  // namespace cisc
