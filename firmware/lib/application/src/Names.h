#pragma once

#include "GasGuard.h"
#include "HumidityController.h"

namespace cisc {

// The names the API contract (docs/api.md) uses for these states.
inline const char* toString(GasState state) {
  switch (state) {
    case GasState::Starting:
      return "starting";
    case GasState::Normal:
      return "normal";
    case GasState::Emergency:
      return "emergency";
  }
  return "unknown";
}

inline const char* toString(ControlMode mode) {
  switch (mode) {
    case ControlMode::Automatic:
      return "automatic";
    case ControlMode::Manual:
      return "manual";
  }
  return "unknown";
}

}  // namespace cisc
