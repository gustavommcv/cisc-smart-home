#include <cstring>

#include <unity.h>

#include "DisplayText.h"

using cisc::ControlMode;
using cisc::formatDisplay;
using cisc::GasState;
using cisc::HouseSnapshot;

namespace {

// A healthy house in NORMAL, with a valid climate reading.
HouseSnapshot normalHouse() {
  HouseSnapshot snapshot{};
  snapshot.gasState = GasState::Normal;
  snapshot.valveOpen = true;
  snapshot.climateSampled = true;
  snapshot.climateValid = true;
  snapshot.temperatureC = 21.4f;
  snapshot.humidityPercent = 74.0f;
  snapshot.dehumidifierMode = ControlMode::Automatic;
  return snapshot;
}

}  // namespace

// ---- gas states -------------------------------------------------------------

void test_starting_asks_to_wait_with_the_valve_closed() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.gasState = GasState::Starting;
  snapshot.valveOpen = false;
  const auto text = formatDisplay(snapshot);
  TEST_ASSERT_EQUAL_STRING("Inicio: aguarde", text.top);
  TEST_ASSERT_EQUAL_STRING("Valvula FECHADA", text.bottom);
}

void test_a_leak_shows_the_alert() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.gasState = GasState::Emergency;
  snapshot.gasDetected = true;
  snapshot.valveOpen = false;
  const auto text = formatDisplay(snapshot);
  TEST_ASSERT_EQUAL_STRING("ALERTA DE GAS", text.top);
  TEST_ASSERT_EQUAL_STRING("Valvula FECHADA", text.bottom);
}

void test_a_latched_alarm_asks_to_wait_for_clean_air() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.gasState = GasState::Emergency;
  snapshot.gasDetected = false;
  snapshot.rearmReady = false;
  const auto text = formatDisplay(snapshot);
  TEST_ASSERT_EQUAL_STRING("Alarme retido", text.top);
  TEST_ASSERT_EQUAL_STRING("Aguarde sem gas", text.bottom);
}

void test_a_latched_alarm_asks_for_the_button_once_it_can_rearm() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.gasState = GasState::Emergency;
  snapshot.gasDetected = false;
  snapshot.rearmReady = true;
  const auto text = formatDisplay(snapshot);
  TEST_ASSERT_EQUAL_STRING("Alarme retido", text.top);
  TEST_ASSERT_EQUAL_STRING("Aperte o botao", text.bottom);
}

// ---- climate ----------------------------------------------------------------

void test_before_the_first_climate_reading_it_says_so() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.climateSampled = false;
  snapshot.climateValid = false;
  const auto text = formatDisplay(snapshot);
  TEST_ASSERT_EQUAL_STRING("Lendo DHT11...", text.top);
  TEST_ASSERT_EQUAL_STRING("Desumid.: OFF", text.bottom);
}

void test_a_failed_climate_reading_reports_the_error() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.climateValid = false;
  const auto text = formatDisplay(snapshot);
  TEST_ASSERT_EQUAL_STRING("Erro no DHT11", text.top);
  TEST_ASSERT_EQUAL_STRING("Desumid.: OFF", text.bottom);
}

void test_a_valid_reading_shows_temperature_and_humidity() {
  const auto text = formatDisplay(normalHouse());
  TEST_ASSERT_EQUAL_STRING("Temp: 21.4 C", text.top);
  TEST_ASSERT_EQUAL_STRING("Umid: 74% OFF", text.bottom);
}

void test_humidity_is_rounded_to_the_nearest_percent() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.humidityPercent = 74.6f;
  TEST_ASSERT_EQUAL_STRING("Umid: 75% OFF", formatDisplay(snapshot).bottom);
}

void test_the_dehumidifier_state_is_shown() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.dehumidifierOn = true;
  TEST_ASSERT_EQUAL_STRING("Umid: 74% LIG", formatDisplay(snapshot).bottom);
}

void test_the_dehumidifier_state_is_shown_even_without_a_reading() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.climateValid = false;
  snapshot.dehumidifierOn = true;  // turned on by hand
  TEST_ASSERT_EQUAL_STRING("Desumid.: LIG", formatDisplay(snapshot).bottom);
}

void test_manual_mode_is_flagged_on_the_first_line() {
  HouseSnapshot snapshot = normalHouse();
  snapshot.dehumidifierMode = ControlMode::Manual;
  TEST_ASSERT_EQUAL_STRING("Temp: 21.4 C MAN", formatDisplay(snapshot).top);
}

// ---- fit --------------------------------------------------------------------

void test_no_line_is_longer_than_the_screen() {
  for (int state = 0; state < 3; ++state) {
    for (int flags = 0; flags < 64; ++flags) {
      for (float humidity = 0.0f; humidity <= 100.0f; humidity += 12.5f) {
        for (float temperature = -40.0f; temperature <= 80.0f; temperature += 30.0f) {
          HouseSnapshot snapshot{};
          snapshot.gasState = static_cast<GasState>(state);
          snapshot.gasDetected = flags & 1;
          snapshot.rearmReady = flags & 2;
          snapshot.climateSampled = flags & 4;
          snapshot.climateValid = flags & 8;
          snapshot.dehumidifierOn = flags & 16;
          snapshot.dehumidifierMode = (flags & 32) ? ControlMode::Manual : ControlMode::Automatic;
          snapshot.humidityPercent = humidity;
          snapshot.temperatureC = temperature;
          const auto text = formatDisplay(snapshot);
          TEST_ASSERT_LESS_OR_EQUAL_UINT(16, std::strlen(text.top));
          TEST_ASSERT_LESS_OR_EQUAL_UINT(16, std::strlen(text.bottom));
        }
      }
    }
  }
}

int main() {
  UNITY_BEGIN();
  RUN_TEST(test_starting_asks_to_wait_with_the_valve_closed);
  RUN_TEST(test_a_leak_shows_the_alert);
  RUN_TEST(test_a_latched_alarm_asks_to_wait_for_clean_air);
  RUN_TEST(test_a_latched_alarm_asks_for_the_button_once_it_can_rearm);
  RUN_TEST(test_before_the_first_climate_reading_it_says_so);
  RUN_TEST(test_a_failed_climate_reading_reports_the_error);
  RUN_TEST(test_a_valid_reading_shows_temperature_and_humidity);
  RUN_TEST(test_humidity_is_rounded_to_the_nearest_percent);
  RUN_TEST(test_the_dehumidifier_state_is_shown);
  RUN_TEST(test_the_dehumidifier_state_is_shown_even_without_a_reading);
  RUN_TEST(test_manual_mode_is_flagged_on_the_first_line);
  RUN_TEST(test_no_line_is_longer_than_the_screen);
  return UNITY_END();
}
