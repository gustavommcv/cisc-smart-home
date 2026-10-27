#include <cmath>

#include <unity.h>

#include "ClimateCheck.h"
#include "Mq2Scale.h"

using cisc::isPlausibleClimate;
using cisc::Mq2Wiring;
using cisc::mq2ReadingFromPinMillivolts;

namespace {

// The maquete: 10 kΩ + 20 kΩ divider, so the pin sees two thirds of the sensor
// output, and the MQ-2 module runs on 5 V.
const Mq2Wiring kDivider{/*upperOhms=*/10000.0f, /*lowerOhms=*/20000.0f, /*supplyMillivolts=*/5000.0f};
// No divider at all: the pin sees the sensor output directly.
const Mq2Wiring kDirect{/*upperOhms=*/0.0f, /*lowerOhms=*/10000.0f, /*supplyMillivolts=*/5000.0f};

}  // namespace

// ---- MQ-2 scale -------------------------------------------------------------

void test_zero_volts_reads_zero() {
  TEST_ASSERT_EQUAL_INT(0, mq2ReadingFromPinMillivolts(0.0f, kDivider));
}

void test_the_divider_is_undone_before_scaling() {
  // 1000 mV at the pin is 1500 mV at the sensor: 1500 / 5000 of 1023 = 306.9.
  TEST_ASSERT_EQUAL_INT(307, mq2ReadingFromPinMillivolts(1000.0f, kDivider));
}

void test_full_sensor_output_reads_the_top_of_the_scale() {
  TEST_ASSERT_EQUAL_INT(1023, mq2ReadingFromPinMillivolts(3333.4f, kDivider));
}

void test_a_reading_above_the_scale_is_clamped() {
  TEST_ASSERT_EQUAL_INT(1023, mq2ReadingFromPinMillivolts(4000.0f, kDivider));
}

void test_a_negative_voltage_is_clamped_to_zero() {
  TEST_ASSERT_EQUAL_INT(0, mq2ReadingFromPinMillivolts(-50.0f, kDivider));
}

void test_a_direct_connection_scales_without_a_divider() {
  TEST_ASSERT_EQUAL_INT(512, mq2ReadingFromPinMillivolts(2500.0f, kDirect));
}

void test_the_scale_follows_the_supply_voltage() {
  const Mq2Wiring wiring{0.0f, 10000.0f, 3300.0f};
  TEST_ASSERT_EQUAL_INT(1023, mq2ReadingFromPinMillivolts(3300.0f, wiring));
}

void test_the_default_wiring_is_the_maquete() {
  TEST_ASSERT_EQUAL_INT(mq2ReadingFromPinMillivolts(1000.0f, kDivider), mq2ReadingFromPinMillivolts(1000.0f));
}

// ---- climate plausibility ---------------------------------------------------

void test_a_normal_reading_is_plausible() {
  TEST_ASSERT_TRUE(isPlausibleClimate(21.4f, 74.1f));
}

void test_the_limits_themselves_are_plausible() {
  TEST_ASSERT_TRUE(isPlausibleClimate(-40.0f, 0.0f));
  TEST_ASSERT_TRUE(isPlausibleClimate(80.0f, 100.0f));
}

void test_a_missing_value_is_not_plausible() {
  TEST_ASSERT_FALSE(isPlausibleClimate(NAN, 50.0f));
  TEST_ASSERT_FALSE(isPlausibleClimate(20.0f, NAN));
}

void test_humidity_out_of_range_is_not_plausible() {
  TEST_ASSERT_FALSE(isPlausibleClimate(20.0f, -0.1f));
  TEST_ASSERT_FALSE(isPlausibleClimate(20.0f, 100.1f));
}

void test_temperature_out_of_range_is_not_plausible() {
  TEST_ASSERT_FALSE(isPlausibleClimate(-40.1f, 50.0f));
  TEST_ASSERT_FALSE(isPlausibleClimate(80.1f, 50.0f));
}

void test_an_all_zero_reading_is_the_signature_of_a_dead_sensor() {
  // A DHT11 with a broken wire can answer "0 and 0" with a valid checksum.
  TEST_ASSERT_FALSE(isPlausibleClimate(0.0f, 0.0f));
  TEST_ASSERT_TRUE(isPlausibleClimate(0.0f, 30.0f));  // freezing but humid is fine
  TEST_ASSERT_TRUE(isPlausibleClimate(20.0f, 0.0f));  // ...and so is bone dry
}

int main() {
  UNITY_BEGIN();
  RUN_TEST(test_zero_volts_reads_zero);
  RUN_TEST(test_the_divider_is_undone_before_scaling);
  RUN_TEST(test_full_sensor_output_reads_the_top_of_the_scale);
  RUN_TEST(test_a_reading_above_the_scale_is_clamped);
  RUN_TEST(test_a_negative_voltage_is_clamped_to_zero);
  RUN_TEST(test_a_direct_connection_scales_without_a_divider);
  RUN_TEST(test_the_scale_follows_the_supply_voltage);
  RUN_TEST(test_the_default_wiring_is_the_maquete);
  RUN_TEST(test_a_normal_reading_is_plausible);
  RUN_TEST(test_the_limits_themselves_are_plausible);
  RUN_TEST(test_a_missing_value_is_not_plausible);
  RUN_TEST(test_humidity_out_of_range_is_not_plausible);
  RUN_TEST(test_temperature_out_of_range_is_not_plausible);
  RUN_TEST(test_an_all_zero_reading_is_the_signature_of_a_dead_sensor);
  return UNITY_END();
}
