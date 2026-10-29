#include <set>
#include <string>

#include <ArduinoJson.h>
#include <unity.h>

#include "ApiJson.h"

using cisc::ControlMode;
using cisc::GasState;
using cisc::HouseSnapshot;

namespace {

// A healthy house in NORMAL, with a valid climate reading.
HouseSnapshot normalHouse() {
  HouseSnapshot snapshot{};
  snapshot.uptimeMs = 183450;
  snapshot.gasReading = 142;
  snapshot.gasState = GasState::Normal;
  snapshot.valveOpen = true;
  snapshot.gasAlertCount = 0;
  snapshot.climateSampled = true;
  snapshot.climateValid = true;
  snapshot.temperatureC = 21.4f;
  snapshot.humidityPercent = 74.1f;
  snapshot.dehumidifierOn = false;
  snapshot.dehumidifierMode = ControlMode::Automatic;
  snapshot.dehumidifierOnAt = 85.0f;
  snapshot.dehumidifierOffAt = 83.0f;
  snapshot.buzzerEnabled = true;
  snapshot.gasAlarmAt = 160;
  return snapshot;
}

JsonDocument parse(const std::string& text) {
  JsonDocument document;
  const DeserializationError error = deserializeJson(document, text);
  TEST_ASSERT_FALSE_MESSAGE(error, "the output is not valid JSON");
  return document;
}

// Looks an entity up by id anywhere in the home tree.
JsonObject entity(JsonDocument& home, const char* id) {
  JsonArray areas = home["areas"];
  for (JsonObject area : areas) {
    JsonArray devices = area["devices"];
    for (JsonObject device : devices) {
      JsonArray entities = device["entities"];
      for (JsonObject candidate : entities) {
        if (std::string(candidate["id"].as<const char*>()) == id) return candidate;
      }
    }
  }
  TEST_FAIL_MESSAGE("entity not found");
  return JsonObject();
}

// Calls `visit` for every entity of the house.
template <typename Visit>
void forEachEntity(JsonDocument& home, Visit visit) {
  JsonArray areas = home["areas"];
  for (JsonObject area : areas) {
    JsonArray devices = area["devices"];
    for (JsonObject device : devices) {
      JsonArray entities = device["entities"];
      for (JsonObject item : entities) visit(item);
    }
  }
}

}  // namespace

// ---- the home tree ----------------------------------------------------------

void test_the_home_envelope_carries_version_boot_and_uptime() {
  JsonDocument home = parse(cisc::api::homeJson(normalHouse(), "5f3a9c21"));
  TEST_ASSERT_EQUAL_INT(1, home["apiVersion"].as<int>());
  TEST_ASSERT_EQUAL_STRING("5f3a9c21", home["bootId"].as<const char*>());
  TEST_ASSERT_EQUAL_UINT32(183450, home["uptimeMs"].as<uint32_t>());
}

void test_the_house_has_a_kitchen_and_a_bedroom() {
  JsonDocument home = parse(cisc::api::homeJson(normalHouse(), "x"));
  JsonArray areas = home["areas"];
  TEST_ASSERT_EQUAL_UINT(2, areas.size());
  TEST_ASSERT_EQUAL_STRING("kitchen", areas[0]["id"].as<const char*>());
  TEST_ASSERT_EQUAL_STRING("Kitchen", areas[0]["name"].as<const char*>());
  TEST_ASSERT_EQUAL_STRING("bedroom", areas[1]["id"].as<const char*>());
}

// A device is a unit someone would buy and install, and its entities are what it senses and what it controls. In
// the maquete one ESP32 runs the unit of each room; in a real house each would be a board of its own.
void test_each_room_has_one_device_that_is_a_unit_and_not_a_component() {
  JsonDocument home = parse(cisc::api::homeJson(normalHouse(), "x"));
  JsonArray kitchen = home["areas"][0]["devices"];
  JsonArray bedroom = home["areas"][1]["devices"];
  TEST_ASSERT_EQUAL_UINT(1, kitchen.size());
  TEST_ASSERT_EQUAL_STRING("gas-monitor", kitchen[0]["id"].as<const char*>());
  TEST_ASSERT_EQUAL_STRING("Gas monitor", kitchen[0]["name"].as<const char*>());
  TEST_ASSERT_EQUAL_UINT(1, bedroom.size());
  TEST_ASSERT_EQUAL_STRING("closet-monitor", bedroom[0]["id"].as<const char*>());
  TEST_ASSERT_EQUAL_STRING("Closet monitor", bedroom[0]["name"].as<const char*>());
}

