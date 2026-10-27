# Firmware

Firmware do ESP32 do CISC Smart Home. Tem o **domínio** (as regras da casa), a **aplicação** (quem coloca as regras para funcionar) e os **adaptadores** (as peças reais). Domínio e aplicação não dependem de hardware, e tudo o que dá para testar no computador foi escrito com testes antes do código.

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
| `Ports.h` | As **portas**: o que a aplicação precisa do mundo (relógio, sensor de gás, sensor de clima, botão, saídas liga/desliga e display). O hardware real as implementa nos adaptadores, e os testes usam versões falsas |
| `HouseController` | Roda a casa: lê os sensores no ritmo de cada um, entrega as leituras às regras do domínio, aciona as saídas só quando o valor muda, guarda o histórico, conta os vazamentos e recebe os comandos do app |
| `HouseSnapshot` | Tudo o que o mundo de fora pode querer saber da casa, em um instante |
| `DisplayText` | A regra do que escrever em cada linha do display, em português e sem acentos, que o LCD não tem |
| `DisplayPresenter` | Mantém um display em dia: olha a casa no seu ritmo e só escreve quando o texto mudou, para um barramento lento nunca atrasar o controle |
| `HouseSettings.h` | O ritmo de cada leitura e o tamanho do histórico |

O `HouseController` ocupa uns 7 KB, quase todos do histórico. Por isso deve ser uma variável global, e não ficar na pilha.

O contrato de como o app enxerga esses dados está em [`docs/api.md`](../docs/api.md).

O arquivo `Names.h` dá os nomes dos estados que o contrato usa (`starting`, `normal`, `emergency`, `automatic`, `manual`).

## Conversões

Em [`lib/conversions/src`](lib/conversions/src): as contas dos adaptadores que não dependem de hardware, separadas para serem testadas no computador.

| Arquivo | O que faz |
|---|---|
| `Mq2Scale.h` | Desfaz o divisor de tensão do MQ-2 e leva a leitura para a escala de 0 a 1023 que as regras do gás usam |
| `ClimateCheck.h` | Decide se uma leitura do DHT11 é confiável: dentro da faixa do sensor, sem NaN e sem o par exato 0 °C e 0 %, que é a assinatura de um fio solto |

## Adaptadores

Em [`lib/adapters/src`](lib/adapters/src): uma classe por peça, cada uma implementando uma porta. Só compilam para o ESP32.

| Classe | Peça |
|---|---|
| `Esp32Clock` | O relógio (`millis()`) |
| `Mq2GasSensor` | O sensor de gás, lido pelo ADC |
| `Dht11ClimateSensor` | O sensor de temperatura e umidade |
| `ModuleButton` | O módulo do botão, com o nível que significa "apertado" |
| `GpioOutput` | O LED, o buzzer e o relé, com o nível que significa "ligado" |
| `ServoValve` | O servo que faz o papel da válvula |
| `I2cLcdDisplay` | O LCD 16x2 com módulo I2C. A biblioteca acha o endereço sozinha, e sem LCD o resto continua funcionando |

Os pinos, os níveis e os ângulos da maquete estão todos em [`src/BoardConfig.h`](src/BoardConfig.h). O [`src/main.cpp`](src/main.cpp) monta tudo: é o único lugar que sabe qual adaptador está atrás de cada porta. Ele também imprime uma linha de resumo por segundo no Serial.

## Rodar os testes

Não precisam da placa. Precisa do [PlatformIO](https://platformio.org) e de um compilador C++ no computador.

```bash
cd firmware
pio test -e native
```

Cada pasta em `test/` é um grupo de testes e roda separado. Para rodar só um:

```bash
pio test -e native -f test_gas_guard
```

## Gravar na placa

```bash
pio run -e esp32dev -t upload
pio device monitor
```

O que conferir depois de gravar está no [roteiro de teste da maquete](../docs/roteiro-maquete.md).

## O que vem depois

Na parte 3c, a rede própria do ESP32 e o servidor da API.
