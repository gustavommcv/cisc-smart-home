#include <unity.h>

#include "../support/Fakes.h"

using cisc::ControlMode;
using cisc::GasState;
using fakes::Rig;

// ---- start-up ---------------------------------------------------------------

void test_begin_puts_every_output_in_a_safe_state() {
  Rig rig;
  rig.controller.begin();
  TEST_ASSERT_FALSE(rig.valve.value);
  TEST_ASSERT_FALSE(rig.alarmLed.value);
  TEST_ASSERT_FALSE(rig.buzzer.value);
  TEST_ASSERT_FALSE(rig.dehumidifier.value);
  TEST_ASSERT_EQUAL(1, rig.valve.writes);
  TEST_ASSERT_EQUAL(1, rig.dehumidifier.writes);
}

void test_begin_takes_a_first_gas_reading() {
  Rig rig;
  rig.controller.begin();
  TEST_ASSERT_EQUAL(1, rig.gas.reads);
}

void test_starting_in_gas_raises_the_alarm_right_away() {
  Rig rig;
  rig.gas.value = 150;
  rig.controller.begin();
  TEST_ASSERT_EQUAL(GasState::Emergency, rig.controller.snapshot().gasState);
  TEST_ASSERT_EQUAL_UINT32(1, rig.controller.snapshot().gasAlertCount);
  TEST_ASSERT_TRUE(rig.buzzer.value);
}

void test_valve_opens_after_the_warm_up_in_clean_air() {
  Rig rig;
  rig.bootToNormal();
  TEST_ASSERT_EQUAL(GasState::Normal, rig.controller.snapshot().gasState);
  TEST_ASSERT_TRUE(rig.valve.value);
  TEST_ASSERT_TRUE(rig.controller.snapshot().valveOpen);
}

// ---- sampling ---------------------------------------------------------------

void test_gas_is_sampled_at_its_own_interval_not_every_tick() {
  Rig rig;
  rig.controller.begin();
  rig.run(200);  // 20 ticks, but a sample only every 50 ms
  TEST_ASSERT_EQUAL(1 + 4, rig.gas.reads);
}

void test_climate_is_sampled_at_its_own_interval() {
  Rig rig;
  rig.controller.begin();
  rig.run(490);
  TEST_ASSERT_EQUAL(0, rig.climate.reads);
  rig.run(10);
  TEST_ASSERT_EQUAL(1, rig.climate.reads);
}

// ---- gas emergency ----------------------------------------------------------

void test_a_leak_closes_the_valve_and_sounds_the_alarm() {
  Rig rig;
  rig.bootToNormal();
  rig.gas.value = 150;
  rig.run(50);
  TEST_ASSERT_EQUAL(GasState::Emergency, rig.controller.snapshot().gasState);
  TEST_ASSERT_FALSE(rig.valve.value);
  TEST_ASSERT_TRUE(rig.alarmLed.value);
  TEST_ASSERT_TRUE(rig.buzzer.value);
}

void test_one_leak_counts_as_one_alert_however_long_it_lasts() {
  Rig rig;
  rig.bootToNormal();
  rig.gas.value = 150;
  rig.run(2000);
  TEST_ASSERT_EQUAL_UINT32(1, rig.controller.snapshot().gasAlertCount);
}

void test_the_button_rearms_after_enough_clean_air() {
  Rig rig;
  rig.bootToNormal();
  rig.gas.value = 150;
  rig.run(100);
  rig.gas.value = 50;
  rig.run(300);  // clean for longer than the clear-air wait
  TEST_ASSERT_EQUAL(GasState::Emergency, rig.controller.snapshot().gasState);

  rig.button.pressed = true;
  rig.run(30);  // longer than the debounce time
  rig.button.pressed = false;

  TEST_ASSERT_EQUAL(GasState::Normal, rig.controller.snapshot().gasState);
  TEST_ASSERT_TRUE(rig.valve.value);
  TEST_ASSERT_FALSE(rig.alarmLed.value);
}

void test_a_second_leak_is_a_second_alert() {
  Rig rig;
  rig.bootToNormal();
  rig.gas.value = 150;
  rig.run(100);
  rig.gas.value = 50;
  rig.run(300);
  rig.button.pressed = true;
  rig.run(30);
  rig.button.pressed = false;
  rig.gas.value = 150;
  rig.run(100);
  TEST_ASSERT_EQUAL_UINT32(2, rig.controller.snapshot().gasAlertCount);
}

