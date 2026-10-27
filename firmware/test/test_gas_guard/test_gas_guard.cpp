#include <unity.h>

#include "GasGuard.h"

using cisc::GasGuard;
using cisc::GasSettings;
using cisc::GasState;
using cisc::Millis;

namespace {

// Small numbers keep the timelines in the tests easy to follow.
const GasSettings kSettings{/*threshold=*/100, /*warmUpMs=*/1000, /*clearAirMs=*/200};
constexpr int kCleanAir = 50;
constexpr int kGasLeak = 150;
constexpr bool kPressed = true;
constexpr bool kNotPressed = false;

GasGuard startedAt(Millis now = 0) {
  GasGuard guard(kSettings);
  guard.begin(now);
  return guard;
}

// A guard that finished warming up and is in NORMAL, at t = 1000.
GasGuard normalGuard() {
  GasGuard guard = startedAt(0);
  guard.update(kCleanAir, kNotPressed, 0);
  guard.update(kCleanAir, kNotPressed, 1000);
  return guard;
}

}  // namespace

void test_starts_with_valve_closed_and_alarm_off() {
  const GasGuard guard = startedAt();
  TEST_ASSERT_EQUAL(GasState::Starting, guard.state());
  TEST_ASSERT_FALSE(guard.valveOpen());
  TEST_ASSERT_FALSE(guard.alarmLedOn());
  TEST_ASSERT_FALSE(guard.buzzerOn());
}

void test_stays_starting_during_warm_up_in_clean_air() {
  GasGuard guard = startedAt();
  guard.update(kCleanAir, kNotPressed, 0);
  guard.update(kCleanAir, kNotPressed, 500);
  guard.update(kCleanAir, kNotPressed, 999);
  TEST_ASSERT_EQUAL(GasState::Starting, guard.state());
  TEST_ASSERT_FALSE(guard.valveOpen());
}

void test_becomes_normal_after_warm_up_in_clean_air() {
  const GasGuard guard = normalGuard();
  TEST_ASSERT_EQUAL(GasState::Normal, guard.state());
  TEST_ASSERT_TRUE(guard.valveOpen());
  TEST_ASSERT_FALSE(guard.alarmLedOn());
}

void test_gas_during_warm_up_triggers_emergency() {
  GasGuard guard = startedAt();
  guard.update(kGasLeak, kNotPressed, 100);
  TEST_ASSERT_EQUAL(GasState::Emergency, guard.state());
}

void test_emergency_closes_valve_and_sounds_alarm() {
  GasGuard guard = normalGuard();
  guard.update(kGasLeak, kNotPressed, 1100);
  TEST_ASSERT_EQUAL(GasState::Emergency, guard.state());
  TEST_ASSERT_TRUE(guard.gasDetected());
  TEST_ASSERT_FALSE(guard.valveOpen());
  TEST_ASSERT_TRUE(guard.alarmLedOn());
  TEST_ASSERT_TRUE(guard.buzzerOn());
}

void test_reading_equal_to_threshold_is_not_gas() {
  GasGuard guard = normalGuard();
  guard.update(kSettings.threshold, kNotPressed, 1100);
  TEST_ASSERT_FALSE(guard.gasDetected());
  TEST_ASSERT_EQUAL(GasState::Normal, guard.state());
}

void test_buzzer_stops_when_gas_clears_but_alarm_stays_latched() {
  GasGuard guard = normalGuard();
  guard.update(kGasLeak, kNotPressed, 1100);
  guard.update(kCleanAir, kNotPressed, 1200);
  TEST_ASSERT_EQUAL(GasState::Emergency, guard.state());
  TEST_ASSERT_FALSE(guard.buzzerOn());
  TEST_ASSERT_TRUE(guard.alarmLedOn());
  TEST_ASSERT_FALSE(guard.valveOpen());
}

void test_emergency_never_rearms_without_the_button() {
  GasGuard guard = normalGuard();
  guard.update(kGasLeak, kNotPressed, 1100);
  guard.update(kCleanAir, kNotPressed, 1200);
  guard.update(kCleanAir, kNotPressed, 60000);
  TEST_ASSERT_EQUAL(GasState::Emergency, guard.state());
}

void test_button_before_clear_air_time_does_not_rearm() {
  GasGuard guard = normalGuard();
  guard.update(kGasLeak, kNotPressed, 2000);
  guard.update(kCleanAir, kNotPressed, 2100);
  guard.update(kCleanAir, kPressed, 2200);  // only 100 ms of clean air
  TEST_ASSERT_EQUAL(GasState::Emergency, guard.state());
}

