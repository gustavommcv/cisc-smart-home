#include <unity.h>

#include "Names.h"

using cisc::ControlMode;
using cisc::GasState;
using cisc::toString;

// These strings are part of the API contract (docs/api.md): changing one is a
// breaking change.

void test_gas_states_use_the_names_of_the_contract() {
  TEST_ASSERT_EQUAL_STRING("starting", toString(GasState::Starting));
  TEST_ASSERT_EQUAL_STRING("normal", toString(GasState::Normal));
  TEST_ASSERT_EQUAL_STRING("emergency", toString(GasState::Emergency));
}

void test_control_modes_use_the_names_of_the_contract() {
  TEST_ASSERT_EQUAL_STRING("automatic", toString(ControlMode::Automatic));
  TEST_ASSERT_EQUAL_STRING("manual", toString(ControlMode::Manual));
}

int main() {
  UNITY_BEGIN();
  RUN_TEST(test_gas_states_use_the_names_of_the_contract);
  RUN_TEST(test_control_modes_use_the_names_of_the_contract);
  return UNITY_END();
}
