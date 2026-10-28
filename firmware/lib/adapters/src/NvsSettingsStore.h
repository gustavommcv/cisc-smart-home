#pragma once

#include "Ports.h"

namespace adapters {

// Keeps the settings the user changed in the board's non-volatile storage (NVS), which survives a
// power cut. Each value is a whole percent, so a byte is enough.
class NvsSettingsStore : public cisc::SettingsStore {
 public:
  bool loadHumidity(cisc::HumiditySettings& out) override;
  void saveHumidity(const cisc::HumiditySettings& settings) override;
};

}  // namespace adapters