void test_early_button_press_is_not_remembered() {
  GasGuard guard = normalGuard();
  guard.update(kGasLeak, kNotPressed, 2000);
  guard.update(kCleanAir, kNotPressed, 2100);
  guard.update(kCleanAir, kPressed, 2200);
  guard.update(kCleanAir, kNotPressed, 2400);  // long enough now, but nobody pressed
  TEST_ASSERT_EQUAL(GasState::Emergency, guard.state());
}

void test_button_after_clear_air_time_rearms() {
  GasGuard guard = normalGuard();
  guard.update(kGasLeak, kNotPressed, 2000);
  guard.update(kCleanAir, kNotPressed, 2100);
  guard.update(kCleanAir, kPressed, 2300);
  TEST_ASSERT_EQUAL(GasState::Normal, guard.state());
  TEST_ASSERT_TRUE(guard.valveOpen());
  TEST_ASSERT_FALSE(guard.alarmLedOn());
}

void test_gas_reappearing_restarts_the_clear_air_wait() {
  GasGuard guard = normalGuard();
  guard.update(kGasLeak, kNotPressed, 2000);
  guard.update(kCleanAir, kNotPressed, 2100);
  guard.update(kGasLeak, kNotPressed, 2150);
  guard.update(kCleanAir, kNotPressed, 2160);
  guard.update(kCleanAir, kPressed, 2300);  // 140 ms since the last gas
  TEST_ASSERT_EQUAL(GasState::Emergency, guard.state());
  guard.update(kCleanAir, kPressed, 2360);  // 200 ms
  TEST_ASSERT_EQUAL(GasState::Normal, guard.state());
}

void test_rearming_during_warm_up_returns_to_starting_then_finishes_alone() {
  GasGuard guard = startedAt(0);
  guard.update(kGasLeak, kNotPressed, 100);
  guard.update(kCleanAir, kNotPressed, 200);
  guard.update(kCleanAir, kPressed, 400);
  TEST_ASSERT_EQUAL(GasState::Starting, guard.state());
  TEST_ASSERT_FALSE(guard.valveOpen());
  guard.update(kCleanAir, kNotPressed, 1000);
  TEST_ASSERT_EQUAL(GasState::Normal, guard.state());
}

void test_can_rearm_reports_the_clear_air_wait() {
  GasGuard guard = normalGuard();
  guard.update(kGasLeak, kNotPressed, 2000);
  guard.update(kCleanAir, kNotPressed, 2100);
  TEST_ASSERT_FALSE(guard.canRearm(2200));
  TEST_ASSERT_TRUE(guard.canRearm(2300));
}

void test_can_rearm_is_false_while_gas_is_present() {
  GasGuard guard = normalGuard();
  guard.update(kGasLeak, kNotPressed, 2000);
  TEST_ASSERT_FALSE(guard.canRearm(60000));
}

void test_timing_survives_millis_wraparound() {
  const Millis start = 0xFFFFFF00u;  // 256 ms before the 32-bit counter wraps
  GasGuard guard = startedAt(start);
  guard.update(kCleanAir, kNotPressed, start);
  guard.update(kCleanAir, kNotPressed, start + 999);
  TEST_ASSERT_EQUAL(GasState::Starting, guard.state());
  guard.update(kCleanAir, kNotPressed, start + 1000);  // wrapped past zero
  TEST_ASSERT_EQUAL(GasState::Normal, guard.state());
}

int main() {
  UNITY_BEGIN();
  RUN_TEST(test_starts_with_valve_closed_and_alarm_off);
  RUN_TEST(test_stays_starting_during_warm_up_in_clean_air);
  RUN_TEST(test_becomes_normal_after_warm_up_in_clean_air);
  RUN_TEST(test_gas_during_warm_up_triggers_emergency);
  RUN_TEST(test_emergency_closes_valve_and_sounds_alarm);
  RUN_TEST(test_reading_equal_to_threshold_is_not_gas);
  RUN_TEST(test_buzzer_stops_when_gas_clears_but_alarm_stays_latched);
  RUN_TEST(test_emergency_never_rearms_without_the_button);
  RUN_TEST(test_button_before_clear_air_time_does_not_rearm);
  RUN_TEST(test_early_button_press_is_not_remembered);
  RUN_TEST(test_button_after_clear_air_time_rearms);
  RUN_TEST(test_gas_reappearing_restarts_the_clear_air_wait);
  RUN_TEST(test_rearming_during_warm_up_returns_to_starting_then_finishes_alone);
  RUN_TEST(test_can_rearm_reports_the_clear_air_wait);
  RUN_TEST(test_can_rearm_is_false_while_gas_is_present);
  RUN_TEST(test_timing_survives_millis_wraparound);
  return UNITY_END();
}