// ---- outputs ----------------------------------------------------------------

void test_outputs_are_written_only_when_their_value_changes() {
  Rig rig;
  rig.bootToNormal();
  const int valveWrites = rig.valve.writes;
  const int buzzerWrites = rig.buzzer.writes;
  rig.run(5000);
  TEST_ASSERT_EQUAL(valveWrites, rig.valve.writes);
  TEST_ASSERT_EQUAL(buzzerWrites, rig.buzzer.writes);
  TEST_ASSERT_EQUAL(2, rig.valve.writes);  // closed at boot, then opened once
}

// ---- climate and dehumidifier -----------------------------------------------

void test_no_climate_reading_is_reported_before_the_first_sample() {
  Rig rig;
  rig.controller.begin();
  TEST_ASSERT_FALSE(rig.controller.snapshot().climateValid);
}

void test_the_snapshot_carries_the_last_climate_reading() {
  Rig rig;
  rig.climate.reading = {true, 22.0f, 55.0f};
  rig.controller.begin();
  rig.run(500);
  TEST_ASSERT_TRUE(rig.controller.snapshot().climateValid);
  TEST_ASSERT_EQUAL_FLOAT(22.0f, rig.controller.snapshot().temperatureC);
  TEST_ASSERT_EQUAL_FLOAT(55.0f, rig.controller.snapshot().humidityPercent);
}

void test_high_humidity_turns_the_dehumidifier_on_in_automatic_mode() {
  Rig rig;
  rig.climate.reading = {true, 22.0f, 85.0f};
  rig.controller.begin();
  rig.run(500);
  TEST_ASSERT_TRUE(rig.dehumidifier.value);
  TEST_ASSERT_TRUE(rig.controller.snapshot().dehumidifierOn);
  TEST_ASSERT_EQUAL(ControlMode::Automatic, rig.controller.snapshot().dehumidifierMode);
}

void test_an_invalid_climate_reading_keeps_the_dehumidifier_off() {
  Rig rig;
  rig.climate.reading = {false, 0.0f, 0.0f};
  rig.controller.begin();
  rig.run(500);
  TEST_ASSERT_FALSE(rig.controller.snapshot().climateValid);
  TEST_ASSERT_FALSE(rig.dehumidifier.value);
}

void test_turning_the_dehumidifier_on_takes_effect_at_once_and_is_manual() {
  Rig rig;
  rig.controller.begin();
  rig.controller.turnDehumidifier(true);
  TEST_ASSERT_TRUE(rig.dehumidifier.value);  // no tick needed
  TEST_ASSERT_EQUAL(ControlMode::Manual, rig.controller.snapshot().dehumidifierMode);
}

void test_manual_mode_ignores_humidity() {
  Rig rig;
  rig.climate.reading = {true, 22.0f, 40.0f};
  rig.controller.begin();
  rig.controller.turnDehumidifier(true);
  rig.run(2000);
  TEST_ASSERT_TRUE(rig.dehumidifier.value);
}

void test_back_to_automatic_applies_the_rules_on_the_next_reading() {
  Rig rig;
  rig.climate.reading = {true, 22.0f, 40.0f};
  rig.controller.begin();
  rig.controller.turnDehumidifier(true);
  rig.controller.setDehumidifierMode(ControlMode::Automatic);
  TEST_ASSERT_TRUE(rig.dehumidifier.value);  // unchanged until a new reading
  rig.run(500);
  TEST_ASSERT_FALSE(rig.dehumidifier.value);
}

void test_switching_to_manual_keeps_the_current_state() {
  Rig rig;
  rig.climate.reading = {true, 22.0f, 85.0f};
  rig.controller.begin();
  rig.run(500);
  TEST_ASSERT_TRUE(rig.dehumidifier.value);
  rig.controller.setDehumidifierMode(ControlMode::Manual);
  TEST_ASSERT_EQUAL(ControlMode::Manual, rig.controller.snapshot().dehumidifierMode);
  TEST_ASSERT_TRUE(rig.dehumidifier.value);
}

