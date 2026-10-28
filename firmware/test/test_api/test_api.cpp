#include <string>

#include <ArduinoJson.h>
#include <unity.h>

#include "../support/Fakes.h"
#include "Api.h"

using cisc::api::Api;
using cisc::api::Method;
using cisc::api::Response;
using fakes::Rig;

namespace {

constexpr const char* kBootId = "5f3a9c21";

JsonDocument parse(const std::string& text) {
  JsonDocument document;
  const DeserializationError error = deserializeJson(document, text);
  TEST_ASSERT_FALSE_MESSAGE(error, "the response is not valid JSON");
  return document;
}

void assertError(const Response& response, int status, const char* code) {
  TEST_ASSERT_EQUAL_INT(status, response.status);
  JsonDocument body = parse(response.body);
  TEST_ASSERT_EQUAL_STRING(code, body["error"]["code"].as<const char*>());
  TEST_ASSERT_TRUE(body["error"]["message"].is<const char*>());
}

Response put(Api& api, const char* entity, const char* body) {
  return api.handle(Method::Put, std::string("/api/v1/entities/") + entity + "/value", "", body);
}

// A booted house with a controller ready to take commands.
struct Fixture {
  Rig rig;
  Api api;
  Fixture() : api(rig.controller, kBootId) { rig.bootToNormal(); }
};

}  // namespace

// ---- GET /api/v1/home -------------------------------------------------------

void test_home_returns_the_state_of_the_house() {
  Fixture f;
  const Response response = f.api.handle(Method::Get, "/api/v1/home", "", "");
  TEST_ASSERT_EQUAL_INT(200, response.status);
  JsonDocument home = parse(response.body);
  TEST_ASSERT_EQUAL_STRING(kBootId, home["bootId"].as<const char*>());
  TEST_ASSERT_EQUAL_UINT(2, home["areas"].as<JsonArray>().size());
}

// ---- PUT /api/v1/entities/{id}/value ----------------------------------------

void test_turning_the_dehumidifier_on_runs_the_relay_and_returns_the_entity() {
  Fixture f;
  const Response response = put(f.api, "dehumidifier-power", "{\"value\":true}");
  TEST_ASSERT_EQUAL_INT(200, response.status);
  JsonDocument entity = parse(response.body);
  TEST_ASSERT_EQUAL_STRING("dehumidifier-power", entity["id"].as<const char*>());
  TEST_ASSERT_TRUE(entity["value"].as<bool>());
  TEST_ASSERT_TRUE(f.rig.dehumidifier.value);
  TEST_ASSERT_EQUAL(cisc::ControlMode::Manual, f.rig.controller.snapshot().dehumidifierMode);
}

void test_turning_the_dehumidifier_off_stops_the_relay() {
  Fixture f;
  put(f.api, "dehumidifier-power", "{\"value\":true}");
  const Response response = put(f.api, "dehumidifier-power", "{\"value\":false}");
  TEST_ASSERT_EQUAL_INT(200, response.status);
  TEST_ASSERT_FALSE(parse(response.body)["value"].as<bool>());
  TEST_ASSERT_FALSE(f.rig.dehumidifier.value);
}

void test_setting_the_mode_to_automatic_gives_control_back_to_the_rules() {
  Fixture f;
  put(f.api, "dehumidifier-power", "{\"value\":true}");
  const Response response = put(f.api, "dehumidifier-mode", "{\"value\":\"automatic\"}");
  TEST_ASSERT_EQUAL_INT(200, response.status);
  TEST_ASSERT_EQUAL_STRING("automatic", parse(response.body)["value"].as<const char*>());
  TEST_ASSERT_EQUAL(cisc::ControlMode::Automatic, f.rig.controller.snapshot().dehumidifierMode);
}

