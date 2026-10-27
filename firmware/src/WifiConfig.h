#pragma once

#include <cstdint>

// The access point of the maquete: the ESP32 creates its own Wi-Fi network and the
// phone joins it. The name is public; the password comes from WifiSecrets.h.
#if __has_include("WifiSecrets.h")
#include "WifiSecrets.h"
#else
// Lets a fresh clone (and the CI) build; copy the example to set a real password.
#include "WifiSecrets.example.h"
#warning "WifiSecrets.h not found: using the example password. Copy WifiSecrets.example.h to WifiSecrets.h."
#endif

namespace wifi {

constexpr char kAccessPointName[] = "cisc-smart-home";
constexpr std::uint8_t kChannel = 1;
// The board serves a few phones at most; a fifth would only take memory from the gas safety.
constexpr std::uint8_t kMaxStations = 4;
// The address is part of the API contract (docs/api.md).
constexpr std::uint8_t kAddress[4] = {192, 168, 4, 1};

static_assert(sizeof(kAccessPointPassword) - 1 >= 8, "WPA2 needs a password of at least 8 characters");
static_assert(sizeof(kAccessPointPassword) - 1 <= 63, "a WPA2 password has at most 63 characters");

}  // namespace wifi