// ---- history and snapshot ---------------------------------------------------

void test_history_records_a_sample_at_each_interval() {
  Rig rig;
  rig.climate.reading = {true, 22.0f, 55.0f};
  rig.controller.begin();
  rig.run(3000);
  const auto& history = rig.controller.history();
  TEST_ASSERT_EQUAL_UINT(3, history.size());
  TEST_ASSERT_EQUAL_UINT32(1000, history.at(0).timestampMs);
  TEST_ASSERT_EQUAL_UINT32(3000, history.at(2).timestampMs);
  TEST_ASSERT_EQUAL_INT(50, history.at(2).gasReading);
  TEST_ASSERT_TRUE(history.at(2).climateValid);
  TEST_ASSERT_EQUAL_FLOAT(55.0f, history.at(2).humidityPercent);
}

void test_history_marks_samples_without_a_valid_climate_reading() {
  Rig rig;
  rig.climate.reading = {false, 0.0f, 0.0f};
  rig.controller.begin();
  rig.run(1000);
  TEST_ASSERT_FALSE(rig.controller.history().at(0).climateValid);
}

void test_snapshot_uptime_follows_the_last_tick() {
  Rig rig;
  rig.controller.begin();
  rig.run(1230);
  TEST_ASSERT_EQUAL_UINT32(1230, rig.controller.snapshot().uptimeMs);
}

void test_snapshot_reports_gas_only_while_it_is_present() {
  Rig rig;
  rig.bootToNormal();
  TEST_ASSERT_FALSE(rig.controller.snapshot().gasDetected);
  rig.gas.value = 150;
  rig.run(50);
  TEST_ASSERT_TRUE(rig.controller.snapshot().gasDetected);
  rig.gas.value = 50;
  rig.run(50);
  TEST_ASSERT_FALSE(rig.controller.snapshot().gasDetected);
}

void test_snapshot_reports_when_the_button_is_accepted_again() {
  Rig rig;
  rig.bootToNormal();
  rig.gas.value = 150;
  rig.run(100);
  rig.gas.value = 50;
  rig.run(100);  // clean for less than the clear-air wait
  TEST_ASSERT_FALSE(rig.controller.snapshot().rearmReady);
  rig.run(200);
  TEST_ASSERT_TRUE(rig.controller.snapshot().rearmReady);
}

void test_snapshot_tells_no_sample_yet_from_an_invalid_sample() {
  Rig rig;
  rig.climate.reading = {false, 0.0f, 0.0f};
  rig.controller.begin();
  TEST_ASSERT_FALSE(rig.controller.snapshot().climateSampled);
  rig.run(500);
  TEST_ASSERT_TRUE(rig.controller.snapshot().climateSampled);
  TEST_ASSERT_FALSE(rig.controller.snapshot().climateValid);
}

// ---- the limits of the dehumidifier --------------------------------------------

void test_the_snapshot_carries_the_limits_the_house_was_made_with() {
  Rig rig;
  rig.controller.begin();
  TEST_ASSERT_EQUAL_FLOAT(80.0f, rig.controller.snapshot().dehumidifierOnAt);
  TEST_ASSERT_EQUAL_FLOAT(70.0f, rig.controller.snapshot().dehumidifierOffAt);
}

void test_begin_takes_the_limits_the_user_saved() {
  Rig rig;
  rig.store.has = true;
  rig.store.humidity = cisc::HumiditySettings{75.0f, 60.0f};
  rig.controller.begin();
  TEST_ASSERT_EQUAL_FLOAT(75.0f, rig.controller.snapshot().dehumidifierOnAt);
  TEST_ASSERT_EQUAL_FLOAT(60.0f, rig.controller.snapshot().dehumidifierOffAt);
}

void test_the_saved_limits_are_the_ones_the_rule_follows() {
  Rig rig;
  rig.store.has = true;
  rig.store.humidity = cisc::HumiditySettings{60.0f, 50.0f};
  rig.climate.reading = {true, 22.0f, 65.0f};
  rig.bootToNormal();
  TEST_ASSERT_TRUE(rig.dehumidifier.value);
}

