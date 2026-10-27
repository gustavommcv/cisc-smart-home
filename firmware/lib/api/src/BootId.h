#pragma once

#include <cstdint>
#include <string>

namespace cisc {
namespace api {

// The boot id of the API: eight lowercase hex digits. It is made from a random
// number drawn at start-up, so it changes every time the board restarts.
inline std::string bootIdFromRandom(std::uint32_t value) {
  static const char digits[] = "0123456789abcdef";
  std::string id(8, '0');
  for (int i = 7; i >= 0; --i) {
    id[i] = digits[value & 0xF];
    value >>= 4;
  }
  return id;
}

}  // namespace api
}  // namespace cisc