void test_setting_the_mode_to_manual_keeps_the_current_state() {
  Fixture f;
  const Response response = put(f.api, "dehumidifier-mode", "{\"value\":\"manual\"}");
  TEST_ASSERT_EQUAL_INT(200, response.status);
  TEST_ASSERT_EQUAL_STRING("manual", parse(response.body)["value"].as<const char*>());
  TEST_ASSERT_FALSE(f.rig.dehumidifier.value);
}

void test_a_value_of_the_wrong_type_is_rejected() {
  Fixture f;
  assertError(put(f.api, "dehumidifier-power", "{\"value\":\"yes\"}"), 400, "invalid-value");
  assertError(put(f.api, "dehumidifier-power", "{\"value\":1}"), 400, "invalid-value");
  assertError(put(f.api, "dehumidifier-mode", "{\"value\":true}"), 400, "invalid-value");
}

void test_a_mode_that_is_not_an_option_is_rejected() {
  Fixture f;
  assertError(put(f.api, "dehumidifier-mode", "{\"value\":\"turbo\"}"), 400, "invalid-value");
}

// ---- the limits of the dehumidifier -----------------------------------------

void test_the_upper_limit_is_changed_and_the_entity_comes_back() {
  Fixture f;
  const Response response = put(f.api, "dehumidifier-on-at", "{\"value\":90}");
  TEST_ASSERT_EQUAL_INT(200, response.status);
  JsonDocument entity = parse(response.body);
  TEST_ASSERT_EQUAL_STRING("dehumidifier-on-at", entity["id"].as<const char*>());
  TEST_ASSERT_EQUAL_STRING("number", entity["kind"].as<const char*>());
  TEST_ASSERT_EQUAL_INT(90, entity["value"].as<int>());
  TEST_ASSERT_EQUAL_FLOAT(90.0f, f.rig.controller.snapshot().dehumidifierOnAt);
  TEST_ASSERT_EQUAL_FLOAT(70.0f, f.rig.controller.snapshot().dehumidifierOffAt);
}

void test_the_lower_limit_is_changed_and_the_entity_comes_back() {
  Fixture f;
  const Response response = put(f.api, "dehumidifier-off-at", "{\"value\":60}");
  TEST_ASSERT_EQUAL_INT(200, response.status);
  JsonDocument entity = parse(response.body);
  TEST_ASSERT_EQUAL_STRING("dehumidifier-off-at", entity["id"].as<const char*>());
  TEST_ASSERT_EQUAL_INT(60, entity["value"].as<int>());
  TEST_ASSERT_EQUAL_FLOAT(80.0f, f.rig.controller.snapshot().dehumidifierOnAt);
  TEST_ASSERT_EQUAL_FLOAT(60.0f, f.rig.controller.snapshot().dehumidifierOffAt);
}

void test_a_limit_that_is_changed_is_saved_for_the_next_time() {
  Fixture f;
  put(f.api, "dehumidifier-on-at", "{\"value\":90}");
  TEST_ASSERT_EQUAL(1, f.rig.store.saves);
  TEST_ASSERT_EQUAL_FLOAT(90.0f, f.rig.store.humidity.onAtPercent);
}

void test_a_limit_outside_the_range_is_rejected() {
  Fixture f;
  assertError(put(f.api, "dehumidifier-on-at", "{\"value\":96}"), 400, "invalid-value");
  assertError(put(f.api, "dehumidifier-off-at", "{\"value\":39}"), 400, "invalid-value");
  assertError(put(f.api, "dehumidifier-off-at", "{\"value\":-5}"), 400, "invalid-value");
}

void test_the_upper_limit_cannot_come_closer_than_two_points_to_the_lower_one() {
  Fixture f;
  assertError(put(f.api, "dehumidifier-on-at", "{\"value\":71}"), 400, "invalid-value");
  assertError(put(f.api, "dehumidifier-on-at", "{\"value\":60}"), 400, "invalid-value");
  TEST_ASSERT_EQUAL(200, put(f.api, "dehumidifier-on-at", "{\"value\":72}").status);
}

