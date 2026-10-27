#pragma once

#include <cstdint>

#include "Mq2Scale.h"

// How the maquete is wired to the ESP32 DevKit (38 pins). The numbers are GPIO
// numbers, the "G.." labels printed on the board.
namespace board {

// GPIO 34 belongs to ADC1, which keeps working when Wi-Fi is on (ADC2 does not).
constexpr std::uint8_t kGasSensorPin = 34;
constexpr std::uint8_t kClimateSensorPin = 4;
constexpr std::uint8_t kButtonPin = 27;
constexpr std::uint8_t kDehumidifierRelayPin = 26;
constexpr std::uint8_t kBuzzerPin = 25;
constexpr std::uint8_t kValveServoPin = 13;
constexpr std::uint8_t kAlarmLedPin = 2;

// The 3-pin button module outputs HIGH while pressed.
constexpr bool kButtonActiveHigh = true;
// The relay module switches on when its input is pulled LOW.
constexpr bool kRelayActiveHigh = false;
constexpr bool kBuzzerActiveHigh = true;
constexpr bool kAlarmLedActiveHigh = true;

constexpr std::uint8_t kValveOpenDegrees = 80;
constexpr std::uint8_t kValveClosedDegrees = 15;

// 10 kOhm in series and 20 kOhm to GND, with the MQ-2 module on 5 V.
constexpr cisc::Mq2Wiring kGasSensorWiring{10000.0f, 20000.0f, 5000.0f};

constexpr unsigned long kSerialBaud = 115200;

}  // namespace board
