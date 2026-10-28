// Composition root: the only place that knows which concrete adapter sits behind
// each port. Everything else talks to the ports.
#include <Arduino.h>
#include <esp_random.h>

#include <memory>
#include <mutex>

#include "AccessPoint.h"
#include "Api.h"
#include "ApiServer.h"
#include "BoardConfig.h"
#include "BootId.h"
#include "Dht11ClimateSensor.h"
#include "DisplayPresenter.h"
#include "Esp32Clock.h"
#include "EventStream.h"
#include "GpioOutput.h"
#include "HouseController.h"
#include "I2cLcdDisplay.h"
#include "ModuleButton.h"
#include "Mq2GasSensor.h"
#include "NvsSettingsStore.h"
#include "OpenDrainOutput.h"
#include "ServoValve.h"
#include "StatusPrinter.h"
#include "WifiConfig.h"

namespace {

adapters::Esp32Clock systemClock;
adapters::Mq2GasSensor gasSensor(board::kGasSensorPin, board::kGasSensorWiring);
adapters::Dht11ClimateSensor climateSensor(board::kClimateSensorPin);
adapters::ModuleButton button(board::kButtonPin, board::kButtonActiveHigh);
adapters::ServoValve valve(board::kValveServoPin, board::kValveOpenDegrees, board::kValveClosedDegrees);
adapters::GpioOutput alarmLed(board::kAlarmLedPin, board::kAlarmLedActiveHigh);
adapters::GpioOutput buzzer(board::kBuzzerPin, board::kBuzzerActiveHigh);
adapters::OpenDrainOutput dehumidifier(board::kDehumidifierRelayPin);
adapters::NvsSettingsStore settingsStore;
adapters::I2cLcdDisplay lcd(board::kLcdSdaPin, board::kLcdSclPin, board::kLcdColumns, board::kLcdRows);

// A global, not a local: it keeps the whole history (about 7 KB).
cisc::HouseController house(cisc::Hardware{systemClock, gasSensor, climateSensor, button, valve, alarmLed, buzzer,
                                           dehumidifier, settingsStore});
cisc::DisplayPresenter lcdPresenter(house, lcd, board::kLcdRefreshMs);
StatusPrinter statusPrinter(house, 1000);

// The web server runs in its own task. This mutex keeps it and the main loop from
// touching the house at the same time.
std::mutex houseMutex;

adapters::AccessPoint accessPoint;
// Made once the network is up: the boot id needs a random number, and the radio has
// to be on for it to be a good one.
std::unique_ptr<cisc::api::Api> api;
std::unique_ptr<cisc::api::EventStream> eventStream;
std::unique_ptr<adapters::ApiServer> apiServer;

void startNetwork() {
  if (!accessPoint.begin(wifi::kAccessPointName, wifi::kAccessPointPassword, wifi::kChannel, wifi::kMaxStations,
                         wifi::kAddress)) {
    // The gas safety does not depend on the network, so the house keeps running.
    Serial.println("wifi: could not start the access point; running without the API");
    return;
  }
  const std::string bootId = cisc::api::bootIdFromRandom(esp_random());
  api.reset(new cisc::api::Api(house, bootId));
  eventStream.reset(new cisc::api::EventStream(house, bootId));
  apiServer.reset(new adapters::ApiServer(*api, *eventStream, houseMutex, board::kHttpPort, board::kMaxEventClients));
  apiServer->begin();
  Serial.printf("wifi: network \"%s\" at %s, boot id %s\n", wifi::kAccessPointName,
                accessPoint.address().toString().c_str(), bootId.c_str());
}

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

  startNetwork();
}

void loop() {
  {
    std::lock_guard<std::mutex> lock(houseMutex);
    house.tick();
    lcdPresenter.tick();
    statusPrinter.tick(systemClock.now());
  }
  if (apiServer) apiServer->tick();
  // Gives the web server's task a turn at the mutex; the house does not need to run
  // faster than once per millisecond.
  delay(1);
}