void test_the_lower_limit_cannot_come_closer_than_two_points_to_the_upper_one() {
  Fixture f;
  assertError(put(f.api, "dehumidifier-off-at", "{\"value\":79}"), 400, "invalid-value");
  assertError(put(f.api, "dehumidifier-off-at", "{\"value\":85}"), 400, "invalid-value");
  TEST_ASSERT_EQUAL(200, put(f.api, "dehumidifier-off-at", "{\"value\":78}").status);
}

void test_a_limit_that_is_not_a_whole_number_is_rejected() {
  Fixture f;
  assertError(put(f.api, "dehumidifier-on-at", "{\"value\":85.5}"), 400, "invalid-value");
  assertError(put(f.api, "dehumidifier-off-at", "{\"value\":60.2}"), 400, "invalid-value");
}

void test_a_limit_that_is_not_a_number_is_rejected() {
  Fixture f;
  assertError(put(f.api, "dehumidifier-on-at", "{\"value\":\"90\"}"), 400, "invalid-value");
  assertError(put(f.api, "dehumidifier-on-at", "{\"value\":true}"), 400, "invalid-value");
  assertError(put(f.api, "dehumidifier-off-at", "{\"value\":[60]}"), 400, "invalid-value");
}

void test_a_rejected_limit_changes_nothing_and_saves_nothing() {
  Fixture f;
  put(f.api, "dehumidifier-on-at", "{\"value\":96}");
  put(f.api, "dehumidifier-off-at", "{\"value\":\"x\"}");
  TEST_ASSERT_EQUAL_FLOAT(80.0f, f.rig.controller.snapshot().dehumidifierOnAt);
  TEST_ASSERT_EQUAL_FLOAT(70.0f, f.rig.controller.snapshot().dehumidifierOffAt);
  TEST_ASSERT_EQUAL(0, f.rig.store.saves);
}

void test_a_limit_does_not_take_the_dehumidifier_out_of_automatic() {
  Fixture f;
  put(f.api, "dehumidifier-on-at", "{\"value\":90}");
  TEST_ASSERT_EQUAL(cisc::ControlMode::Automatic, f.rig.controller.snapshot().dehumidifierMode);
}

// ---- the buzzer ---------------------------------------------------------------

void test_silencing_the_buzzer_is_a_command_and_the_entity_comes_back() {
  Fixture f;
  const Response response = put(f.api, "buzzer-enabled", "{\"value\":false}");
  TEST_ASSERT_EQUAL_INT(200, response.status);
  JsonDocument entity = parse(response.body);
  TEST_ASSERT_EQUAL_STRING("buzzer-enabled", entity["id"].as<const char*>());
  TEST_ASSERT_FALSE(entity["value"].as<bool>());
  TEST_ASSERT_FALSE(f.rig.controller.snapshot().buzzerEnabled);
}

void test_enabling_the_buzzer_again_is_a_command_too() {
  Fixture f;
  put(f.api, "buzzer-enabled", "{\"value\":false}");
  const Response response = put(f.api, "buzzer-enabled", "{\"value\":true}");
  TEST_ASSERT_EQUAL_INT(200, response.status);
  TEST_ASSERT_TRUE(parse(response.body)["value"].as<bool>());
  TEST_ASSERT_TRUE(f.rig.controller.snapshot().buzzerEnabled);
}

void test_a_value_that_is_not_true_or_false_does_not_touch_the_buzzer() {
  Fixture f;
  assertError(put(f.api, "buzzer-enabled", "{\"value\":\"off\"}"), 400, "invalid-value");
  assertError(put(f.api, "buzzer-enabled", "{\"value\":0}"), 400, "invalid-value");
  TEST_ASSERT_TRUE(f.rig.controller.snapshot().buzzerEnabled);
}

