#pragma once

#include "Ports.h"

namespace adapters {

// Keeps the settings the user changed in the board's non-volatile storage (NVS), which survives a
// power cut. Humidity uses bytes; the gas threshold uses an unsigned short.
class NvsSettingsStore : public cisc::SettingsStore {
 public:
  bool loadGasThreshold(int& out) override;
  void saveGasThreshold(int threshold) override;
  bool loadHumidity(cisc::HumiditySettings& out) override;
  void saveHumidity(const cisc::HumiditySettings& settings) override;
};

}  // namespace adapters
