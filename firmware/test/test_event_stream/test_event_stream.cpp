#include <string>

#include <ArduinoJson.h>
#include <unity.h>

#include "../support/Fakes.h"
#include "ApiJson.h"
#include "EventStream.h"

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
  const std::string frame = f.stream.connectFrame();
  TEST_ASSERT_TRUE(frame.rfind("event: home\ndata: ", 0) == 0);
  TEST_ASSERT_TRUE(frame.size() > 2 && frame.compare(frame.size() - 2, 2, "\n\n") == 0);
  const std::string data = frame.substr(18, frame.size() - 18 - 2);
  JsonDocument home;
  TEST_ASSERT_FALSE(deserializeJson(home, data));
  TEST_ASSERT_EQUAL_STRING(kBootId, home["bootId"].as<const char*>());
}

void test_nothing_is_sent_while_nothing_happens() {
  Fixture f;
  TEST_ASSERT_EQUAL_STRING("", f.stream.poll().c_str());
}

// ---- home events ------------------------------------------------------------

void test_a_change_is_sent_as_a_home_event() {
  Fixture f;
  f.rig.controller.turnDehumidifier(true);
  f.rig.run(100);
  const std::string frames = f.stream.poll();
  TEST_ASSERT_TRUE(frames.rfind("event: home\n", 0) == 0);
  TEST_ASSERT_TRUE(contains(frames, "\"dehumidifier-power\",\"kind\":\"switch\",\"name\":\"Power\",\"value\":true"));
}

void test_changes_are_held_back_until_the_minimum_interval_has_passed() {
  Fixture f;
  f.rig.run(10);
  f.rig.controller.turnDehumidifier(true);
  f.rig.run(50);
  TEST_ASSERT_EQUAL_STRING("", f.stream.poll().c_str());  // changed, but only 60 ms since the start
  f.rig.run(50);
  TEST_ASSERT_TRUE(contains(f.stream.poll(), "event: home"));
}

void test_the_same_state_is_not_sent_twice() {
  Fixture f;
  f.rig.controller.turnDehumidifier(true);
  f.rig.run(100);
  TEST_ASSERT_TRUE(contains(f.stream.poll(), "event: home"));
  f.rig.run(100);  // time passes, the state does not change
  TEST_ASSERT_EQUAL_STRING("", f.stream.poll().c_str());
}

void test_only_the_uptime_changing_is_not_a_change() {
  Fixture f;
  f.rig.run(300);
  TEST_ASSERT_EQUAL_STRING("", f.stream.poll().c_str());
}

// ---- keepalive --------------------------------------------------------------

void test_a_quiet_stream_sends_a_keepalive_comment() {
  Fixture f;
  f.rig.run(490);
  TEST_ASSERT_EQUAL_STRING("", f.stream.poll().c_str());
  f.rig.run(10);
  TEST_ASSERT_EQUAL_STRING(": keepalive\n\n", f.stream.poll().c_str());
}

void test_the_keepalive_is_not_repeated_right_away() {
  Fixture f;
  f.rig.run(500);
  TEST_ASSERT_EQUAL_STRING(": keepalive\n\n", f.stream.poll().c_str());
  f.rig.run(100);
  TEST_ASSERT_EQUAL_STRING("", f.stream.poll().c_str());
}

void test_sending_something_restarts_the_keepalive_clock() {
  Fixture f;
  f.rig.run(400);
  f.rig.controller.turnDehumidifier(true);
  f.rig.run(100);
  TEST_ASSERT_TRUE(contains(f.stream.poll(), "event: home"));  // t = 500: a real frame
  f.rig.run(400);
  TEST_ASSERT_EQUAL_STRING("", f.stream.poll().c_str());  // 400 ms since that frame
}

// ---- alerts -----------------------------------------------------------------

void test_a_leak_sends_an_alert_and_the_new_state_at_once() {
  Fixture f;
  f.rig.gas.value = 150;
  f.rig.run(60);  // well inside the minimum interval of home events... but an alert cannot wait
  const std::string frames = f.stream.poll();
  const size_t alert = frames.find("event: alert\n");
  const size_t home = frames.find("event: home\n");
  TEST_ASSERT_NOT_EQUAL(std::string::npos, alert);
  TEST_ASSERT_NOT_EQUAL(std::string::npos, home);
  TEST_ASSERT_TRUE_MESSAGE(alert < home, "the alert must come before the state");
}

void test_the_alert_carries_the_leak_count_in_the_contract_shape() {
  Fixture f;
  f.rig.gas.value = 150;
  f.rig.run(60);
  const std::string frames = f.stream.poll();
  const std::string expected = "event: alert\ndata: " + cisc::api::alertJson(f.rig.controller.snapshot()) + "\n\n";
  TEST_ASSERT_TRUE(frames.rfind(expected, 0) == 0);
  TEST_ASSERT_TRUE(contains(frames, "\"count\":1"));
}

void test_one_leak_is_one_alert() {
  Fixture f;
  f.rig.gas.value = 150;
  f.rig.run(60);
  f.stream.poll();
  f.rig.run(300);
  TEST_ASSERT_FALSE(contains(f.stream.poll(), "event: alert"));
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
  const std::string frames = f.stream.poll();
  TEST_ASSERT_TRUE(contains(frames, "event: alert"));
  TEST_ASSERT_TRUE(contains(frames, "\"count\":2"));
}

void test_a_stream_created_after_a_leak_does_not_repeat_the_old_alert() {
  Rig rig;
  rig.bootToNormal();
  rig.gas.value = 150;
  rig.run(100);  // the leak happens before the stream exists
  EventStream stream(rig.controller, kBootId, testEventSettings());
  rig.run(200);
  TEST_ASSERT_FALSE(contains(stream.poll(), "event: alert"));
}

int main() {
  UNITY_BEGIN();
  RUN_TEST(test_a_new_client_gets_the_whole_state_as_a_home_event);
  RUN_TEST(test_nothing_is_sent_while_nothing_happens);
  RUN_TEST(test_a_change_is_sent_as_a_home_event);
  RUN_TEST(test_changes_are_held_back_until_the_minimum_interval_has_passed);
  RUN_TEST(test_the_same_state_is_not_sent_twice);
  RUN_TEST(test_only_the_uptime_changing_is_not_a_change);
  RUN_TEST(test_a_quiet_stream_sends_a_keepalive_comment);
  RUN_TEST(test_the_keepalive_is_not_repeated_right_away);
  RUN_TEST(test_sending_something_restarts_the_keepalive_clock);
  RUN_TEST(test_a_leak_sends_an_alert_and_the_new_state_at_once);
  RUN_TEST(test_the_alert_carries_the_leak_count_in_the_contract_shape);
  RUN_TEST(test_one_leak_is_one_alert);
  RUN_TEST(test_a_second_leak_is_a_second_alert);
  RUN_TEST(test_a_stream_created_after_a_leak_does_not_repeat_the_old_alert);
  return UNITY_END();
}