void test_saved_limits_that_are_not_valid_are_left_out_and_the_house_keeps_its_own() {
  Rig rig;
  rig.store.has = true;
  rig.store.humidity = cisc::HumiditySettings{90.0f, 89.0f};
  rig.controller.begin();
  TEST_ASSERT_EQUAL_FLOAT(80.0f, rig.controller.snapshot().dehumidifierOnAt);
  TEST_ASSERT_EQUAL_FLOAT(70.0f, rig.controller.snapshot().dehumidifierOffAt);
}

void test_nothing_saved_is_nothing_written_at_start() {
  Rig rig;
  rig.controller.begin();
  TEST_ASSERT_EQUAL(0, rig.store.saves);
}

void test_changing_the_upper_limit_shows_in_the_snapshot_and_is_saved() {
  Rig rig;
  rig.controller.begin();
  TEST_ASSERT_TRUE(rig.controller.setDehumidifierOnAt(90.0f));
  TEST_ASSERT_EQUAL_FLOAT(90.0f, rig.controller.snapshot().dehumidifierOnAt);
  TEST_ASSERT_EQUAL_FLOAT(70.0f, rig.controller.snapshot().dehumidifierOffAt);
  TEST_ASSERT_EQUAL(1, rig.store.saves);
  TEST_ASSERT_EQUAL_FLOAT(90.0f, rig.store.humidity.onAtPercent);
  TEST_ASSERT_EQUAL_FLOAT(70.0f, rig.store.humidity.offAtPercent);
}

void test_changing_the_lower_limit_shows_in_the_snapshot_and_is_saved() {
  Rig rig;
  rig.controller.begin();
  TEST_ASSERT_TRUE(rig.controller.setDehumidifierOffAt(60.0f));
  TEST_ASSERT_EQUAL_FLOAT(80.0f, rig.controller.snapshot().dehumidifierOnAt);
  TEST_ASSERT_EQUAL_FLOAT(60.0f, rig.controller.snapshot().dehumidifierOffAt);
  TEST_ASSERT_EQUAL(1, rig.store.saves);
  TEST_ASSERT_EQUAL_FLOAT(60.0f, rig.store.humidity.offAtPercent);
}

void test_a_limit_that_is_not_valid_changes_nothing_and_saves_nothing() {
  Rig rig;
  rig.controller.begin();
  TEST_ASSERT_FALSE(rig.controller.setDehumidifierOnAt(71.0f));
  TEST_ASSERT_FALSE(rig.controller.setDehumidifierOffAt(79.0f));
  TEST_ASSERT_FALSE(rig.controller.setDehumidifierOnAt(96.0f));
  TEST_ASSERT_FALSE(rig.controller.setDehumidifierOffAt(39.0f));
  TEST_ASSERT_EQUAL_FLOAT(80.0f, rig.controller.snapshot().dehumidifierOnAt);
  TEST_ASSERT_EQUAL_FLOAT(70.0f, rig.controller.snapshot().dehumidifierOffAt);
  TEST_ASSERT_EQUAL(0, rig.store.saves);
}

void test_the_new_limit_is_the_one_the_next_reading_is_judged_by() {
  Rig rig;
  rig.climate.reading = {true, 22.0f, 75.0f};
  rig.bootToNormal();
  TEST_ASSERT_FALSE(rig.dehumidifier.value);
  rig.controller.setDehumidifierOnAt(75.0f);
  rig.run(600);
  TEST_ASSERT_TRUE(rig.dehumidifier.value);
}

void test_changing_a_limit_does_not_touch_the_relay_by_itself() {
  Rig rig;
  rig.climate.reading = {true, 22.0f, 75.0f};
  rig.bootToNormal();
  const int writes = rig.dehumidifier.writes;
  rig.controller.setDehumidifierOnAt(90.0f);
  TEST_ASSERT_EQUAL(writes, rig.dehumidifier.writes);
}

// ---- the limit of the alarm is told, not copied --------------------------------

void test_the_snapshot_carries_the_limit_of_the_gas_alarm_of_the_settings() {
  cisc::HouseSettings settings = fakes::testSettings();
  settings.gas.threshold = 200;
  Rig rig(settings);
  rig.controller.begin();
  TEST_ASSERT_EQUAL_INT(200, rig.controller.snapshot().gasAlarmAt);
}