void test_silencing_the_buzzer_does_not_touch_the_dehumidifier() {
  Fixture f;
  put(f.api, "buzzer-enabled", "{\"value\":false}");
  TEST_ASSERT_EQUAL(cisc::ControlMode::Automatic, f.rig.controller.snapshot().dehumidifierMode);
  TEST_ASSERT_FALSE(f.rig.dehumidifier.value);
}

void test_a_rejected_command_changes_nothing() {
  Fixture f;
  put(f.api, "dehumidifier-power", "{\"value\":\"yes\"}");
  TEST_ASSERT_EQUAL(cisc::ControlMode::Automatic, f.rig.controller.snapshot().dehumidifierMode);
  TEST_ASSERT_FALSE(f.rig.dehumidifier.value);
}

void test_a_body_that_is_not_a_json_object_with_a_value_is_rejected() {
  Fixture f;
  assertError(put(f.api, "dehumidifier-power", ""), 400, "invalid-body");
  assertError(put(f.api, "dehumidifier-power", "not json"), 400, "invalid-body");
  assertError(put(f.api, "dehumidifier-power", "[true]"), 400, "invalid-body");
  assertError(put(f.api, "dehumidifier-power", "{}"), 400, "invalid-body");
  assertError(put(f.api, "dehumidifier-power", "{\"value\":null}"), 400, "invalid-body");
}

void test_a_read_only_entity_does_not_accept_commands() {
  Fixture f;
  assertError(put(f.api, "valve-open", "{\"value\":true}"), 403, "not-commandable");
  assertError(put(f.api, "gas-level", "{\"value\":0}"), 403, "not-commandable");
}

void test_an_unknown_entity_is_not_found() {
  Fixture f;
  assertError(put(f.api, "no-such-thing", "{\"value\":true}"), 404, "not-found");
}

void test_the_entity_is_checked_before_the_body() {
  Fixture f;
  assertError(put(f.api, "no-such-thing", "garbage"), 404, "not-found");
  assertError(put(f.api, "valve-open", "garbage"), 403, "not-commandable");
}

// ---- routing ----------------------------------------------------------------

void test_an_unknown_route_is_not_found() {
  Fixture f;
  assertError(f.api.handle(Method::Get, "/api/v1/nothing", "", ""), 404, "not-found");
  assertError(f.api.handle(Method::Get, "/", "", ""), 404, "not-found");
}

void test_a_known_route_with_the_wrong_method_is_not_allowed() {
  Fixture f;
  assertError(f.api.handle(Method::Put, "/api/v1/home", "", "{}"), 405, "method-not-allowed");
  assertError(f.api.handle(Method::Other, "/api/v1/home", "", ""), 405, "method-not-allowed");
  assertError(f.api.handle(Method::Get, "/api/v1/entities/dehumidifier-power/value", "", ""), 405,
              "method-not-allowed");
  assertError(f.api.handle(Method::Put, "/api/v1/history", "", "{}"), 405, "method-not-allowed");
}

void test_a_malformed_entity_path_is_not_found() {
  Fixture f;
  assertError(f.api.handle(Method::Put, "/api/v1/entities//value", "", "{\"value\":true}"), 404, "not-found");
  assertError(f.api.handle(Method::Put, "/api/v1/entities/dehumidifier-power", "", "{\"value\":true}"), 404,
              "not-found");
  assertError(f.api.handle(Method::Put, "/api/v1/entities/dehumidifier-power/value/extra", "", "{\"value\":true}"),
              404, "not-found");
}

// ---- GET /api/v1/history ----------------------------------------------------

namespace {

// A house that has recorded `samples` samples, one every 10 ms.
struct HistoryFixture {
  cisc::HouseSettings settings = fakes::testSettings();
  Rig rig;
  Api api;
  explicit HistoryFixture(int samples) : rig(withFastHistory(settings)), api(rig.controller, kBootId) {
    rig.controller.begin();
    rig.run(static_cast<cisc::Millis>(samples) * 10);
  }
  static cisc::HouseSettings withFastHistory(cisc::HouseSettings s) {
    s.historyIntervalMs = 10;
    return s;
  }
};

JsonDocument history(Api& api, const char* query) {
  const Response response = api.handle(Method::Get, "/api/v1/history", query, "");
  TEST_ASSERT_EQUAL_INT(200, response.status);
  return parse(response.body);
}

}  // namespace

