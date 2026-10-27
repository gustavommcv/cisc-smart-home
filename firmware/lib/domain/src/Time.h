#pragma once

#include <cstdint>

namespace cisc {

// Milliseconds since boot, as returned by millis(). Wraps around after ~49 days.
using Millis = std::uint32_t;

// True once at least `interval` ms have passed since `since`.
// Unsigned subtraction keeps this correct across the counter wraparound.
inline bool hasElapsed(Millis now, Millis since, Millis interval) {
  return static_cast<Millis>(now - since) >= interval;
}

}  // namespace cisc
