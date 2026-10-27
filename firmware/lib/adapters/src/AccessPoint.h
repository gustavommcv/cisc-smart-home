#pragma once

#include <WiFi.h>

#include <cstdint>

namespace adapters {

// The Wi-Fi network of the maquete. The board is the access point itself, so the
// demonstration needs no router and no internet.
class AccessPoint {
 public:
  // Starts the network at a fixed address. Returns false when the board refuses it,
  // for instance with a password shorter than 8 characters.
  bool begin(const char* name, const char* password, std::uint8_t channel, std::uint8_t maxStations,
             const std::uint8_t address[4]);

  IPAddress address() const { return WiFi.softAPIP(); }
};

}  // namespace adapters