void test_an_empty_history_is_an_empty_list() {
  Rig rig;
  Api api(rig.controller, kBootId);
  rig.controller.begin();  // just booted: nothing recorded yet
  JsonDocument body = history(api, "");
  TEST_ASSERT_EQUAL_STRING(kBootId, body["bootId"].as<const char*>());
  TEST_ASSERT_EQUAL_UINT(0, body["samples"].as<JsonArray>().size());
}

void test_samples_come_oldest_first_with_values_by_entity_id() {
  HistoryFixture f(3);
  JsonDocument body = history(f.api, "");
  JsonArray samples = body["samples"];
  TEST_ASSERT_EQUAL_UINT(3, samples.size());
  TEST_ASSERT_EQUAL_UINT32(10, samples[0]["uptimeMs"].as<uint32_t>());
  TEST_ASSERT_EQUAL_UINT32(30, samples[2]["uptimeMs"].as<uint32_t>());
  TEST_ASSERT_EQUAL_INT(50, samples[0]["values"]["gas-level"].as<int>());
}

void test_samples_without_a_valid_climate_reading_have_null_values() {
  HistoryFixture f(1);
  f.rig.climate.reading = {false, 0.0f, 0.0f};
  f.rig.run(600);  // the sensor is read again and keeps failing
  JsonDocument body = history(f.api, "");
  JsonObject last = body["samples"][body["samples"].as<JsonArray>().size() - 1];
  TEST_ASSERT_TRUE(last["values"]["temperature"].isNull());
  TEST_ASSERT_TRUE(last["values"]["humidity"].isNull());
  TEST_ASSERT_EQUAL_INT(50, last["values"]["gas-level"].as<int>());
}

void test_after_returns_only_newer_samples() {
  HistoryFixture f(5);
  JsonDocument body = history(f.api, "after=30");
  JsonArray samples = body["samples"];
  TEST_ASSERT_EQUAL_UINT(2, samples.size());
  TEST_ASSERT_EQUAL_UINT32(40, samples[0]["uptimeMs"].as<uint32_t>());
}

void test_limit_keeps_the_oldest_samples_of_the_filter() {
  HistoryFixture f(5);
  JsonDocument body = history(f.api, "after=10&limit=2");
  JsonArray samples = body["samples"];
  TEST_ASSERT_EQUAL_UINT(2, samples.size());
  TEST_ASSERT_EQUAL_UINT32(20, samples[0]["uptimeMs"].as<uint32_t>());
  TEST_ASSERT_EQUAL_UINT32(30, samples[1]["uptimeMs"].as<uint32_t>());
}

void test_a_response_without_limit_has_at_most_one_hundred_samples() {
  HistoryFixture f(150);
  TEST_ASSERT_EQUAL_UINT(100, history(f.api, "")["samples"].as<JsonArray>().size());
}

void test_a_limit_above_what_is_stored_returns_everything() {
  HistoryFixture f(5);
  TEST_ASSERT_EQUAL_UINT(5, history(f.api, "limit=100000")["samples"].as<JsonArray>().size());
}

void test_unknown_query_parameters_are_ignored() {
  HistoryFixture f(3);
  TEST_ASSERT_EQUAL_UINT(3, history(f.api, "colour=red&limit=3")["samples"].as<JsonArray>().size());
}

void test_a_bad_query_is_rejected() {
  HistoryFixture f(3);
  const char* bad[] = {"limit=0", "limit=abc", "limit=-1", "limit=", "after=abc", "after=-1", "after=99999999999"};
  for (const char* query : bad) {
    assertError(f.api.handle(Method::Get, "/api/v1/history", query, ""), 400, "invalid-query");
  }
}

