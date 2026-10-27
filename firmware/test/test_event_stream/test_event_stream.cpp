#include <string>
#include <vector>

#include <ArduinoJson.h>
#include <unity.h>

#include "../support/Fakes.h"
#include "ApiJson.h"
#include "EventStream.h"

using cisc::api::Event;
using cisc::api::EventSettings;
using cisc::api::EventStream;
using fakes::Rig;

namespace {

constexpr const char* kBootId = "5f3a9c21";

// Short intervals keep the timelines readable.
EventSettings testEventSettings() {
  EventSettings settings;
  settings.homeMinIntervalMs = 100;
  settings.keepaliveMs = 500;
  return settings;
}

bool contains(const std::string& text, const std::string& part) {
  return text.find(part) != std::string::npos;
}

// The index of the first event with this name, or -1.
int indexOf(const std::vector<Event>& events, const char* name) {
  for (size_t i = 0; i < events.size(); ++i) {
    if (events[i].name == name) return static_cast<int>(i);
  }
  return -1;
}

struct Fixture {
  Rig rig;
  EventStream stream;
  Fixture() : stream(startedHouse(rig), kBootId, testEventSettings()) {}
  static cisc::HouseController& startedHouse(Rig& rig) {
    rig.bootToNormal();
    return rig.controller;
  }
};

}  // namespace

// ---- a client that just connected -------------------------------------------

void test_a_new_client_gets_the_whole_state_as_a_home_event() {
  Fixture f;
  const Event event = f.stream.connectEvent();
  TEST_ASSERT_EQUAL_STRING("home", event.name.c_str());
  JsonDocument home;
  TEST_ASSERT_FALSE(deserializeJson(home, event.data));
  TEST_ASSERT_EQUAL_STRING(kBootId, home["bootId"].as<const char*>());
}

void test_nothing_is_sent_while_nothing_happens() {
  Fixture f;
  TEST_ASSERT_EQUAL_UINT(0, f.stream.poll().size());
}

// ---- home events ------------------------------------------------------------

void test_a_change_is_sent_as_a_home_event() {
  Fixture f;
  f.rig.controller.turnDehumidifier(true);
  f.rig.run(100);
  const std::vector<Event> events = f.stream.poll();
  TEST_ASSERT_EQUAL_UINT(1, events.size());
  TEST_ASSERT_EQUAL_STRING("home", events[0].name.c_str());
  TEST_ASSERT_TRUE(contains(events[0].data, "\"dehumidifier-power\",\"kind\":\"switch\",\"name\":\"Power\",\"value\":true"));
}

void test_changes_are_held_back_until_the_minimum_interval_has_passed() {
  Fixture f;
  f.rig.run(10);
  f.rig.controller.turnDehumidifier(true);
  f.rig.run(50);
  TEST_ASSERT_EQUAL_UINT(0, f.stream.poll().size());  // changed, but only 60 ms since the start
  f.rig.run(50);
  TEST_ASSERT_EQUAL(0, indexOf(f.stream.poll(), "home"));
}

void test_the_same_state_is_not_sent_twice() {
  Fixture f;
  f.rig.controller.turnDehumidifier(true);
  f.rig.run(100);
  TEST_ASSERT_EQUAL(0, indexOf(f.stream.poll(), "home"));
  f.rig.run(100);  // time passes, the state does not change
  TEST_ASSERT_EQUAL_UINT(0, f.stream.poll().size());
}

void test_only_the_uptime_changing_is_not_a_change() {
  Fixture f;
  f.rig.run(300);
  TEST_ASSERT_EQUAL_UINT(0, f.stream.poll().size());
}

// ---- keepalive --------------------------------------------------------------

void test_a_quiet_stream_sends_an_empty_ping() {
  Fixture f;
  f.rig.run(490);
  TEST_ASSERT_EQUAL_UINT(0, f.stream.poll().size());
  f.rig.run(10);
  const std::vector<Event> events = f.stream.poll();
  TEST_ASSERT_EQUAL_UINT(1, events.size());
  TEST_ASSERT_EQUAL_STRING("ping", events[0].name.c_str());
  TEST_ASSERT_EQUAL_STRING("", events[0].data.c_str());
}

