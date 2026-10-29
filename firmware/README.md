# Firmware

Firmware do ESP32 do CISC Smart Home. Tem o **domínio** (as regras da casa), a **aplicação** (quem coloca as regras para funcionar) e os **adaptadores** (as peças reais). Domínio e aplicação não dependem de hardware, e tudo o que dá para testar no computador foi escrito com testes antes do código.

## Domínio

Em [`lib/domain/src`](lib/domain/src):

| Arquivo | O que faz |
|---|---|
| `GasGuard` | Máquina de estados do gás: decide a válvula, o LED e o buzzer. Só reabre com o botão, depois de um tempo sem gás |
| `HumidityController` | Liga e desliga o desumidificador com dois limites. Modo Automático ou Manual. Os limites **podem ser mudados** (`setLimits`) dentro do que `Settings.h` permite: números inteiros de 40 a 95 %, com o de desligar pelo menos 2 pontos abaixo do de ligar |
| `ButtonDebouncer` | Transforma o sinal do botão, que tremula, em um único evento por aperto |
| `RingBuffer` e `Sample` | O histórico: um buffer de tamanho fixo que sobrescreve o dado mais antigo |
| `Settings.h` | Todos os limites e tempos, em um só lugar, para calibrar com o sensor real. Os valores de fábrica dos limites de umidade são **65 % (liga) e 55 % (desliga)**, o ideal para um armário, e a regra do que se pode escolher (`isValid`) mora aqui também |
| `Time.h` | Comparação de tempo que continua certa quando o `millis()` dá a volta |

O código é C++ puro, sem `Arduino.h`, sem exceções e sem alocação dinâmica. Compila também com o compilador do ESP32.

## Aplicação

Em [`lib/application/src`](lib/application/src):

| Arquivo | O que faz |
|---|---|
| `Ports.h` | As **portas**: o que a aplicação precisa do mundo (relógio, sensor de gás, sensor de clima, botão, saídas liga/desliga, display e o **`SettingsStore`**, onde ficam guardados os limites de umidade que o usuário mudou). O hardware real as implementa nos adaptadores, e os testes usam versões falsas |
| `HouseController` | Roda a casa: lê os sensores no ritmo de cada um, entrega as leituras às regras do domínio, aciona as saídas só quando o valor muda, guarda o histórico, conta os vazamentos e recebe os comandos do app, entre eles **silenciar o buzzer** (só o buzzer, na hora, sem guardar: ao reiniciar ele volta ligado) e **mudar os limites de umidade**: o que mudou é guardado e, ao ligar, o que estava guardado vale no lugar dos valores de fábrica (se ainda for permitido) |
| `HouseSnapshot` | Tudo o que o mundo de fora pode querer saber da casa, em um instante |
| `DisplayText` | A regra do que escrever em cada linha do display, em português e sem acentos, que o LCD não tem |
| `DisplayPresenter` | Mantém um display em dia: olha a casa no seu ritmo e só escreve quando o texto mudou, para um barramento lento nunca atrasar o controle |
| `HouseSettings.h` | O ritmo de cada leitura e o tamanho do histórico |

O `HouseController` ocupa uns 7 KB, quase todos do histórico. Por isso deve ser uma variável global, e não ficar na pilha.

O contrato de como o app enxerga esses dados está em [`docs/api.md`](../docs/api.md).

O arquivo `Names.h` dá os nomes dos estados que o contrato usa (`starting`, `normal`, `emergency`, `automatic`, `manual`).

## API

Em [`lib/api/src`](lib/api/src): tudo o que o contrato de [`docs/api.md`](../docs/api.md) pede, **menos a rede**. Cada peça recebe e devolve texto e não sabe nada de sockets, por isso é testada no computador.

| Arquivo | O que faz |
|---|---|
| `ApiJson` | O JSON do contrato: o estado da casa (cômodos, dispositivos e entidades), uma entidade, o evento de alerta e o histórico. Os sensores dizem a **classe** (`gas`, `temperature`, `humidity`) e as **linhas de limite** do gráfico, e o limite do alarme do gás vem da configuração da placa (`HouseSnapshot::gasAlarmAt`), não de uma cópia no app |
| `Api` | O roteador REST: recebe método, caminho, parâmetros e corpo, age na casa e devolve o status e o JSON, com os códigos de erro do contrato |
| `EventStream` | O que o fluxo SSE envia a cada instante: o estado quando muda (no máximo uma vez por segundo), o alerta de vazamento na hora e um sinal de vida a cada 15 s |

Os eventos são entregues como pares nome e dados: quem formata o `event:` e o `data:` é o servidor web.

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
| `GpioOutput` | O LED e o buzzer, com o nível que significa "ligado" |
| `OpenDrainOutput` | O relé. "Ligado" puxa o pino para o GND e "desligado" **solta** o pino. O módulo não desliga de forma confiável com o pino em 3,3 V |
| `ServoValve` | O servo que faz o papel da válvula |
| `I2cLcdDisplay` | O LCD 16x2 com módulo I2C. A biblioteca acha o endereço sozinha, e sem LCD o resto continua funcionando |
| `NvsSettingsStore` | Guarda os limites de umidade na memória não volátil da placa (NVS), que sobrevive a uma queda de energia. Cada limite é um byte |
| `AccessPoint` | O Wi-Fi da maquete. A placa cria a própria rede, em um endereço fixo (`192.168.4.1`) |
| `ApiServer` | Leva a API por HTTP com a ESPAsyncWebServer: lê a requisição, pergunta à `Api` o que responder e escreve a resposta, e entrega os eventos do `EventStream` aos clientes. Não tem regra própria |

Os pinos, os níveis e os ângulos da maquete estão todos em [`src/BoardConfig.h`](src/BoardConfig.h). O [`src/main.cpp`](src/main.cpp) monta tudo: é o único lugar que sabe qual adaptador está atrás de cada porta. Ele também imprime uma linha de resumo por segundo no Serial.

### A senha do Wi-Fi

O nome da rede (`cisc-smart-home`) é público e fica em [`src/WifiConfig.h`](src/WifiConfig.h). A **senha** fica em `src/WifiSecrets.h`, que o Git ignora. Para criar o seu:

```bash
cp src/WifiSecrets.example.h src/WifiSecrets.h
```

e troque a senha dentro (de 8 a 63 caracteres; o código não compila com menos). Sem esse arquivo, o firmware compila com a senha do exemplo e um aviso. É o que acontece no CI.

### Duas tarefas ao mesmo tempo

O servidor web roda em uma tarefa própria, enquanto o laço principal controla a casa. Um `std::mutex` impede os dois de mexerem na casa ao mesmo tempo: o laço o segura durante o `tick()` e o servidor, durante cada resposta. O laço também espera 1 ms por volta, para dar a vez ao servidor. O **alarme de gás não depende da rede**: se o Wi-Fi não subir, a casa continua funcionando.

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

Na fase 4, o aplicativo Android que conversa com essa API.