void test_too_many_clients_has_the_error_of_the_contract() {
  assertError(Api::tooManyClients(), 503, "too-many-clients");
}

void test_the_event_route_asks_for_an_event_stream_client() {
  assertError(Api::notAcceptable(), 406, "not-acceptable");
}

int main() {
  UNITY_BEGIN();
  RUN_TEST(test_home_returns_the_state_of_the_house);
  RUN_TEST(test_turning_the_dehumidifier_on_runs_the_relay_and_returns_the_entity);
  RUN_TEST(test_turning_the_dehumidifier_off_stops_the_relay);
  RUN_TEST(test_setting_the_mode_to_automatic_gives_control_back_to_the_rules);
  RUN_TEST(test_setting_the_mode_to_manual_keeps_the_current_state);
  RUN_TEST(test_a_value_of_the_wrong_type_is_rejected);
  RUN_TEST(test_a_mode_that_is_not_an_option_is_rejected);
  RUN_TEST(test_the_upper_limit_is_changed_and_the_entity_comes_back);
  RUN_TEST(test_the_lower_limit_is_changed_and_the_entity_comes_back);
  RUN_TEST(test_a_limit_that_is_changed_is_saved_for_the_next_time);
  RUN_TEST(test_a_limit_outside_the_range_is_rejected);
  RUN_TEST(test_the_upper_limit_cannot_come_closer_than_two_points_to_the_lower_one);
  RUN_TEST(test_the_lower_limit_cannot_come_closer_than_two_points_to_the_upper_one);
  RUN_TEST(test_a_limit_that_is_not_a_whole_number_is_rejected);
  RUN_TEST(test_a_limit_that_is_not_a_number_is_rejected);
  RUN_TEST(test_a_rejected_limit_changes_nothing_and_saves_nothing);
  RUN_TEST(test_a_limit_does_not_take_the_dehumidifier_out_of_automatic);
  RUN_TEST(test_silencing_the_buzzer_is_a_command_and_the_entity_comes_back);
  RUN_TEST(test_enabling_the_buzzer_again_is_a_command_too);
  RUN_TEST(test_a_value_that_is_not_true_or_false_does_not_touch_the_buzzer);
  RUN_TEST(test_silencing_the_buzzer_does_not_touch_the_dehumidifier);
  RUN_TEST(test_a_rejected_command_changes_nothing);
  RUN_TEST(test_a_body_that_is_not_a_json_object_with_a_value_is_rejected);
  RUN_TEST(test_a_read_only_entity_does_not_accept_commands);
  RUN_TEST(test_an_unknown_entity_is_not_found);
  RUN_TEST(test_the_entity_is_checked_before_the_body);
  RUN_TEST(test_an_unknown_route_is_not_found);
  RUN_TEST(test_a_known_route_with_the_wrong_method_is_not_allowed);
  RUN_TEST(test_a_malformed_entity_path_is_not_found);
  RUN_TEST(test_an_empty_history_is_an_empty_list);
  RUN_TEST(test_samples_come_oldest_first_with_values_by_entity_id);
  RUN_TEST(test_samples_without_a_valid_climate_reading_have_null_values);
  RUN_TEST(test_after_returns_only_newer_samples);
  RUN_TEST(test_limit_keeps_the_oldest_samples_of_the_filter);
  RUN_TEST(test_a_response_without_limit_has_at_most_one_hundred_samples);
  RUN_TEST(test_a_limit_above_what_is_stored_returns_everything);
  RUN_TEST(test_unknown_query_parameters_are_ignored);
  RUN_TEST(test_a_bad_query_is_rejected);
  RUN_TEST(test_too_many_clients_has_the_error_of_the_contract);
  RUN_TEST(test_the_event_route_asks_for_an_event_stream_client);
  return UNITY_END();
}