std::string idsOf(JsonArray entities) {
  std::string ids;
  for (JsonObject item : entities) {
    if (!ids.empty()) ids += ",";
    ids += item["id"].as<const char*>();
  }
  return ids;
}

void test_a_device_holds_everything_its_unit_senses_and_controls() {
  JsonDocument home = parse(cisc::api::homeJson(normalHouse(), "x"));
  TEST_ASSERT_EQUAL_STRING("gas-level,gas-state,gas-alerts,valve-open,buzzer-enabled", idsOf(home["areas"][0]["devices"][0]["entities"]).c_str());
  TEST_ASSERT_EQUAL_STRING("temperature,humidity,dehumidifier-power,dehumidifier-mode,dehumidifier-on-at,dehumidifier-off-at", idsOf(home["areas"][1]["devices"][0]["entities"]).c_str());
}

void test_every_entity_id_is_unique_across_the_house() {
  JsonDocument home = parse(cisc::api::homeJson(normalHouse(), "x"));
  std::set<std::string> ids;
  size_t total = 0;
  forEachEntity(home, [&](JsonObject item) {
    ids.insert(item["id"].as<const char*>());
    ++total;
  });
  TEST_ASSERT_EQUAL_UINT(11, total);
  TEST_ASSERT_EQUAL_UINT(total, ids.size());
}

// ---- the kitchen ------------------------------------------------------------

void test_the_gas_level_is_a_sensor_in_adc_units() {
  JsonDocument home = parse(cisc::api::homeJson(normalHouse(), "x"));
  JsonObject level = entity(home, "gas-level");
  TEST_ASSERT_EQUAL_STRING("sensor", level["kind"].as<const char*>());
  TEST_ASSERT_EQUAL_INT(142, level["value"].as<int>());
  TEST_ASSERT_EQUAL_STRING("adc", level["unit"].as<const char*>());
}

void test_the_gas_state_uses_the_names_of_the_contract() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.gasState = GasState::Emergency;
  JsonDocument home = parse(cisc::api::homeJson(snapshot, "x"));
  TEST_ASSERT_EQUAL_STRING("emergency", entity(home, "gas-state")["value"].as<const char*>());
}

void test_the_alert_counter_and_the_valve_are_reported() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.gasAlertCount = 3;
  snapshot.valveOpen = false;
  JsonDocument home = parse(cisc::api::homeJson(snapshot, "x"));
  TEST_ASSERT_EQUAL_UINT32(3, entity(home, "gas-alerts")["value"].as<uint32_t>());
  JsonObject valve = entity(home, "valve-open");
  TEST_ASSERT_EQUAL_STRING("binary_sensor", valve["kind"].as<const char*>());
  TEST_ASSERT_FALSE(valve["value"].as<bool>());
}

// ---- the bedroom ------------------------------------------------------------

void test_temperature_and_humidity_come_with_their_units() {
  JsonDocument home = parse(cisc::api::homeJson(normalHouse(), "x"));
  TEST_ASSERT_EQUAL_FLOAT(21.4f, entity(home, "temperature")["value"].as<float>());
  TEST_ASSERT_EQUAL_STRING("°C", entity(home, "temperature")["unit"].as<const char*>());
  TEST_ASSERT_EQUAL_FLOAT(74.1f, entity(home, "humidity")["value"].as<float>());
  TEST_ASSERT_EQUAL_STRING("%", entity(home, "humidity")["unit"].as<const char*>());
}

void test_without_a_valid_reading_the_climate_values_are_null() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.climateValid = false;
  JsonDocument home = parse(cisc::api::homeJson(snapshot, "x"));
  TEST_ASSERT_TRUE(entity(home, "temperature")["value"].isNull());
  TEST_ASSERT_TRUE(entity(home, "humidity")["value"].isNull());
}

void test_decimals_are_rounded_to_one_place() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.temperatureC = 21.43f;
  snapshot.humidityPercent = 74.06f;
  const std::string json = cisc::api::homeJson(snapshot, "x");
  TEST_ASSERT_NOT_EQUAL(std::string::npos, json.find("\"value\":21.4,"));
  TEST_ASSERT_NOT_EQUAL(std::string::npos, json.find("\"value\":74.1,"));
}

