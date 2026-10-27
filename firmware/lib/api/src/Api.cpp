#include "Api.h"

#include <ArduinoJson.h>

#include <algorithm>
#include <cstdlib>

#include "ApiJson.h"

namespace cisc {
namespace api {

namespace {

// Without a limit a response carries this many samples; the app asks for the rest
// with `after`. It keeps a response small enough for the board's memory.
constexpr std::size_t kDefaultHistoryLimit = 100;

Response ok(std::string body) {
  return Response{200, std::move(body)};
}

Response error(int status, const char* code, const std::string& message) {
  JsonDocument document;
  document["error"]["code"] = code;
  document["error"]["message"] = message;
  std::string body;
  serializeJson(document, body);
  return Response{status, body};
}

Response notFound(const std::string& what) {
  return error(404, "not-found", what);
}

Response methodNotAllowed() {
  return error(405, "method-not-allowed", "This route does not accept that method");
}

// "/api/v1/entities/<id>/value" -> <id>. The id must be one non-empty path segment.
bool matchEntityValue(const std::string& path, std::string& entityId) {
  static const std::string prefix = "/api/v1/entities/";
  static const std::string suffix = "/value";
  if (path.size() <= prefix.size() + suffix.size()) return false;
  if (path.compare(0, prefix.size(), prefix) != 0) return false;
  if (path.compare(path.size() - suffix.size(), suffix.size(), suffix) != 0) return false;
  entityId = path.substr(prefix.size(), path.size() - prefix.size() - suffix.size());
  return entityId.find('/') == std::string::npos;
}

// Finds `name` in a query such as "after=30&limit=5".
bool queryValue(const std::string& query, const std::string& name, std::string& value) {
  std::size_t start = 0;
  while (start <= query.size()) {
    std::size_t end = query.find('&', start);
    if (end == std::string::npos) end = query.size();
    const std::string pair = query.substr(start, end - start);
    const std::size_t equals = pair.find('=');
    if (equals != std::string::npos && pair.compare(0, equals, name) == 0) {
      value = pair.substr(equals + 1);
      return true;
    }
    start = end + 1;
  }
  return false;
}

// Digits only, no sign, no spaces, and not above `max`.
bool parseUnsigned(const std::string& text, unsigned long long max, unsigned long long& result) {
  if (text.empty() || text.size() > 20) return false;
  for (char c : text) {
    if (c < '0' || c > '9') return false;
  }
  result = std::strtoull(text.c_str(), nullptr, 10);
  return result <= max;
}

bool isOption(JsonArray options, const std::string& value) {
  for (JsonVariant option : options) {
    if (value == option.as<const char*>()) return true;
  }
  return false;
}

}  // namespace

Response Api::tooManyClients() {
  return error(503, "too-many-clients", "Too many clients are listening to the events");
}

Response Api::handle(Method method, const std::string& path, const std::string& query, const std::string& body) {
  if (path == "/api/v1/home") {
    if (method != Method::Get) return methodNotAllowed();
    return ok(homeJson(house_.snapshot(), bootId_));
  }
  if (path == "/api/v1/history") {
    if (method != Method::Get) return methodNotAllowed();
    return history(query);
  }
  std::string entityId;
  if (matchEntityValue(path, entityId)) {
    if (method != Method::Put) return methodNotAllowed();
    return command(entityId, body);
  }
  return notFound("No such route: " + path);
}

Response Api::command(const std::string& entityId, const std::string& body) {
  // The entity comes first: asking for something that does not exist, or that
  // never takes commands, is the same answer whatever the body says.
  std::string current;
  if (!entityJson(house_.snapshot(), entityId, current)) return notFound("Entity '" + entityId + "' does not exist");
  JsonDocument entity;
  deserializeJson(entity, current);
  if (!entity["commandable"].as<bool>()) {
    return error(403, "not-commandable", "Entity '" + entityId + "' does not accept commands");
  }

  JsonDocument request;
  const DeserializationError parsed = deserializeJson(request, body);
  if (parsed || !request.is<JsonObject>() || request["value"].isNull()) {
    return error(400, "invalid-body", "The body must be a JSON object with a \"value\"");
  }
  JsonVariant value = request["value"];

  if (entityId == "dehumidifier-power") {
    if (!value.is<bool>()) return error(400, "invalid-value", "The value of a switch must be true or false");
    house_.turnDehumidifier(value.as<bool>());
  } else if (entityId == "dehumidifier-mode") {
    if (!value.is<const char*>() || !isOption(entity["options"], value.as<const char*>())) {
      return error(400, "invalid-value", "The value must be one of the entity's options");
    }
    const std::string mode = value.as<const char*>();
    house_.setDehumidifierMode(mode == "automatic" ? ControlMode::Automatic : ControlMode::Manual);
  } else {
    return error(403, "not-commandable", "Entity '" + entityId + "' does not accept commands");
  }

  entityJson(house_.snapshot(), entityId, current);
  return ok(current);
}

Response Api::history(const std::string& query) {
  std::string text;
  unsigned long long number = 0;

  bool hasAfter = false;
  std::uint32_t after = 0;
  if (queryValue(query, "after", text)) {
    if (!parseUnsigned(text, UINT32_MAX, number)) {
      return error(400, "invalid-query", "'after' must be a whole number of milliseconds");
    }
    hasAfter = true;
    after = static_cast<std::uint32_t>(number);
  }

  std::size_t limit = kDefaultHistoryLimit;
  if (queryValue(query, "limit", text)) {
    if (!parseUnsigned(text, UINT32_MAX, number) || number == 0) {
      return error(400, "invalid-query", "'limit' must be a whole number of at least 1");
    }
    limit = std::min<unsigned long long>(number, kHistoryCapacity);
  }

  return ok(historyJson(house_.history(), bootId_, hasAfter, after, limit));
}

}  // namespace api
}  // namespace cisc
