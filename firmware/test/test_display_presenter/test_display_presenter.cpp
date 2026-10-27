#include <unity.h>

#include "../support/Fakes.h"
#include "DisplayPresenter.h"

using fakes::Rig;

namespace {

constexpr cisc::Millis kRefreshMs = 250;

// Advances the clock in 10 ms steps, ticking the house and then the presenter,
// like the main loop would.
void run(Rig& rig, cisc::DisplayPresenter& presenter, cisc::Millis durationMs) {
  for (cisc::Millis elapsed = 0; elapsed < durationMs; elapsed += 10) {
    rig.clock.time += 10;
    rig.controller.tick();
    presenter.tick();
  }
}

}  // namespace

void test_the_first_tick_draws_the_current_state() {
  Rig rig;
  fakes::Display display;
  cisc::DisplayPresenter presenter(rig.controller, display, kRefreshMs);
  rig.controller.begin();
  presenter.tick();
  TEST_ASSERT_EQUAL(1, display.shows);
  TEST_ASSERT_EQUAL_STRING("Inicio: aguarde", display.top);
}

void test_nothing_is_redrawn_while_the_text_does_not_change() {
  Rig rig;
  fakes::Display display;
  cisc::DisplayPresenter presenter(rig.controller, display, kRefreshMs);
  rig.controller.begin();
  run(rig, presenter, 400);  // still starting: the text stays the same
  TEST_ASSERT_EQUAL(1, display.shows);
}

void test_the_display_follows_the_state_of_the_house() {
  Rig rig;
  fakes::Display display;
  cisc::DisplayPresenter presenter(rig.controller, display, kRefreshMs);
  rig.controller.begin();
  run(rig, presenter, 1500);  // warm-up over and the first climate reading in
  TEST_ASSERT_EQUAL_STRING("Temp: 21.5 C", display.top);
  TEST_ASSERT_EQUAL_STRING("Umid: 60% OFF", display.bottom);
}

void test_a_leak_reaches_the_display_within_one_refresh() {
  Rig rig;
  fakes::Display display;
  cisc::DisplayPresenter presenter(rig.controller, display, kRefreshMs);
  rig.controller.begin();
  run(rig, presenter, 1500);
  rig.gas.value = 150;
  run(rig, presenter, kRefreshMs + 50);
  TEST_ASSERT_EQUAL_STRING("ALERTA DE GAS", display.top);
}

void test_a_change_waits_for_the_refresh_interval() {
  Rig rig;
  fakes::Display display;
  cisc::DisplayPresenter presenter(rig.controller, display, kRefreshMs);
  rig.controller.begin();
  run(rig, presenter, 1500);
  const int showsBefore = display.shows;
  rig.climate.reading = {true, 25.0f, 60.0f};
  run(rig, presenter, 3 * kRefreshMs);  // the reading arrives, then the display catches up
  TEST_ASSERT_EQUAL_STRING("Temp: 25.0 C", display.top);
  TEST_ASSERT_EQUAL(showsBefore + 1, display.shows);
}

void test_the_display_is_not_touched_again_before_the_refresh_interval() {
  Rig rig;
  fakes::Display display;
  cisc::DisplayPresenter presenter(rig.controller, display, kRefreshMs);
  rig.controller.begin();
  presenter.tick();  // t = 0: draws "Inicio: aguarde"
  rig.gas.value = 150;
  rig.run(60);       // the house sees the leak...
  presenter.tick();  // ...but only 60 ms have passed since the last draw
  TEST_ASSERT_EQUAL(1, display.shows);
  TEST_ASSERT_EQUAL_STRING("Inicio: aguarde", display.top);
  rig.run(200);      // t = 260 ms
  presenter.tick();
  TEST_ASSERT_EQUAL(2, display.shows);
  TEST_ASSERT_EQUAL_STRING("ALERTA DE GAS", display.top);
}

int main() {
  UNITY_BEGIN();
  RUN_TEST(test_the_first_tick_draws_the_current_state);
  RUN_TEST(test_nothing_is_redrawn_while_the_text_does_not_change);
  RUN_TEST(test_the_display_follows_the_state_of_the_house);
  RUN_TEST(test_a_leak_reaches_the_display_within_one_refresh);
  RUN_TEST(test_a_change_waits_for_the_refresh_interval);
  RUN_TEST(test_the_display_is_not_touched_again_before_the_refresh_interval);
  return UNITY_END();
}
