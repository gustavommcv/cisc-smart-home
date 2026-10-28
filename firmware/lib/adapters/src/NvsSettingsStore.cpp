#include "NvsSettingsStore.h"

#include <Preferences.h>

namespace adapters {

namespace {

constexpr const char* kNamespace = "cisc";
constexpr const char* kHumidityOnAt = "humOn";
constexpr const char* kHumidityOffAt = "humOff";

}  // namespace

bool NvsSettingsStore::loadHumidity(cisc::HumiditySettings& out) {
  Preferences preferences;
  // Read-only: it fails when nothing was ever saved, which is the same as having nothing to load.
  if (!preferences.begin(kNamespace, true)) return false;
  const bool saved = preferences.isKey(kHumidityOnAt) && preferences.isKey(kHumidityOffAt);
  if (saved) {
    out.onAtPercent = preferences.getUChar(kHumidityOnAt);
    out.offAtPercent = preferences.getUChar(kHumidityOffAt);
  }
  preferences.end();
  return saved;
}

void NvsSettingsStore::saveHumidity(const cisc::HumiditySettings& settings) {
  Preferences preferences;
  if (!preferences.begin(kNamespace, false)) return;
  preferences.putUChar(kHumidityOnAt, static_cast<uint8_t>(settings.onAtPercent));
  preferences.putUChar(kHumidityOffAt, static_cast<uint8_t>(settings.offAtPercent));
  preferences.end();
}

}  // namespace adapters
