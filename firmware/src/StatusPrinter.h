#pragma once

#include "HouseController.h"
#include "Time.h"

// Prints one summary line of the house to the serial port at a fixed pace,
// for watching and debugging the maquete from a computer.
class StatusPrinter {
 public:
  StatusPrinter(const cisc::HouseController& house, cisc::Millis intervalMs)
      : house_(house), intervalMs_(intervalMs) {}

  void tick(cisc::Millis now);

 private:
  const cisc::HouseController& house_;
  cisc::Millis intervalMs_;
  cisc::Millis lastPrintAt_ = 0;
};
