#pragma once

#include <cstdint>
#include <string>
#include <vector>

#include "HouseController.h"
#include "HouseSnapshot.h"
#include "Time.h"

namespace cisc {
namespace api {

// One server-sent event: its name and its data. The web server library writes the
// "event:" and "data:" lines itself, so this is all it needs.
struct Event {
  std::string name;
  std::string data;
};

struct EventSettings {
  // The state is sent at most this often, however fast the readings move.
  Millis homeMinIntervalMs = 1000;
  // After this long with nothing to send, an empty "ping" event tells the app the
  // connection is alive. The app reconnects when it hears nothing for longer.
  Millis keepaliveMs = 15000;
};

// What the SSE stream (GET /api/v1/events) sends. It only decides which events to
// send and when: the web server calls poll() often and hands whatever comes back to
// every connected client. Keeping it apart from the network lets it be tested on the host.
class EventStream {
 public:
  // Starts from the current state of the house: only what happens from now on is sent.
  EventStream(const HouseController& house, std::string bootId, const EventSettings& settings = EventSettings{});

  // The whole state as a "home" event, for a client that has just connected.
  Event connectEvent() const;

  // The events to send now to every client, in order; empty when there is nothing to send.
  std::vector<Event> poll();

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