void test_the_dehumidifier_can_be_commanded_and_shows_its_mode() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.dehumidifierOn = true;
  snapshot.dehumidifierMode = ControlMode::Manual;
  JsonDocument home = parse(cisc::api::homeJson(snapshot, "x"));
  JsonObject power = entity(home, "dehumidifier-power");
  TEST_ASSERT_EQUAL_STRING("switch", power["kind"].as<const char*>());
  TEST_ASSERT_TRUE(power["value"].as<bool>());
  TEST_ASSERT_TRUE(power["commandable"].as<bool>());
  JsonObject mode = entity(home, "dehumidifier-mode");
  TEST_ASSERT_EQUAL_STRING("select", mode["kind"].as<const char*>());
  TEST_ASSERT_EQUAL_STRING("manual", mode["value"].as<const char*>());
  TEST_ASSERT_TRUE(mode["commandable"].as<bool>());
  TEST_ASSERT_EQUAL_UINT(2, mode["options"].as<JsonArray>().size());
  TEST_ASSERT_EQUAL_STRING("automatic", mode["options"][0].as<const char*>());
  TEST_ASSERT_EQUAL_STRING("manual", mode["options"][1].as<const char*>());
}

// The limits are numbers the user can change: they come with the range, the step and the value the house has when nothing was
// changed, so the app can draw a control that knows what is allowed, and a way back, without knowing the rule.
void test_the_limits_of_the_dehumidifier_are_numbers_with_their_range_and_default() {
  std::string json;
  TEST_ASSERT_TRUE(cisc::api::entityJson(normalHouse(), "dehumidifier-on-at", json));
  TEST_ASSERT_EQUAL_STRING(
      "{\"id\":\"dehumidifier-on-at\",\"kind\":\"number\",\"name\":\"Turn on at\",\"value\":85,\"unit\":\"%\",\"min\":40,\"max\":95,\"step\":1,\"default\":65,\"commandable\":true}",
      json.c_str());
  TEST_ASSERT_TRUE(cisc::api::entityJson(normalHouse(), "dehumidifier-off-at", json));
  TEST_ASSERT_EQUAL_STRING(
      "{\"id\":\"dehumidifier-off-at\",\"kind\":\"number\",\"name\":\"Turn off at\",\"value\":83,\"unit\":\"%\",\"min\":40,\"max\":95,\"step\":1,\"default\":55,\"commandable\":true}",
      json.c_str());
}

void test_the_limits_show_the_values_the_house_has_not_the_defaults() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.dehumidifierOnAt = 90.0f;
  snapshot.dehumidifierOffAt = 60.0f;
  JsonDocument home = parse(cisc::api::homeJson(snapshot, "x"));
  JsonObject on = entity(home, "dehumidifier-on-at");
  JsonObject off = entity(home, "dehumidifier-off-at");
  TEST_ASSERT_EQUAL_INT(90, on["value"].as<int>());
  TEST_ASSERT_EQUAL_INT(65, on["default"].as<int>());
  TEST_ASSERT_EQUAL_INT(60, off["value"].as<int>());
  TEST_ASSERT_EQUAL_INT(55, off["default"].as<int>());
}

// The buzzer is a switch of the gas monitor: it silences the sound of the board and nothing else of the alarm.
void test_the_buzzer_is_a_switch_of_the_gas_monitor() {
  std::string json;
  TEST_ASSERT_TRUE(cisc::api::entityJson(normalHouse(), "buzzer-enabled", json));
  TEST_ASSERT_EQUAL_STRING("{\"id\":\"buzzer-enabled\",\"kind\":\"switch\",\"name\":\"Buzzer\",\"value\":true,\"commandable\":true}", json.c_str());
}

void test_a_silenced_buzzer_shows_as_off() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.buzzerEnabled = false;
  JsonDocument home = parse(cisc::api::homeJson(snapshot, "x"));
  TEST_ASSERT_FALSE(entity(home, "buzzer-enabled")["value"].as<bool>());
}