void test_the_limit_of_the_gas_alarm_is_160_when_nothing_changed_it() {
  Rig rig{cisc::HouseSettings{}};
  rig.controller.begin();
  TEST_ASSERT_EQUAL_INT(160, rig.controller.snapshot().gasAlarmAt);
}

// ---- the buzzer can be silenced ------------------------------------------------

void test_the_buzzer_is_enabled_until_someone_says_otherwise() {
  Rig rig;
  rig.controller.begin();
  TEST_ASSERT_TRUE(rig.controller.snapshot().buzzerEnabled);
}

void test_a_leak_sounds_the_buzzer_while_it_is_enabled() {
  Rig rig;
  rig.bootToNormal();
  rig.gas.value = 150;
  rig.run(200);
  TEST_ASSERT_TRUE(rig.buzzer.value);
}

void test_a_silenced_buzzer_stays_quiet_in_a_leak_and_the_rest_of_the_alarm_goes_on() {
  Rig rig;
  rig.bootToNormal();
  rig.controller.setBuzzerEnabled(false);
  rig.gas.value = 150;
  rig.run(200);
  TEST_ASSERT_FALSE(rig.buzzer.value);
  TEST_ASSERT_TRUE(rig.alarmLed.value);
  TEST_ASSERT_FALSE(rig.valve.value);
  TEST_ASSERT_EQUAL(GasState::Emergency, rig.controller.snapshot().gasState);
  TEST_ASSERT_EQUAL_UINT32(1, rig.controller.snapshot().gasAlertCount);
}

void test_silencing_the_buzzer_in_the_middle_of_a_leak_stops_it_at_once() {
  Rig rig;
  rig.bootToNormal();
  rig.gas.value = 150;
  rig.run(200);
  TEST_ASSERT_TRUE(rig.buzzer.value);
  rig.controller.setBuzzerEnabled(false);
  TEST_ASSERT_FALSE(rig.buzzer.value);
  TEST_ASSERT_FALSE(rig.controller.snapshot().buzzerEnabled);
}

void test_enabling_the_buzzer_in_the_middle_of_a_leak_sounds_it_at_once() {
  Rig rig;
  rig.bootToNormal();
  rig.controller.setBuzzerEnabled(false);
  rig.gas.value = 150;
  rig.run(200);
  TEST_ASSERT_FALSE(rig.buzzer.value);
  rig.controller.setBuzzerEnabled(true);
  TEST_ASSERT_TRUE(rig.buzzer.value);
}

void test_the_buzzer_does_not_sound_without_a_leak_whatever_it_is_set_to() {
  Rig rig;
  rig.bootToNormal();
  rig.controller.setBuzzerEnabled(true);
  rig.controller.setBuzzerEnabled(false);
  rig.controller.setBuzzerEnabled(true);
  TEST_ASSERT_FALSE(rig.buzzer.value);
}

void test_a_silenced_buzzer_stays_silenced_through_the_next_leak() {
  Rig rig;
  rig.bootToNormal();
  rig.controller.setBuzzerEnabled(false);
  rig.gas.value = 150;
  rig.run(200);
  rig.gas.value = 50;
  rig.run(400);
  rig.button.pressed = true;
  rig.run(100);
  rig.button.pressed = false;
  rig.run(100);
  rig.gas.value = 150;
  rig.run(200);
  TEST_ASSERT_FALSE(rig.buzzer.value);
  TEST_ASSERT_EQUAL_UINT32(2, rig.controller.snapshot().gasAlertCount);
}

void test_the_silence_is_not_saved_so_a_restart_brings_the_buzzer_back() {
  Rig rig;
  rig.controller.begin();
  rig.controller.setBuzzerEnabled(false);
  TEST_ASSERT_EQUAL(0, rig.store.saves);
}

void test_the_buzzer_output_is_touched_only_when_it_changes() {
  Rig rig;
  rig.bootToNormal();
  const int writes = rig.buzzer.writes;
  rig.controller.setBuzzerEnabled(false);
  rig.controller.setBuzzerEnabled(true);
  rig.controller.setBuzzerEnabled(true);
  TEST_ASSERT_EQUAL(writes, rig.buzzer.writes);
}

