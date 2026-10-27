# Firmware

Firmware do ESP32 do CISC Smart Home. Esta fase tem só o **domínio**: as regras da casa, sem nenhuma dependência de hardware, escritas com testes antes do código.

## O que existe

Tudo está em [`lib/domain/src`](lib/domain/src):

| Arquivo | O que faz |
|---|---|
| `GasGuard` | Máquina de estados do gás: decide a válvula, o LED e o buzzer. Só reabre com o botão, depois de um tempo sem gás |
| `HumidityController` | Liga e desliga o desumidificador com dois limites. Modo Automático ou Manual |
| `ButtonDebouncer` | Transforma o sinal do botão, que tremula, em um único evento por aperto |
| `RingBuffer` e `Sample` | O histórico: um buffer de tamanho fixo que sobrescreve o dado mais antigo |
| `Settings.h` | Todos os limites e tempos, em um só lugar, para calibrar com o sensor real |
| `Time.h` | Comparação de tempo que continua certa quando o `millis()` dá a volta |

O código é C++ puro, sem `Arduino.h`, sem exceções e sem alocação dinâmica. Compila também com o compilador do ESP32.

## Rodar os testes

Não precisa da placa. Precisa do [PlatformIO](https://platformio.org) e de um compilador C++ no computador.

```bash
cd firmware
pio test -e native
```

Cada pasta em `test/` é um grupo de testes e roda separado. Para rodar só um:

```bash
pio test -e native -f test_gas_guard
```

## O que vem depois

Na fase 3 entram as portas (interfaces para sensores, relé e relógio), os adaptadores com o hardware real, a rede própria do ESP32 e a API. É quando o ambiente do ESP32 é acrescentado ao `platformio.ini`.