void test_only_the_dehumidifier_entities_are_commandable() {
  JsonDocument home = parse(cisc::api::homeJson(normalHouse(), "x"));
  forEachEntity(home, [](JsonObject item) {
    const std::string id = item["id"].as<const char*>();
    const bool expected = id == "dehumidifier-power" || id == "dehumidifier-mode" || id == "dehumidifier-on-at" || id == "dehumidifier-off-at" || id == "buzzer-enabled";
    TEST_ASSERT_EQUAL_MESSAGE(expected, item["commandable"].as<bool>(), id.c_str());
  });
}

// ---- a single entity and the alert ------------------------------------------

// ---- what a sensor says it is, and the lines to draw over it --------------------

void test_the_gas_level_says_it_is_gas_and_gives_the_limit_of_the_alarm() {
  JsonDocument home = parse(cisc::api::homeJson(normalHouse(), "x"));
  JsonObject level = entity(home, "gas-level");
  TEST_ASSERT_EQUAL_STRING("gas", level["class"].as<const char*>());
  JsonArray limits = level["limits"];
  TEST_ASSERT_EQUAL_UINT(1, limits.size());
  TEST_ASSERT_EQUAL_STRING("alarm", limits[0]["kind"].as<const char*>());
  TEST_ASSERT_EQUAL_INT(160, limits[0]["value"].as<int>());
  TEST_ASSERT_FALSE(limits[0]["entity"].is<const char*>());
}

void test_the_limit_of_the_alarm_is_the_one_the_house_has_not_a_copy() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.gasAlarmAt = 200;
  JsonDocument home = parse(cisc::api::homeJson(snapshot, "x"));
  TEST_ASSERT_EQUAL_INT(200, entity(home, "gas-level")["limits"][0]["value"].as<int>());
}

void test_the_temperature_says_it_is_temperature_and_has_no_limits() {
  JsonDocument home = parse(cisc::api::homeJson(normalHouse(), "x"));
  JsonObject temperature = entity(home, "temperature");
  TEST_ASSERT_EQUAL_STRING("temperature", temperature["class"].as<const char*>());
  TEST_ASSERT_FALSE(temperature["limits"].is<JsonArray>());
}

void test_the_humidity_says_it_is_humidity_and_points_to_the_limits_the_user_can_change() {
  JsonDocument home = parse(cisc::api::homeJson(normalHouse(), "x"));
  JsonObject humidity = entity(home, "humidity");
  TEST_ASSERT_EQUAL_STRING("humidity", humidity["class"].as<const char*>());
  JsonArray limits = humidity["limits"];
  TEST_ASSERT_EQUAL_UINT(2, limits.size());
  TEST_ASSERT_EQUAL_STRING("turns-on", limits[0]["kind"].as<const char*>());
  TEST_ASSERT_EQUAL_STRING("dehumidifier-on-at", limits[0]["entity"].as<const char*>());
  TEST_ASSERT_EQUAL_STRING("turns-off", limits[1]["kind"].as<const char*>());
  TEST_ASSERT_EQUAL_STRING("dehumidifier-off-at", limits[1]["entity"].as<const char*>());
  TEST_ASSERT_FALSE(limits[0]["value"].is<int>());
}

void test_the_limits_of_the_humidity_are_entities_that_exist_and_are_numbers() {
  JsonDocument home = parse(cisc::api::homeJson(normalHouse(), "x"));
  for (JsonObject limit : entity(home, "humidity")["limits"].as<JsonArray>()) {
    JsonObject target = entity(home, limit["entity"].as<const char*>());
    TEST_ASSERT_EQUAL_STRING("number", target["kind"].as<const char*>());
  }
}

void test_only_the_three_sensors_that_are_charted_have_a_class() {
  JsonDocument home = parse(cisc::api::homeJson(normalHouse(), "x"));
  std::set<std::string> withClass;
  forEachEntity(home, [&](JsonObject item) {
    if (item["class"].is<const char*>()) withClass.insert(item["id"].as<const char*>());
  });
  TEST_ASSERT_TRUE(withClass == (std::set<std::string>{"gas-level", "temperature", "humidity"}));
}

void test_only_the_gas_level_and_the_humidity_have_limits() {
  JsonDocument home = parse(cisc::api::homeJson(normalHouse(), "x"));
  std::set<std::string> withLimits;
  forEachEntity(home, [&](JsonObject item) {
    if (item["limits"].is<JsonArray>()) withLimits.insert(item["id"].as<const char*>());
  });
  TEST_ASSERT_TRUE(withLimits == (std::set<std::string>{"gas-level", "humidity"}));
}

