#pragma once

// Copy this file to WifiSecrets.h and change the password. WifiSecrets.h is
// ignored by Git, so the real password never reaches the repository.
//
// WPA2 needs at least 8 characters; WifiConfig.h refuses to build with less.
namespace wifi {

constexpr char kAccessPointPassword[] = "change-me-please";

}  // namespace wifi
