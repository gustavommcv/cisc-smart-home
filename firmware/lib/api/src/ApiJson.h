#pragma once

#include <string>

#include "HouseSnapshot.h"

// JSON for the API contract in docs/api.md. Everything here is a pure function of
// a snapshot, so it is tested on the host without any network.
namespace cisc {
namespace api {

constexpr int kApiVersion = 1;

// The whole house: areas, devices and entities (GET /api/v1/home).
std::string homeJson(const HouseSnapshot& snapshot, const std::string& bootId);

// One entity, in the shape it has inside the home tree. Returns false when no
// entity has that id.
bool entityJson(const HouseSnapshot& snapshot, const std::string& entityId, std::string& json);

// The data of the SSE "alert" event, sent when a gas leak starts.
std::string alertJson(const HouseSnapshot& snapshot);

}  // namespace api
}  // namespace cisc
