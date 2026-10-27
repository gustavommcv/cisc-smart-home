// Composition root: the only place that knows which concrete adapter sits behind
// each port. Everything else talks to the ports.
#include <Arduino.h>

#include "BoardConfig.h"
#include "Dht11ClimateSensor.h"
#include "DisplayPresenter.h"
#include "Esp32Clock.h"
#include "GpioOutput.h"
#include "HouseController.h"
#include "I2cLcdDisplay.h"
#include "ModuleButton.h"
#include "Mq2GasSensor.h"
#include "ServoValve.h"
#include "StatusPrinter.h"

namespace {

adapters::Esp32Clock systemClock;
adapters::Mq2GasSensor gasSensor(board::kGasSensorPin, board::kGasSensorWiring);
adapters::Dht11ClimateSensor climateSensor(board::kClimateSensorPin);
adapters::ModuleButton button(board::kButtonPin, board::kButtonActiveHigh);
adapters::ServoValve valve(board::kValveServoPin, board::kValveOpenDegrees, board::kValveClosedDegrees);
adapters::GpioOutput alarmLed(board::kAlarmLedPin, board::kAlarmLedActiveHigh);
adapters::GpioOutput buzzer(board::kBuzzerPin, board::kBuzzerActiveHigh);
adapters::GpioOutput dehumidifier(board::kDehumidifierRelayPin, board::kRelayActiveHigh);
adapters::I2cLcdDisplay lcd(board::kLcdSdaPin, board::kLcdSclPin, board::kLcdColumns, board::kLcdRows);

// A global, not a local: it keeps the whole history (about 7 KB).
cisc::HouseController house(cisc::Hardware{systemClock, gasSensor, climateSensor, button, valve, alarmLed, buzzer,
                                           dehumidifier});
cisc::DisplayPresenter lcdPresenter(house, lcd, board::kLcdRefreshMs);
StatusPrinter statusPrinter(house, 1000);

}  // namespace

void setup() {
  Serial.begin(board::kSerialBaud);

  // Hardware first, with every output off, then the house takes over.
  gasSensor.begin();
  climateSensor.begin();
  button.begin();
  alarmLed.begin();
  buzzer.begin();
  dehumidifier.begin();
  valve.begin();
  const bool lcdFound = lcd.begin();

  house.begin();
  Serial.println(lcdFound ? "lcd: found" : "lcd: not found, running without it");
}

void loop() {
  house.tick();
  lcdPresenter.tick();
  statusPrinter.tick(systemClock.now());
}
