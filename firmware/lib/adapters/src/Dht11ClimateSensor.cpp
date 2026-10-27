#include "Dht11ClimateSensor.h"

#include "ClimateCheck.h"

namespace adapters {

cisc::ClimateReading Dht11ClimateSensor::read() {
  const float humidity = sensor_.readHumidity();
  const float temperature = sensor_.readTemperature();
  if (!cisc::isPlausibleClimate(temperature, humidity)) return {false, 0.0f, 0.0f};
  return {true, temperature, humidity};
}

}  // namespace adapters
