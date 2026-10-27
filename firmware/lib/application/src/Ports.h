#pragma once

#include "Time.h"

// The ports: what the application needs from the outside world. The real
// hardware implements them in the adapters; the tests use fakes.
namespace cisc {

class Clock {
 public:
  virtual ~Clock() = default;
  virtual Millis now() = 0;
};

class GasSensor {
 public:
  virtual ~GasSensor() = default;
  // MQ-2 reading on the 0-1023 scale, whatever the board's real ADC range is.
  virtual int read() = 0;
};

struct ClimateReading {
  bool valid;  // false when the sensor did not answer or gave nonsense
  float temperatureC;
  float humidityPercent;
};

class ClimateSensor {
 public:
  virtual ~ClimateSensor() = default;
  virtual ClimateReading read() = 0;
};

class Button {
 public:
  virtual ~Button() = default;
  // True while the button is held down. How the signal is wired is the
  // adapter's business.
  virtual bool isPressed() = 0;
};

// Anything that is simply on or off: the valve (open = on), the alarm LED, the
// buzzer and the dehumidifier relay.
class Output {
 public:
  virtual ~Output() = default;
  virtual void set(bool on) = 0;
};

}  // namespace cisc
