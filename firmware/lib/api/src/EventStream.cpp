#include "EventStream.h"

#include "ApiJson.h"

namespace cisc {
namespace api {

namespace {

// Same state, whatever the clock says: the uptime alone is not a change.
bool sameState(const HouseSnapshot& a, const HouseSnapshot& b) {
  return a.gasReading == b.gasReading && a.gasState == b.gasState && a.gasDetected == b.gasDetected &&
         a.valveOpen == b.valveOpen && a.rearmReady == b.rearmReady && a.gasAlertCount == b.gasAlertCount &&
         a.climateSampled == b.climateSampled && a.climateValid == b.climateValid &&
         a.temperatureC == b.temperatureC && a.humidityPercent == b.humidityPercent &&
         a.dehumidifierOn == b.dehumidifierOn && a.dehumidifierMode == b.dehumidifierMode;
}

std::string frame(const char* event, const std::string& data) {
  return std::string("event: ") + event + "\ndata: " + data + "\n\n";
}

}  // namespace

EventStream::EventStream(const HouseController& house, std::string bootId, const EventSettings& settings)
    : house_(house),
      bootId_(std::move(bootId)),
      settings_(settings),
      lastSent_(house.snapshot()),
      lastHomeAt_(lastSent_.uptimeMs),
      lastActivityAt_(lastSent_.uptimeMs),
      lastAlertCount_(lastSent_.gasAlertCount) {}

std::string EventStream::connectFrame() const {
  return frame("home", homeJson(house_.snapshot(), bootId_));
}

std::string EventStream::poll() {
  const HouseSnapshot snapshot = house_.snapshot();
  const Millis now = snapshot.uptimeMs;
  std::string frames;

  // A new leak cannot wait for the minimum interval, and the state that comes
  // with it goes out at once too.
  const bool newAlert = snapshot.gasAlertCount != lastAlertCount_;
  if (newAlert) {
    frames += frame("alert", alertJson(snapshot));
    lastAlertCount_ = snapshot.gasAlertCount;
  }

  const bool changed = !sameState(snapshot, lastSent_);
  const bool due = hasElapsed(now, lastHomeAt_, settings_.homeMinIntervalMs);
  if (newAlert || (changed && due)) {
    frames += frame("home", homeJson(snapshot, bootId_));
    lastSent_ = snapshot;
    lastHomeAt_ = now;
  }

  if (!frames.empty()) {
    lastActivityAt_ = now;
  } else if (hasElapsed(now, lastActivityAt_, settings_.keepaliveMs)) {
    frames = ": keepalive\n\n";
    lastActivityAt_ = now;
  }
  return frames;
}

}  // namespace api
}  // namespace cisc
