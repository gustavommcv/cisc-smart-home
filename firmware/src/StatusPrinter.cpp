#include "StatusPrinter.h"

#include <Arduino.h>

#include "Names.h"

void StatusPrinter::tick(cisc::Millis now) {
  if (!cisc::hasElapsed(now, lastPrintAt_, intervalMs_)) return;
  lastPrintAt_ = now;

  const cisc::HouseSnapshot s = house_.snapshot();
  Serial.printf("gas=%d state=%s valve=%s alerts=%lu | ", s.gasReading, cisc::toString(s.gasState),
                s.valveOpen ? "open" : "closed", static_cast<unsigned long>(s.gasAlertCount));
  if (s.climateValid) {
    Serial.printf("climate=%.1fC/%.1f%% | ", s.temperatureC, s.humidityPercent);
  } else {
    Serial.print("climate=n/a | ");
  }
  Serial.printf("dehumidifier=%s mode=%s\n", s.dehumidifierOn ? "on" : "off", cisc::toString(s.dehumidifierMode));
}
