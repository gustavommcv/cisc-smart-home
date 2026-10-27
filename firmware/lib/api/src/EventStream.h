#pragma once

#include <string>

#include "HouseController.h"
#include "HouseSnapshot.h"
#include "Time.h"

namespace cisc {
namespace api {

struct EventSettings {
  // The state is sent at most this often, however fast the readings move.
  Millis homeMinIntervalMs = 1000;
  // After this long with nothing to send, a comment line tells the app the
  // connection is alive. The app reconnects when it hears nothing for longer.
  Millis keepaliveMs = 15000;
};

// What the SSE stream (GET /api/v1/events) sends. It is a pure producer of text:
// the web server calls poll() often and writes whatever comes back to every
// connected client. Keeping it apart from the network lets it be tested on the host.
class EventStream {
 public:
  // Starts from the current state of the house: only what happens from now on is sent.
  EventStream(const HouseController& house, std::string bootId, const EventSettings& settings = EventSettings{});

  // The whole state as a "home" event, for a client that has just connected.
  std::string connectFrame() const;

  // The frames to send now to every client, or "" when there is nothing to send.
  std::string poll();

 private:
  const HouseController& house_;
  std::string bootId_;
  EventSettings settings_;

  HouseSnapshot lastSent_;
  Millis lastHomeAt_;
  Millis lastActivityAt_;
  std::uint32_t lastAlertCount_;
};

}  // namespace api
}  // namespace cisc
