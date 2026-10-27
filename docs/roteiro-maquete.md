# Roteiro de teste da maquete

O que conferir na placa depois de gravar o firmware. Os testes do computador garantem as regras. Este roteiro garante que **os fios, os sensores e os atuadores** fazem o que as regras mandam, e é a única parte que o CI não cobre.

## Preparar

Com o ESP32 na USB e a maquete montada:

```bash
cd firmware
pio run -e esp32dev -t upload
pio device monitor
```

O monitor já abre a 115200. Para sair, `Ctrl+C`. Feche o monitor antes de gravar de novo, porque ele segura a porta.

A cada segundo, o firmware imprime uma linha assim:

```text
gas=142 state=normal valve=open alerts=0 | climate=21.4C/74.1% | dehumidifier=off mode=automatic
```

| Campo | Significado |
|---|---|
| `gas` | Leitura do MQ-2 na escala de 0 a 1023 |
| `state` | `starting`, `normal` ou `emergency` |
| `valve` | `open` ou `closed`: a posição que o servo recebeu |
| `alerts` | Quantos vazamentos desde que o ESP32 ligou |
| `climate` | Temperatura e umidade, ou `n/a` sem leitura válida |
| `dehumidifier` e `mode` | O relé e o modo Automático ou Manual |

## Passos

Os limites atuais estão em [`Settings.h`](../firmware/lib/domain/src/Settings.h): gás acima de **160**, umidade liga em **85 %** e desliga em **84 %**.

| # | O que fazer | O que deve acontecer | Se não acontecer |
|---|---|---|---|
| 1 | Ligar o ESP32 | `state=starting` e `valve=closed` por cerca de **10 s**, depois `state=normal` e `valve=open`. O servo gira para a posição aberta | Se o servo não se move, confira a alimentação dele e o GND comum com o ESP32 |
| 2 | Esperar em ar limpo | `gas` estável, entre uns 100 e 150 com o sensor aquecido. Depois de 2 s, `climate` mostra temperatura e umidade | **`gas=44` fixo** quer dizer 0 V no pino do sensor: o MQ-2 está sem alimentação ou o fio AO soltou. Confira o VCC, o GND e o divisor de resistores |
| 3 | Aproximar o gás do isqueiro (sem acender) do MQ-2 | `gas` passa de 160, `state=emergency`, `valve=closed`, `alerts` sobe 1. O LED acende, o buzzer toca e o servo fecha | Se `gas` sobe mas nada acontece, o problema não é o sensor: avise |
| 4 | Afastar o gás | O buzzer **para** quando `gas` volta a 160 ou menos. O LED continua aceso e `state` continua `emergency` | |
| 5 | Esperar 2 s com o gás abaixo do limite e **apertar** o botão | `state=normal`, `valve=open`, LED apagado, servo na posição aberta | Se nada acontece, aperte de novo depois de uns 2 s abaixo do limite. O botão só vale com o ar limpo |
| 6 | Soprar no DHT11 até a umidade passar de 85 % | `dehumidifier=on`. O relé estala e o LED verde da maquete acende | |
| 7 | Esperar a umidade cair a 84 % ou menos | `dehumidifier=off`, relé solto | |
| 8 | (Opcional) Soltar o fio DATA do DHT11 | `climate=n/a` e `dehumidifier=off`. Volte o fio e a leitura retorna | |

O botão deste módulo dá nível alto quando apertado, então **o rearme acontece ao apertar**, e não ao soltar.

## Anotar

Se algum passo falhar, anote o número do passo e copie 3 ou 4 linhas do monitor perto do problema. Com isso dá para saber se a causa é o fio, o sensor ou o código.
