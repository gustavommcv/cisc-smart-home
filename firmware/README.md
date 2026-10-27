# Firmware

Firmware do ESP32 do CISC Smart Home. Por enquanto tem o **domínio** (as regras da casa) e a **aplicação** (quem coloca as regras para funcionar). Nenhum dos dois depende de hardware, e ambos foram escritos com testes antes do código.

## Domínio

Em [`lib/domain/src`](lib/domain/src):

| Arquivo | O que faz |
|---|---|
| `GasGuard` | Máquina de estados do gás: decide a válvula, o LED e o buzzer. Só reabre com o botão, depois de um tempo sem gás |
| `HumidityController` | Liga e desliga o desumidificador com dois limites. Modo Automático ou Manual |
| `ButtonDebouncer` | Transforma o sinal do botão, que tremula, em um único evento por aperto |
| `RingBuffer` e `Sample` | O histórico: um buffer de tamanho fixo que sobrescreve o dado mais antigo |
| `Settings.h` | Todos os limites e tempos, em um só lugar, para calibrar com o sensor real |
| `Time.h` | Comparação de tempo que continua certa quando o `millis()` dá a volta |

O código é C++ puro, sem `Arduino.h`, sem exceções e sem alocação dinâmica. Compila também com o compilador do ESP32.

## Aplicação

Em [`lib/application/src`](lib/application/src):

| Arquivo | O que faz |
|---|---|
| `Ports.h` | As **portas**: o que a aplicação precisa do mundo (relógio, sensor de gás, sensor de clima, botão e saídas liga/desliga). O hardware real as implementa nos adaptadores, e os testes usam versões falsas |
| `HouseController` | Roda a casa: lê os sensores no ritmo de cada um, entrega as leituras às regras do domínio, aciona as saídas só quando o valor muda, guarda o histórico, conta os vazamentos e recebe os comandos do app |
| `HouseSnapshot` | Tudo o que o mundo de fora pode querer saber da casa, em um instante |
| `HouseSettings.h` | O ritmo de cada leitura e o tamanho do histórico |

O `HouseController` ocupa uns 7 KB, quase todos do histórico. Por isso deve ser uma variável global, e não ficar na pilha.

O contrato de como o app enxerga esses dados está em [`docs/api.md`](../docs/api.md).

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

Na parte 3b entram os adaptadores com o hardware real e o ambiente do ESP32 no `platformio.ini`. Na 3c, a rede própria do ESP32 e o servidor da API.
