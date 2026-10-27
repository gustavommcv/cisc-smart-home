#pragma once

#include <ESPAsyncWebServer.h>

#include <cstddef>
#include <cstdint>
#include <mutex>

#include "Api.h"
#include "EventStream.h"

namespace adapters {

// Carries the API over HTTP: it reads the request, asks Api what to answer and
// writes the answer, and hands the events of EventStream to every listening client.
// It holds no rules of its own; those are in the api library.
//
// The web server runs in its own task, so everything that touches the house
// happens with `houseMutex` held, and the main loop takes the same mutex around
// the house's tick.
class ApiServer {
 public:
  ApiServer(cisc::api::Api& api, cisc::api::EventStream& events, std::mutex& houseMutex, std::uint16_t port,
            std::size_t maxEventClients)
      : api_(api),
        stream_(events),
        mutex_(houseMutex),
        maxEventClients_(maxEventClients),
        server_(port),
        eventSource_("/api/v1/events") {}

  void begin();

  // Call from the main loop, outside the house's mutex: sends what EventStream has to say.
  void tick();

 private:
  void answer(AsyncWebServerRequest* request);
  void collectBody(AsyncWebServerRequest* request, std::uint8_t* data, std::size_t length, std::size_t index,
                   std::size_t total);

  cisc::api::Api& api_;
  cisc::api::EventStream& stream_;
  std::mutex& mutex_;
  std::size_t maxEventClients_;
  AsyncWebServer server_;
  AsyncEventSource eventSource_;
};

}  // namespace adapters
