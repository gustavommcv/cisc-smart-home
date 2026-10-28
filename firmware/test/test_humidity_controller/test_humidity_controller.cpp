#include <limits>

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

// ---- the limits can be changed, within what is allowed ---------------------------

void test_the_default_limits_are_85_and_83_with_a_gap_that_a_whole_percent_reading_respects() {
  const HumiditySettings defaults;
  TEST_ASSERT_EQUAL_FLOAT(85.0f, defaults.onAtPercent);
  TEST_ASSERT_EQUAL_FLOAT(83.0f, defaults.offAtPercent);
  TEST_ASSERT_TRUE(cisc::isValid(defaults));
}

void test_the_limits_are_the_ones_the_controller_was_made_with() {
  const HumidityController controller(kSettings);
  TEST_ASSERT_EQUAL_FLOAT(80.0f, controller.limits().onAtPercent);
  TEST_ASSERT_EQUAL_FLOAT(70.0f, controller.limits().offAtPercent);
}

void test_valid_limits_are_taken() {
  HumidityController controller(kSettings);
  TEST_ASSERT_TRUE(controller.setLimits(90.0f, 60.0f));
  TEST_ASSERT_EQUAL_FLOAT(90.0f, controller.limits().onAtPercent);
  TEST_ASSERT_EQUAL_FLOAT(60.0f, controller.limits().offAtPercent);
}

void test_the_new_limits_are_the_ones_the_rule_follows() {
  HumidityController controller(kSettings);
  controller.setLimits(60.0f, 50.0f);
  controller.update(65.0f, kValid);
  TEST_ASSERT_TRUE(controller.dehumidifierOn());
  controller.update(50.0f, kValid);
  TEST_ASSERT_FALSE(controller.dehumidifierOn());
}

void test_limits_that_are_not_valid_change_nothing() {
  HumidityController controller(kSettings);
  TEST_ASSERT_FALSE(controller.setLimits(50.0f, 60.0f));
  TEST_ASSERT_EQUAL_FLOAT(80.0f, controller.limits().onAtPercent);
  TEST_ASSERT_EQUAL_FLOAT(70.0f, controller.limits().offAtPercent);
}

void test_the_range_is_40_to_95_percent_both_ends_included() {
  TEST_ASSERT_TRUE(cisc::isValid(HumiditySettings{95.0f, 93.0f}));
  TEST_ASSERT_TRUE(cisc::isValid(HumiditySettings{42.0f, 40.0f}));
  TEST_ASSERT_FALSE(cisc::isValid(HumiditySettings{96.0f, 90.0f}));
  TEST_ASSERT_FALSE(cisc::isValid(HumiditySettings{50.0f, 39.0f}));
  TEST_ASSERT_FALSE(cisc::isValid(HumiditySettings{41.0f, 39.0f}));
}

void test_the_lower_limit_is_at_least_2_points_below_the_upper_one() {
  TEST_ASSERT_TRUE(cisc::isValid(HumiditySettings{80.0f, 78.0f}));
  TEST_ASSERT_FALSE(cisc::isValid(HumiditySettings{80.0f, 79.0f}));
  TEST_ASSERT_FALSE(cisc::isValid(HumiditySettings{80.0f, 80.0f}));
  TEST_ASSERT_FALSE(cisc::isValid(HumiditySettings{70.0f, 80.0f}));
}

void test_limits_are_whole_percents() {
  TEST_ASSERT_FALSE(cisc::isValid(HumiditySettings{85.5f, 80.0f}));
  TEST_ASSERT_FALSE(cisc::isValid(HumiditySettings{85.0f, 80.5f}));
}

void test_a_number_that_is_not_a_number_is_not_a_limit() {
  const float nan = std::numeric_limits<float>::quiet_NaN();
  TEST_ASSERT_FALSE(cisc::isValid(HumiditySettings{nan, 80.0f}));
  TEST_ASSERT_FALSE(cisc::isValid(HumiditySettings{85.0f, nan}));
}

void test_infinity_and_not_a_number_are_not_whole_numbers() {
  TEST_ASSERT_FALSE(cisc::isWholeNumber(std::numeric_limits<float>::infinity()));
  TEST_ASSERT_FALSE(cisc::isWholeNumber(-std::numeric_limits<float>::infinity()));
  TEST_ASSERT_FALSE(cisc::isWholeNumber(std::numeric_limits<float>::quiet_NaN()));
  TEST_ASSERT_TRUE(cisc::isWholeNumber(85.0f));
  TEST_ASSERT_TRUE(cisc::isWholeNumber(-3.0f));
  TEST_ASSERT_FALSE(cisc::isWholeNumber(85.5f));
}

void test_changing_the_limits_leaves_the_mode_and_the_state_alone() {
  HumidityController controller(kSettings);
  controller.setManual(true);
  controller.setLimits(90.0f, 60.0f);
  TEST_ASSERT_EQUAL(ControlMode::Manual, controller.mode());
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
  RUN_TEST(test_the_default_limits_are_85_and_83_with_a_gap_that_a_whole_percent_reading_respects);
  RUN_TEST(test_the_limits_are_the_ones_the_controller_was_made_with);
  RUN_TEST(test_valid_limits_are_taken);
  RUN_TEST(test_the_new_limits_are_the_ones_the_rule_follows);
  RUN_TEST(test_limits_that_are_not_valid_change_nothing);
  RUN_TEST(test_the_range_is_40_to_95_percent_both_ends_included);
  RUN_TEST(test_the_lower_limit_is_at_least_2_points_below_the_upper_one);
  RUN_TEST(test_limits_are_whole_percents);
  RUN_TEST(test_a_number_that_is_not_a_number_is_not_a_limit);
  RUN_TEST(test_infinity_and_not_a_number_are_not_whole_numbers);
  RUN_TEST(test_changing_the_limits_leaves_the_mode_and_the_state_alone);
  return UNITY_END();
}
