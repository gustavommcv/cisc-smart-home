#include "ApiJson.h"

#include <ArduinoJson.h>

#include <cmath>

#include "Names.h"

namespace cisc {
namespace api {

namespace {

// The contract gives decimals with one place. Rounding here keeps float noise
// (21.399999) out of the output.
double oneDecimal(float value) {
  return std::round(static_cast<double>(value) * 10.0) / 10.0;
}

JsonObject addEntity(JsonArray entities, const char* id, const char* kind, const char* name) {
  JsonObject entity = entities.add<JsonObject>();
  entity["id"] = id;
  entity["kind"] = kind;
  entity["name"] = name;
  return entity;
}

// A number the user can change: it comes with the range, the step and the value the house has when nothing was changed.
void addPercentLimit(JsonArray entities, const char* id, const char* name, float value, float defaultValue) {
  JsonObject entity = addEntity(entities, id, "number", name);
  entity["value"] = static_cast<int>(std::lround(value));
  entity["unit"] = "%";
  entity["min"] = static_cast<int>(humidity_limits::kMinPercent);
  entity["max"] = static_cast<int>(humidity_limits::kMaxPercent);
  entity["step"] = static_cast<int>(humidity_limits::kStepPercent);
  entity["default"] = static_cast<int>(std::lround(defaultValue));
  entity["commandable"] = true;
}

JsonArray addArea(JsonArray areas, const char* id, const char* name) {
  JsonObject area = areas.add<JsonObject>();
  area["id"] = id;
  area["name"] = name;
  return area["devices"].to<JsonArray>();
}

JsonArray addDevice(JsonArray devices, const char* id, const char* name) {
  JsonObject device = devices.add<JsonObject>();
  device["id"] = id;
  device["name"] = name;
  return device["entities"].to<JsonArray>();
}

// The maquete: a kitchen with a gas monitor and a bedroom with a closet monitor. A device is
// the unit someone would buy and install, and its entities are what it senses and controls:
// here one ESP32 runs both units, because the maquete asks for it, and in a real house each
// would be a board of its own. Entity ids and units are part of the contract.
void buildHome(JsonDocument& home, const HouseSnapshot& s, const std::string& bootId) {
  home["apiVersion"] = kApiVersion;
  home["bootId"] = bootId;
  home["uptimeMs"] = s.uptimeMs;
  JsonArray areas = home["areas"].to<JsonArray>();

  JsonArray kitchen = addArea(areas, "kitchen", "Kitchen");

  JsonArray gasMonitor = addDevice(kitchen, "gas-monitor", "Gas monitor");
  JsonObject level = addEntity(gasMonitor, "gas-level", "sensor", "Gas level");
  level["value"] = s.gasReading;
  level["unit"] = "adc";
  addEntity(gasMonitor, "gas-state", "sensor", "Gas state")["value"] = toString(s.gasState);
  addEntity(gasMonitor, "gas-alerts", "sensor", "Gas alerts since boot")["value"] = s.gasAlertCount;
  addEntity(gasMonitor, "valve-open", "binary_sensor", "Valve open")["value"] = s.valveOpen;
  JsonObject buzzer = addEntity(gasMonitor, "buzzer-enabled", "switch", "Buzzer");
  buzzer["value"] = s.buzzerEnabled;
  buzzer["commandable"] = true;

  JsonArray bedroom = addArea(areas, "bedroom", "Bedroom");

  JsonArray closetMonitor = addDevice(bedroom, "closet-monitor", "Closet monitor");
  JsonObject temperature = addEntity(closetMonitor, "temperature", "sensor", "Temperature");
  if (s.climateValid) {
    temperature["value"] = oneDecimal(s.temperatureC);
  } else {
    temperature["value"] = nullptr;
  }
  temperature["unit"] = "°C";
  JsonObject humidity = addEntity(closetMonitor, "humidity", "sensor", "Humidity");
  if (s.climateValid) {
    humidity["value"] = oneDecimal(s.humidityPercent);
  } else {
    humidity["value"] = nullptr;
  }
  humidity["unit"] = "%";

  JsonObject power = addEntity(closetMonitor, "dehumidifier-power", "switch", "Power");
  power["value"] = s.dehumidifierOn;
  power["commandable"] = true;
  JsonObject mode = addEntity(closetMonitor, "dehumidifier-mode", "select", "Mode");
  mode["value"] = toString(s.dehumidifierMode);
  JsonArray options = mode["options"].to<JsonArray>();
  options.add(toString(ControlMode::Automatic));
  options.add(toString(ControlMode::Manual));
  mode["commandable"] = true;
  const HumiditySettings defaults;
  addPercentLimit(closetMonitor, "dehumidifier-on-at", "Turn on at", s.dehumidifierOnAt, defaults.onAtPercent);
  addPercentLimit(closetMonitor, "dehumidifier-off-at", "Turn off at", s.dehumidifierOffAt, defaults.offAtPercent);
}

}  // namespace

std::string homeJson(const HouseSnapshot& snapshot, const std::string& bootId) {
  JsonDocument home;
  buildHome(home, snapshot, bootId);
  std::string json;
  serializeJson(home, json);
  return json;
}

bool entityJson(const HouseSnapshot& snapshot, const std::string& entityId, std::string& json) {
  JsonDocument home;
  buildHome(home, snapshot, "");
  JsonArray areas = home["areas"];
  for (JsonObject area : areas) {
    JsonArray devices = area["devices"];
    for (JsonObject device : devices) {
      JsonArray entities = device["entities"];
      for (JsonObject entity : entities) {
        if (entityId == entity["id"].as<const char*>()) {
          json.clear();
          serializeJson(entity, json);
          return true;
        }
      }
    }
  }
  return false;
}

std::string historyJson(const HouseController::History& history, const std::string& bootId, bool hasAfter,
                        std::uint32_t after, std::size_t limit) {
  JsonDocument document;
  document["bootId"] = bootId;
  JsonArray samples = document["samples"].to<JsonArray>();
  for (std::size_t i = 0; i < history.size() && samples.size() < limit; ++i) {
    const Sample& sample = history.at(i);
    if (hasAfter && sample.timestampMs <= after) continue;
    JsonObject entry = samples.add<JsonObject>();
    entry["uptimeMs"] = sample.timestampMs;
    JsonObject values = entry["values"].to<JsonObject>();
    values["gas-level"] = sample.gasReading;
    if (sample.climateValid) {
      values["temperature"] = oneDecimal(sample.temperatureC);
      values["humidity"] = oneDecimal(sample.humidityPercent);
    } else {
      values["temperature"] = nullptr;
      values["humidity"] = nullptr;
    }
  }
  std::string json;
  serializeJson(document, json);
  return json;
}

std::string alertJson(const HouseSnapshot& snapshot) {
  JsonDocument alert;
  alert["type"] = "gas-leak";
  alert["areaId"] = "kitchen";
  alert["entityId"] = "gas-state";
  alert["count"] = snapshot.gasAlertCount;
  alert["uptimeMs"] = snapshot.uptimeMs;
  std::string json;
  serializeJson(alert, json);
  return json;
}

}  // namespace api
}  // namespace cisc