void test_a_single_gas_level_comes_with_its_class_and_limits_too() {
  std::string json;
  TEST_ASSERT_TRUE(cisc::api::entityJson(normalHouse(), "gas-level", json));
  TEST_ASSERT_EQUAL_STRING(
      "{\"id\":\"gas-level\",\"kind\":\"sensor\",\"name\":\"Gas level\",\"value\":142,\"unit\":\"adc\",\"class\":\"gas\","
      "\"limits\":[{\"kind\":\"alarm\",\"value\":160}]}",
      json.c_str());
}

void test_a_single_entity_has_the_exact_shape_of_the_contract() {
  std::string json;
  TEST_ASSERT_TRUE(cisc::api::entityJson(normalHouse(), "dehumidifier-power", json));
  TEST_ASSERT_EQUAL_STRING(
      "{\"id\":\"dehumidifier-power\",\"kind\":\"switch\",\"name\":\"Power\",\"value\":false,\"commandable\":true}",
      json.c_str());
}

void test_an_unknown_entity_is_reported_as_missing() {
  std::string json;
  TEST_ASSERT_FALSE(cisc::api::entityJson(normalHouse(), "no-such-thing", json));
}

void test_the_alert_event_has_the_exact_shape_of_the_contract() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.gasAlertCount = 1;
  TEST_ASSERT_EQUAL_STRING(
      "{\"type\":\"gas-leak\",\"areaId\":\"kitchen\",\"entityId\":\"gas-state\",\"count\":1,\"uptimeMs\":183450}",
      cisc::api::alertJson(snapshot).c_str());
}

int main() {
  UNITY_BEGIN();
  RUN_TEST(test_the_home_envelope_carries_version_boot_and_uptime);
  RUN_TEST(test_the_house_has_a_kitchen_and_a_bedroom);
  RUN_TEST(test_each_room_has_one_device_that_is_a_unit_and_not_a_component);
  RUN_TEST(test_a_device_holds_everything_its_unit_senses_and_controls);
  RUN_TEST(test_every_entity_id_is_unique_across_the_house);
  RUN_TEST(test_the_gas_level_is_a_sensor_in_adc_units);
  RUN_TEST(test_the_gas_state_uses_the_names_of_the_contract);
  RUN_TEST(test_the_alert_counter_and_the_valve_are_reported);
  RUN_TEST(test_temperature_and_humidity_come_with_their_units);
  RUN_TEST(test_without_a_valid_reading_the_climate_values_are_null);
  RUN_TEST(test_decimals_are_rounded_to_one_place);
  RUN_TEST(test_the_dehumidifier_can_be_commanded_and_shows_its_mode);
  RUN_TEST(test_the_limits_of_the_dehumidifier_are_numbers_with_their_range_and_default);
  RUN_TEST(test_the_limits_show_the_values_the_house_has_not_the_defaults);
  RUN_TEST(test_the_buzzer_is_a_switch_of_the_gas_monitor);
  RUN_TEST(test_a_silenced_buzzer_shows_as_off);
  RUN_TEST(test_only_the_dehumidifier_entities_are_commandable);
  RUN_TEST(test_the_gas_level_says_it_is_gas_and_gives_the_limit_of_the_alarm);
  RUN_TEST(test_the_limit_of_the_alarm_is_the_one_the_house_has_not_a_copy);
  RUN_TEST(test_the_temperature_says_it_is_temperature_and_has_no_limits);
  RUN_TEST(test_the_humidity_says_it_is_humidity_and_points_to_the_limits_the_user_can_change);
  RUN_TEST(test_the_limits_of_the_humidity_are_entities_that_exist_and_are_numbers);
  RUN_TEST(test_only_the_three_sensors_that_are_charted_have_a_class);
  RUN_TEST(test_only_the_gas_level_and_the_humidity_have_limits);
  RUN_TEST(test_a_single_gas_level_comes_with_its_class_and_limits_too);
  RUN_TEST(test_a_single_entity_has_the_exact_shape_of_the_contract);
  RUN_TEST(test_an_unknown_entity_is_reported_as_missing);
  RUN_TEST(test_the_alert_event_has_the_exact_shape_of_the_contract);
  return UNITY_END();
}