int main() {
  UNITY_BEGIN();
  RUN_TEST(test_begin_puts_every_output_in_a_safe_state);
  RUN_TEST(test_begin_takes_a_first_gas_reading);
  RUN_TEST(test_starting_in_gas_raises_the_alarm_right_away);
  RUN_TEST(test_valve_opens_after_the_warm_up_in_clean_air);
  RUN_TEST(test_gas_is_sampled_at_its_own_interval_not_every_tick);
  RUN_TEST(test_climate_is_sampled_at_its_own_interval);
  RUN_TEST(test_a_leak_closes_the_valve_and_sounds_the_alarm);
  RUN_TEST(test_one_leak_counts_as_one_alert_however_long_it_lasts);
  RUN_TEST(test_the_button_rearms_after_enough_clean_air);
  RUN_TEST(test_a_second_leak_is_a_second_alert);
  RUN_TEST(test_outputs_are_written_only_when_their_value_changes);
  RUN_TEST(test_no_climate_reading_is_reported_before_the_first_sample);
  RUN_TEST(test_the_snapshot_carries_the_last_climate_reading);
  RUN_TEST(test_high_humidity_turns_the_dehumidifier_on_in_automatic_mode);
  RUN_TEST(test_an_invalid_climate_reading_keeps_the_dehumidifier_off);
  RUN_TEST(test_turning_the_dehumidifier_on_takes_effect_at_once_and_is_manual);
  RUN_TEST(test_manual_mode_ignores_humidity);
  RUN_TEST(test_back_to_automatic_applies_the_rules_on_the_next_reading);
  RUN_TEST(test_switching_to_manual_keeps_the_current_state);
  RUN_TEST(test_history_records_a_sample_at_each_interval);
  RUN_TEST(test_history_marks_samples_without_a_valid_climate_reading);
  RUN_TEST(test_snapshot_uptime_follows_the_last_tick);
  RUN_TEST(test_snapshot_reports_gas_only_while_it_is_present);
  RUN_TEST(test_snapshot_reports_when_the_button_is_accepted_again);
  RUN_TEST(test_snapshot_tells_no_sample_yet_from_an_invalid_sample);
  RUN_TEST(test_the_snapshot_carries_the_limits_the_house_was_made_with);
  RUN_TEST(test_begin_takes_the_limits_the_user_saved);
  RUN_TEST(test_the_saved_limits_are_the_ones_the_rule_follows);
  RUN_TEST(test_saved_limits_that_are_not_valid_are_left_out_and_the_house_keeps_its_own);
  RUN_TEST(test_nothing_saved_is_nothing_written_at_start);
  RUN_TEST(test_changing_the_upper_limit_shows_in_the_snapshot_and_is_saved);
  RUN_TEST(test_changing_the_lower_limit_shows_in_the_snapshot_and_is_saved);
  RUN_TEST(test_a_limit_that_is_not_valid_changes_nothing_and_saves_nothing);
  RUN_TEST(test_the_new_limit_is_the_one_the_next_reading_is_judged_by);
  RUN_TEST(test_changing_a_limit_does_not_touch_the_relay_by_itself);
  RUN_TEST(test_the_snapshot_carries_the_limit_of_the_gas_alarm_of_the_settings);
  RUN_TEST(test_the_limit_of_the_gas_alarm_is_160_when_nothing_changed_it);
  RUN_TEST(test_the_buzzer_is_enabled_until_someone_says_otherwise);
  RUN_TEST(test_a_leak_sounds_the_buzzer_while_it_is_enabled);
  RUN_TEST(test_a_silenced_buzzer_stays_quiet_in_a_leak_and_the_rest_of_the_alarm_goes_on);
  RUN_TEST(test_silencing_the_buzzer_in_the_middle_of_a_leak_stops_it_at_once);
  RUN_TEST(test_enabling_the_buzzer_in_the_middle_of_a_leak_sounds_it_at_once);
  RUN_TEST(test_the_buzzer_does_not_sound_without_a_leak_whatever_it_is_set_to);
  RUN_TEST(test_a_silenced_buzzer_stays_silenced_through_the_next_leak);
  RUN_TEST(test_the_silence_is_not_saved_so_a_restart_brings_the_buzzer_back);
  RUN_TEST(test_the_buzzer_output_is_touched_only_when_it_changes);
  return UNITY_END();
}
