#include <unity.h>

#include "BootId.h"

using cisc::api::bootIdFromRandom;

void test_a_random_number_becomes_eight_lowercase_hex_digits() {
  TEST_ASSERT_EQUAL_STRING("5f3a9c21", bootIdFromRandom(0x5F3A9C21u).c_str());
  TEST_ASSERT_EQUAL_STRING("deadbeef", bootIdFromRandom(0xDEADBEEFu).c_str());
}

void test_small_numbers_are_padded_with_zeros() {
  TEST_ASSERT_EQUAL_STRING("00000000", bootIdFromRandom(0).c_str());
  TEST_ASSERT_EQUAL_STRING("000000ff", bootIdFromRandom(0xFFu).c_str());
}

void test_the_largest_number_fits() {
  TEST_ASSERT_EQUAL_STRING("ffffffff", bootIdFromRandom(0xFFFFFFFFu).c_str());
}

int main() {
  UNITY_BEGIN();
  RUN_TEST(test_a_random_number_becomes_eight_lowercase_hex_digits);
  RUN_TEST(test_small_numbers_are_padded_with_zeros);
  RUN_TEST(test_the_largest_number_fits);
  return UNITY_END();
}
