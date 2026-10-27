#include "AccessPoint.h"

namespace adapters {

bool AccessPoint::begin(const char* name, const char* password, std::uint8_t channel, std::uint8_t maxStations,
                        const std::uint8_t address[4]) {
  WiFi.mode(WIFI_AP);
  const IPAddress ip(address[0], address[1], address[2], address[3]);
  // The address is part of the API contract, so it is set instead of left to the default.
  if (!WiFi.softAPConfig(ip, ip, IPAddress(255, 255, 255, 0))) return false;
  return WiFi.softAP(name, password, channel, /*ssid_hidden=*/false, maxStations);
}

}  // namespace adapters
