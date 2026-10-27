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

// The maquete: a kitchen with the gas system and a bedroom with the closet climate
// and the dehumidifier. Entity ids and units are part of the contract.
void buildHome(JsonDocument& home, const HouseSnapshot& s, const std::string& bootId) {
  home["apiVersion"] = kApiVersion;
  home["bootId"] = bootId;
  home["uptimeMs"] = s.uptimeMs;
  JsonArray areas = home["areas"].to<JsonArray>();

  JsonArray kitchen = addArea(areas, "kitchen", "Kitchen");

  JsonArray gasSensor = addDevice(kitchen, "gas-sensor", "Gas sensor");
  JsonObject level = addEntity(gasSensor, "gas-level", "sensor", "Gas level");
  level["value"] = s.gasReading;
  level["unit"] = "adc";
  addEntity(gasSensor, "gas-state", "sensor", "Gas state")["value"] = toString(s.gasState);
  addEntity(gasSensor, "gas-alerts", "sensor", "Gas alerts since boot")["value"] = s.gasAlertCount;

  JsonArray gasValve = addDevice(kitchen, "gas-valve", "Gas valve");
  addEntity(gasValve, "valve-open", "binary_sensor", "Valve open")["value"] = s.valveOpen;

  JsonArray bedroom = addArea(areas, "bedroom", "Bedroom");

  JsonArray closet = addDevice(bedroom, "closet-sensor", "Closet sensor");
  JsonObject temperature = addEntity(closet, "temperature", "sensor", "Temperature");
  if (s.climateValid) {
    temperature["value"] = oneDecimal(s.temperatureC);
  } else {
    temperature["value"] = nullptr;
  }
  temperature["unit"] = "°C";
  JsonObject humidity = addEntity(closet, "humidity", "sensor", "Humidity");
  if (s.climateValid) {
    humidity["value"] = oneDecimal(s.humidityPercent);
  } else {
    humidity["value"] = nullptr;
  }
  humidity["unit"] = "%";

  JsonArray dehumidifier = addDevice(bedroom, "dehumidifier", "Dehumidifier");
  JsonObject power = addEntity(dehumidifier, "dehumidifier-power", "switch", "Power");
  power["value"] = s.dehumidifierOn;
  power["commandable"] = true;
  JsonObject mode = addEntity(dehumidifier, "dehumidifier-mode", "select", "Mode");
  mode["value"] = toString(s.dehumidifierMode);
  JsonArray options = mode["options"].to<JsonArray>();
  options.add(toString(ControlMode::Automatic));
  options.add(toString(ControlMode::Manual));
  mode["commandable"] = true;
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
