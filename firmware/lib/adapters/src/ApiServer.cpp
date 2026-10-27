#include "ApiServer.h"

#include <cstdlib>
#include <cstring>
#include <string>
#include <vector>

namespace adapters {

namespace {

constexpr const char* kEventsPath = "/api/v1/events";
// A command body is a few dozen bytes; anything bigger is not a command.
constexpr std::size_t kMaxBodyBytes = 256;

cisc::api::Method methodOf(const AsyncWebServerRequest* request) {
  switch (request->method()) {
    case HTTP_GET:
      return cisc::api::Method::Get;
    case HTTP_PUT:
      return cisc::api::Method::Put;
    default:
      return cisc::api::Method::Other;
  }
}

// "a=1&b=2", rebuilt from the parameters the library has already split.
std::string queryOf(const AsyncWebServerRequest* request) {
  std::string query;
  for (std::size_t i = 0; i < request->params(); ++i) {
    const AsyncWebParameter* parameter = request->getParam(i);
    if (parameter->isPost() || parameter->isFile()) continue;
    if (!query.empty()) query += '&';
    query += parameter->name().c_str();
    query += '=';
    query += parameter->value().c_str();
  }
  return query;
}

}  // namespace

void ApiServer::begin() {
  // A client over the limit is not handled by the event source; it falls through to
  // answer() below, which tells it why.
  eventSource_.setFilter([this](AsyncWebServerRequest*) { return eventSource_.count() < maxEventClients_; });
  eventSource_.onConnect([this](AsyncEventSourceClient* client) {
    cisc::api::Event event;
    {
      std::lock_guard<std::mutex> lock(mutex_);
      event = stream_.connectEvent();
    }
    client->send(event.data.c_str(), event.name.c_str());
  });
  server_.addHandler(&eventSource_);

  server_.onRequestBody([this](AsyncWebServerRequest* request, std::uint8_t* data, std::size_t length,
                               std::size_t index, std::size_t total) { collectBody(request, data, length, index, total); });
  server_.onNotFound([this](AsyncWebServerRequest* request) { answer(request); });
  server_.begin();
}

void ApiServer::tick() {
  std::vector<cisc::api::Event> events;
  {
    std::lock_guard<std::mutex> lock(mutex_);
    events = stream_.poll();
  }
  for (const cisc::api::Event& event : events) {
    eventSource_.send(event.data.c_str(), event.name.c_str());
  }
}

// Keeps the body of a request in the request itself: the library frees
// _tempObject with free() when the request ends. A body that is too big is not
// kept, and Api then answers that the body is invalid.
void ApiServer::collectBody(AsyncWebServerRequest* request, std::uint8_t* data, std::size_t length, std::size_t index,
                            std::size_t total) {
  if (total > kMaxBodyBytes) return;
  if (index == 0) {
    request->_tempObject = std::malloc(total + 1);
    if (request->_tempObject == nullptr) return;
  }
  char* body = static_cast<char*>(request->_tempObject);
  if (body == nullptr) return;
  std::memcpy(body + index, data, length);
  body[index + length] = '\0';
}

void ApiServer::answer(AsyncWebServerRequest* request) {
  cisc::api::Response response;
  if (request->url() == kEventsPath) {
    // Here only because the event source did not take it.
    response = request->isSSE() ? cisc::api::Api::tooManyClients() : cisc::api::Api::notAcceptable();
  } else {
    const char* body = request->_tempObject != nullptr ? static_cast<const char*>(request->_tempObject) : "";
    std::lock_guard<std::mutex> lock(mutex_);
    response = api_.handle(methodOf(request), request->url().c_str(), queryOf(request), body);
  }
  AsyncWebServerResponse* reply = request->beginResponse(response.status, "application/json", response.body.c_str());
  reply->addHeader("Cache-Control", "no-store");
  request->send(reply);
}

}  // namespace adapters
