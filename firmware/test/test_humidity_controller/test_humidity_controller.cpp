#include <unity.h>

#include "HumidityController.h"

using cisc::ControlMode;
using cisc::HumidityController;
using cisc::HumiditySettings;

namespace {

const HumiditySettings kSettings{/*onAtPercent=*/80.0f, /*offAtPercent=*/70.0f};
constexpr bool kValid = true;
constexpr bool kInvalid = false;

}  // namespace

void test_starts_off_in_automatic_mode() {
  const HumidityController controller(kSettings);
  TEST_ASSERT_FALSE(controller.dehumidifierOn());
  TEST_ASSERT_EQUAL(ControlMode::Automatic, controller.mode());
}

void test_turns_on_at_the_upper_limit() {
  HumidityController controller(kSettings);
  controller.update(79.9f, kValid);
  TEST_ASSERT_FALSE(controller.dehumidifierOn());
  controller.update(80.0f, kValid);
  TEST_ASSERT_TRUE(controller.dehumidifierOn());
}

void test_stays_on_between_the_limits() {
  HumidityController controller(kSettings);
  controller.update(85.0f, kValid);
  controller.update(75.0f, kValid);
  TEST_ASSERT_TRUE(controller.dehumidifierOn());
}

void test_turns_off_at_the_lower_limit() {
  HumidityController controller(kSettings);
  controller.update(85.0f, kValid);
  controller.update(70.1f, kValid);
  TEST_ASSERT_TRUE(controller.dehumidifierOn());
  controller.update(70.0f, kValid);
  TEST_ASSERT_FALSE(controller.dehumidifierOn());
}

void test_stays_off_between_the_limits() {
  HumidityController controller(kSettings);
  controller.update(75.0f, kValid);
  TEST_ASSERT_FALSE(controller.dehumidifierOn());
}

void test_turns_off_when_the_reading_is_invalid() {
  HumidityController controller(kSettings);
  controller.update(90.0f, kValid);
  controller.update(90.0f, kInvalid);
  TEST_ASSERT_FALSE(controller.dehumidifierOn());
}

void test_manual_on_ignores_humidity() {
  HumidityController controller(kSettings);
  controller.setManual(true);
  controller.update(10.0f, kValid);
  controller.update(10.0f, kInvalid);
  TEST_ASSERT_EQUAL(ControlMode::Manual, controller.mode());
  TEST_ASSERT_TRUE(controller.dehumidifierOn());
}

void test_manual_off_ignores_humidity() {
  HumidityController controller(kSettings);
  controller.setManual(false);
  controller.update(95.0f, kValid);
  TEST_ASSERT_FALSE(controller.dehumidifierOn());
}

void test_manual_command_overrides_a_running_automatic_cycle() {
  HumidityController controller(kSettings);
  controller.update(90.0f, kValid);
  TEST_ASSERT_TRUE(controller.dehumidifierOn());
  controller.setManual(false);
  TEST_ASSERT_FALSE(controller.dehumidifierOn());
}

void test_switching_to_automatic_keeps_the_state_until_the_next_reading() {
  HumidityController controller(kSettings);
  controller.setManual(true);
  controller.setAutomatic();
  TEST_ASSERT_EQUAL(ControlMode::Automatic, controller.mode());
  TEST_ASSERT_TRUE(controller.dehumidifierOn());
}

void test_automatic_rules_apply_again_after_manual() {
  HumidityController controller(kSettings);
  controller.setManual(true);
  controller.setAutomatic();
  controller.update(60.0f, kValid);
  TEST_ASSERT_FALSE(controller.dehumidifierOn());
}

void test_automatic_inside_the_band_keeps_what_manual_left() {
  HumidityController controller(kSettings);
  controller.setManual(true);
  controller.setAutomatic();
  controller.update(75.0f, kValid);
  TEST_ASSERT_TRUE(controller.dehumidifierOn());
}

int main() {
  UNITY_BEGIN();
  RUN_TEST(test_starts_off_in_automatic_mode);
  RUN_TEST(test_turns_on_at_the_upper_limit);
  RUN_TEST(test_stays_on_between_the_limits);
  RUN_TEST(test_turns_off_at_the_lower_limit);
  RUN_TEST(test_stays_off_between_the_limits);
  RUN_TEST(test_turns_off_when_the_reading_is_invalid);
  RUN_TEST(test_manual_on_ignores_humidity);
  RUN_TEST(test_manual_off_ignores_humidity);
  RUN_TEST(test_manual_command_overrides_a_running_automatic_cycle);
  RUN_TEST(test_switching_to_automatic_keeps_the_state_until_the_next_reading);
  RUN_TEST(test_automatic_rules_apply_again_after_manual);
  RUN_TEST(test_automatic_inside_the_band_keeps_what_manual_left);
  return UNITY_END();
}
