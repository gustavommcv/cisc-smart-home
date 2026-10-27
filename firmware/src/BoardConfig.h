#pragma once

#include <cstddef>
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

// The 16x2 LCD with an I2C backpack. It runs on 5 V, so SDA and SCL go through a
// 3.3 V <-> 5 V level shifter.
constexpr std::uint8_t kLcdSdaPin = 21;
constexpr std::uint8_t kLcdSclPin = 22;
constexpr std::uint8_t kLcdColumns = 16;
constexpr std::uint8_t kLcdRows = 2;
constexpr unsigned long kLcdRefreshMs = 250;

// The 3-pin button module outputs HIGH while pressed.
constexpr bool kButtonActiveHigh = true;
// The relay module switches on when its input is pulled LOW and does not release
// reliably when the pin is driven to 3.3 V (measured on the maquete: pin HIGH after
// LOW keeps it on, a floating pin turns it off). So the relay pin is open-drain.
constexpr bool kBuzzerActiveHigh = true;
constexpr bool kAlarmLedActiveHigh = true;

constexpr std::uint8_t kValveOpenDegrees = 80;
constexpr std::uint8_t kValveClosedDegrees = 15;

// 10 kOhm in series and 20 kOhm to GND, with the MQ-2 module on 5 V.
constexpr cisc::Mq2Wiring kGasSensorWiring{10000.0f, 20000.0f, 5000.0f};

constexpr unsigned long kSerialBaud = 115200;

// The API server.
constexpr std::uint16_t kHttpPort = 80;
// Each listening client takes memory and a socket; the maquete needs one or two.
constexpr std::size_t kMaxEventClients = 3;

}  // namespace board
