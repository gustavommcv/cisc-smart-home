#pragma once

#include <string>

#include "HouseController.h"

namespace cisc {
namespace api {

enum class Method { Get, Put, Other };

// What to send back. The body is always JSON.
struct Response {
  int status;
  std::string body;
};

// The REST side of docs/api.md: it receives a request already split into method,
// path, query and body, acts on the house and says what to answer. It knows
// nothing about the web server that carries the bytes, so it is tested on the host.
// (The SSE stream, GET /api/v1/events, is handled by EventStream.)
class Api {
 public:
  Api(HouseController& house, std::string bootId) : house_(house), bootId_(std::move(bootId)) {}

  Response handle(Method method, const std::string& path, const std::string& query, const std::string& body);

  // The answer for a client of the event stream that does not fit: the web server
  // owns the connections, so it asks for this one when it has to refuse.
  static Response tooManyClients();

  // The answer for a request to the event route that is not from an event stream
  // client (it lacks "Accept: text/event-stream").
  static Response notAcceptable();

 private:
  Response command(const std::string& entityId, const std::string& body);
  Response history(const std::string& query);

  HouseController& house_;
  std::string bootId_;
};

}  // namespace api
}  // namespace cisc