void test_the_ping_is_not_repeated_right_away() {
  Fixture f;
  f.rig.run(500);
  TEST_ASSERT_EQUAL(0, indexOf(f.stream.poll(), "ping"));
  f.rig.run(100);
  TEST_ASSERT_EQUAL_UINT(0, f.stream.poll().size());
}

void test_sending_something_restarts_the_keepalive_clock() {
  Fixture f;
  f.rig.run(400);
  f.rig.controller.turnDehumidifier(true);
  f.rig.run(100);
  TEST_ASSERT_EQUAL(0, indexOf(f.stream.poll(), "home"));  // t = 500: a real event
  f.rig.run(400);
  TEST_ASSERT_EQUAL_UINT(0, f.stream.poll().size());  // 400 ms since that event
}

// ---- alerts -----------------------------------------------------------------

void test_a_leak_sends_an_alert_and_the_new_state_at_once() {
  Fixture f;
  f.rig.gas.value = 150;
  f.rig.run(60);  // inside the minimum interval of home events, but an alert cannot wait
  const std::vector<Event> events = f.stream.poll();
  TEST_ASSERT_EQUAL_UINT(2, events.size());
  TEST_ASSERT_EQUAL_STRING("alert", events[0].name.c_str());
  TEST_ASSERT_EQUAL_STRING("home", events[1].name.c_str());
}

void test_the_alert_carries_the_leak_count_in_the_contract_shape() {
  Fixture f;
  f.rig.gas.value = 150;
  f.rig.run(60);
  const std::vector<Event> events = f.stream.poll();
  TEST_ASSERT_EQUAL_STRING(cisc::api::alertJson(f.rig.controller.snapshot()).c_str(), events[0].data.c_str());
  TEST_ASSERT_TRUE(contains(events[0].data, "\"count\":1"));
}

void test_one_leak_is_one_alert() {
  Fixture f;
  f.rig.gas.value = 150;
  f.rig.run(60);
  f.stream.poll();
  f.rig.run(300);
  TEST_ASSERT_EQUAL(-1, indexOf(f.stream.poll(), "alert"));
}

void test_a_second_leak_is_a_second_alert() {
  Fixture f;
  f.rig.gas.value = 150;
  f.rig.run(60);
  f.stream.poll();
  f.rig.gas.value = 50;
  f.rig.run(300);
  f.rig.button.pressed = true;
  f.rig.run(30);
  f.rig.button.pressed = false;
  f.stream.poll();
  f.rig.gas.value = 150;
  f.rig.run(60);
  const std::vector<Event> events = f.stream.poll();
  const int alert = indexOf(events, "alert");
  TEST_ASSERT_NOT_EQUAL(-1, alert);
  TEST_ASSERT_TRUE(contains(events[alert].data, "\"count\":2"));
}

void test_a_stream_created_after_a_leak_does_not_repeat_the_old_alert() {
  Rig rig;
  rig.bootToNormal();
  rig.gas.value = 150;
  rig.run(100);  // the leak happens before the stream exists
  EventStream stream(rig.controller, kBootId, testEventSettings());
  rig.run(200);
  TEST_ASSERT_EQUAL(-1, indexOf(stream.poll(), "alert"));
}

int main() {
  UNITY_BEGIN();
  RUN_TEST(test_a_new_client_gets_the_whole_state_as_a_home_event);
  RUN_TEST(test_nothing_is_sent_while_nothing_happens);
  RUN_TEST(test_a_change_is_sent_as_a_home_event);
  RUN_TEST(test_changes_are_held_back_until_the_minimum_interval_has_passed);
  RUN_TEST(test_the_same_state_is_not_sent_twice);
  RUN_TEST(test_only_the_uptime_changing_is_not_a_change);
  RUN_TEST(test_a_quiet_stream_sends_an_empty_ping);
  RUN_TEST(test_the_ping_is_not_repeated_right_away);
  RUN_TEST(test_sending_something_restarts_the_keepalive_clock);
  RUN_TEST(test_a_leak_sends_an_alert_and_the_new_state_at_once);
  RUN_TEST(test_the_alert_carries_the_leak_count_in_the_contract_shape);
  RUN_TEST(test_one_leak_is_one_alert);
  RUN_TEST(test_a_second_leak_is_a_second_alert);
  RUN_TEST(test_a_stream_created_after_a_leak_does_not_repeat_the_old_alert);
  return UNITY_END();
}
