#include <unity.h>

#include "ButtonDebouncer.h"

using cisc::ButtonDebouncer;
using cisc::ButtonSettings;

namespace {

const ButtonSettings kSettings{/*debounceMs=*/20};
constexpr bool kPressed = true;
constexpr bool kReleased = false;

ButtonDebouncer releasedAt(cisc::Millis now = 0) {
  ButtonDebouncer button(kSettings);
  button.begin(kReleased, now);
  return button;
}

}  // namespace

void test_no_event_while_the_button_stays_released() {
  ButtonDebouncer button = releasedAt();
  TEST_ASSERT_FALSE(button.update(kReleased, 10));
  TEST_ASSERT_FALSE(button.update(kReleased, 1000));
}

void test_press_counts_only_after_the_debounce_time() {
  ButtonDebouncer button = releasedAt();
  TEST_ASSERT_FALSE(button.update(kPressed, 100));
  TEST_ASSERT_FALSE(button.update(kPressed, 119));
  TEST_ASSERT_TRUE(button.update(kPressed, 120));
}

void test_bounce_restarts_the_debounce_time() {
  ButtonDebouncer button = releasedAt();
  button.update(kPressed, 100);
  button.update(kReleased, 110);  // contact bounced
  button.update(kPressed, 115);
  TEST_ASSERT_FALSE(button.update(kPressed, 134));
  TEST_ASSERT_TRUE(button.update(kPressed, 135));
}

void test_holding_the_button_does_not_repeat_the_event() {
  ButtonDebouncer button = releasedAt();
  button.update(kPressed, 100);
  TEST_ASSERT_TRUE(button.update(kPressed, 120));
  TEST_ASSERT_FALSE(button.update(kPressed, 200));
  TEST_ASSERT_FALSE(button.update(kPressed, 5000));
}

void test_release_is_not_an_event() {
  ButtonDebouncer button = releasedAt();
  button.update(kPressed, 100);
  button.update(kPressed, 120);
  TEST_ASSERT_FALSE(button.update(kReleased, 300));
  TEST_ASSERT_FALSE(button.update(kReleased, 320));
}

void test_a_second_press_after_release_counts_again() {
  ButtonDebouncer button = releasedAt();
  button.update(kPressed, 100);
  button.update(kPressed, 120);
  button.update(kReleased, 300);
  button.update(kReleased, 320);
  button.update(kPressed, 400);
  TEST_ASSERT_TRUE(button.update(kPressed, 420));
}

void test_a_button_already_pressed_at_begin_is_not_an_event() {
  ButtonDebouncer button(kSettings);
  button.begin(kPressed, 0);
  TEST_ASSERT_FALSE(button.update(kPressed, 100));
}

void test_debounce_survives_millis_wraparound() {
  const cisc::Millis start = 0xFFFFFFF0u;
  ButtonDebouncer button = releasedAt(start);
  button.update(kPressed, start);
  TEST_ASSERT_FALSE(button.update(kPressed, start + 19));
  TEST_ASSERT_TRUE(button.update(kPressed, start + 20));
}

int main() {
  UNITY_BEGIN();
  RUN_TEST(test_no_event_while_the_button_stays_released);
  RUN_TEST(test_press_counts_only_after_the_debounce_time);
  RUN_TEST(test_bounce_restarts_the_debounce_time);
  RUN_TEST(test_holding_the_button_does_not_repeat_the_event);
  RUN_TEST(test_release_is_not_an_event);
  RUN_TEST(test_a_second_press_after_release_counts_again);
  RUN_TEST(test_a_button_already_pressed_at_begin_is_not_an_event);
  RUN_TEST(test_debounce_survives_millis_wraparound);
  return UNITY_END();
}
