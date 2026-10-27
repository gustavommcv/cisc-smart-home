#include "DisplayText.h"

#include <cstdio>

namespace cisc {

namespace {

// snprintf truncates to the buffer, so a line can never be wider than the screen.
template <std::size_t N>
void write(char (&line)[N], const char* text) {
  std::snprintf(line, N, "%s", text);
}

}  // namespace

DisplayText formatDisplay(const HouseSnapshot& snapshot) {
  DisplayText text{};
  const char* dehumidifier = snapshot.dehumidifierOn ? "LIG" : "OFF";

  if (snapshot.gasState == GasState::Emergency) {
    if (snapshot.gasDetected) {
      write(text.top, "ALERTA DE GAS");
      write(text.bottom, "Valvula FECHADA");
    } else {
      write(text.top, "Alarme retido");
      write(text.bottom, snapshot.rearmReady ? "Aperte o botao" : "Aguarde sem gas");
    }
  } else if (snapshot.gasState == GasState::Starting) {
    write(text.top, "Inicio: aguarde");
    write(text.bottom, "Valvula FECHADA");
  } else if (!snapshot.climateSampled) {
    write(text.top, "Lendo DHT11...");
    std::snprintf(text.bottom, sizeof(text.bottom), "Desumid.: %s", dehumidifier);
  } else if (!snapshot.climateValid) {
    write(text.top, "Erro no DHT11");
    std::snprintf(text.bottom, sizeof(text.bottom), "Desumid.: %s", dehumidifier);
  } else {
    const bool manual = snapshot.dehumidifierMode == ControlMode::Manual;
    std::snprintf(text.top, sizeof(text.top), "Temp: %4.1f C%s", snapshot.temperatureC, manual ? " MAN" : "");
    std::snprintf(text.bottom, sizeof(text.bottom), "Umid:%3u%% %s",
                  static_cast<unsigned>(snapshot.humidityPercent + 0.5f), dehumidifier);
  }
  return text;
}

}  // namespace cisc
