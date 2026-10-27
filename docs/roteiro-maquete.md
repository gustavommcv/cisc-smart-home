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

Logo depois de ligar, o firmware diz se achou o LCD: `lcd: found` ou `lcd: not found, running without it`. Sem o LCD, tudo o mais continua funcionando.

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

## O que o LCD mostra

O LCD acompanha o mesmo estado do Serial, com mensagens para quem está na casa:

| Situação | Linha de cima | Linha de baixo |
|---|---|---|
| Aquecendo o sensor | `Inicio: aguarde` | `Valvula FECHADA` |
| Vazamento | `ALERTA DE GAS` | `Valvula FECHADA` |
| Gás já foi embora, esperando 2 s | `Alarme retido` | `Aguarde sem gas` |
| Pronto para rearmar | `Alarme retido` | `Aperte o botao` |
| Normal | `Temp: 21.4 C` | `Umid: 74% OFF` (ou `LIG`) |
| Modo manual | `Temp: 21.4 C MAN` | `Umid: 74% LIG` |
| Antes da primeira leitura do DHT11 | `Lendo DHT11...` | `Desumid.: OFF` |
| DHT11 sem resposta | `Erro no DHT11` | `Desumid.: OFF` |

A tela é atualizada no máximo 4 vezes por segundo, e só quando o texto muda. Se o texto estiver fraco ou sumir, gire o potenciômetro azul atrás do módulo I2C.

## Passos

Os limites atuais estão em [`Settings.h`](../firmware/lib/domain/src/Settings.h): gás acima de **160**, umidade liga em **85 %** e desliga em **84 %**.

| # | O que fazer | O que deve acontecer | Se não acontecer |
|---|---|---|---|
| 1 | Ligar o ESP32 | `state=starting` e `valve=closed` por cerca de **10 s**, depois `state=normal` e `valve=open`. O servo gira para a posição aberta. O LCD mostra `Inicio: aguarde` e depois a temperatura | Se o servo não se move, confira a alimentação dele e o GND comum com o ESP32 |
| 2 | Esperar em ar limpo | `gas` baixo e estável, entre **44 e uns 60** com a fiação da maquete. Depois de 2 s, `climate` mostra temperatura e umidade | `gas=44` é o **piso do ADC**: o ESP32 não mede abaixo de uns 140 mV no pino, e em ar limpo a saída do MQ-2 fica por aí. É normal. Só desconfie se a leitura **nunca sobe** com o gás no passo 3: aí confira o VCC, o GND e o divisor de resistores |
| 3 | Aproximar o gás do isqueiro (sem acender) do MQ-2 | `gas` passa de 160, `state=emergency`, `valve=closed`, `alerts` sobe 1. O LED acende, o buzzer toca, o servo fecha e o LCD mostra `ALERTA DE GAS` | Se `gas` sobe mas nada acontece, o problema não é o sensor: avise |
| 4 | Afastar o gás | O buzzer **para** quando `gas` volta a 160 ou menos. O LED continua aceso e `state` continua `emergency`. O LCD mostra `Alarme retido`, e depois de 2 s passa a pedir `Aperte o botao` | |
| 5 | Esperar 2 s com o gás abaixo do limite e **apertar** o botão | `state=normal`, `valve=open`, LED apagado, servo na posição aberta e o LCD de volta à temperatura | Se nada acontece, aperte de novo depois de uns 2 s abaixo do limite. O botão só vale com o ar limpo |
| 6 | Soprar no DHT11 até a umidade passar de 85 % | `dehumidifier=on`. O relé estala, o LED verde da maquete acende e o LCD mostra `LIG` | |
| 7 | Esperar a umidade cair a 84 % ou menos | `dehumidifier=off`, relé solto | |
| 8 | (Opcional) Soltar o fio DATA do DHT11 | `climate=n/a` e `dehumidifier=off`. Volte o fio e a leitura retorna | |

O botão deste módulo dá nível alto quando apertado, então **o rearme acontece ao apertar**, e não ao soltar.

## Anotar

Se algum passo falhar, anote o número do passo e copie 3 ou 4 linhas do monitor perto do problema. Com isso dá para saber se a causa é o fio, o sensor ou o código.
