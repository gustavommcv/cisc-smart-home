# 0002. REST para comandos e SSE para eventos, sem MQTT

**Estado:** aceita

## Contexto

O app precisa consultar o estado, mandar comandos (ligar o desumidificador, trocar o modo) e receber alertas e leituras assim que acontecem. Não há servidor no meio: o app fala direto com o ESP32.

## Decisão

- **REST com JSON**, versionado em `/api/v1`, para consultas e comandos.
- **SSE** (Server-Sent Events), em um fluxo único, para o ESP32 enviar leituras e alarmes.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| MQTT | Exige um *broker*. O [PicoMQTT](https://reference.arduino.cc/reference/en/libraries/picomqtt), que roda dentro do ESP32, só tem QoS 0 e não guarda mensagens retidas, justamente o que o estado de um dispositivo precisaria |
| WebSocket | Resolve o mesmo, mas é bidirecional e mais elaborado do que precisamos. O sentido ESP32 → app é o que importa, e os comandos já têm o REST |
| Bluetooth Low Energy (GATT) | Ver [0001](0001-esp32-como-ponto-de-acesso.md) |

## Consequências

- Os dois lados usam HTTP, que se testa com `curl` e com um servidor falso no computador.
- O SSE é texto e reconecta com facilidade, o que simplifica a tela "Sem conexão".
- A API pode ser lida e depurada sem ferramentas especiais, o que ajuda na hora de apresentar.
- Ao reconectar, o app precisa pedir o estado completo antes de ouvir os eventos. O contrato dirá isso explicitamente.
- O contrato (rotas, formatos, erros) será escrito em `docs/api.md` **antes** da implementação.
