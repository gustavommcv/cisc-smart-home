#include "ButtonDebouncer.h"

namespace cisc {

void ButtonDebouncer::begin(bool pressed, Millis now) {
  raw_ = pressed;
  stable_ = pressed;
  changedAt_ = now;
}

bool ButtonDebouncer::update(bool pressed, Millis now) {
  if (pressed != raw_) {
    raw_ = pressed;
    changedAt_ = now;
  }
  if (raw_ != stable_ && hasElapsed(now, changedAt_, settings_.debounceMs)) {
    stable_ = raw_;
    return stable_;
  }
  return false;
}

}  // namespace cisc
