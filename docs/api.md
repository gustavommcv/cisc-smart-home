# API entre o app e o ESP32

Este é o **contrato** entre o firmware e o aplicativo. Ele foi escrito antes do código que o implementa, e os dois lados o seguem. Se o contrato e o código divergirem, o erro está no código.

As escolhas por trás dele estão nas decisões [0001](decisoes/0001-esp32-como-ponto-de-acesso.md), [0002](decisoes/0002-rest-e-sse-em-vez-de-mqtt.md) e [0004](decisoes/0004-modelo-comodo-dispositivo-entidade.md).

## Princípios

- **HTTP/1.1 sem criptografia**, JSON em UTF-8. O ESP32 fica sempre em `http://192.168.4.1`.
- **Versionada** no caminho: `/api/v1`. Acrescentar campos ou entidades não muda a versão. Mudar ou remover algo existente exige `/api/v2`.
- **O app é tolerante:** ignora campos que não conhece e entidades de um `kind` que não conhece. Um `value` que não combina com o `kind` conta como leitura inválida (`null`). O firmware é rigoroso com o que recebe.
- **O modelo é genérico:** o app desenha a tela a partir de cômodos, dispositivos e entidades, sem conhecer o gás nem a umidade. Veja o [modelo de dados](arquitetura.md#modelo-de-dados).
- **Sem autenticação.** A proteção é a senha da rede Wi-Fi do ESP32 (WPA2). É uma limitação aceita para um projeto de demonstração.
- **Sem estado de hora.** O ESP32 só conhece o tempo desde que ligou (`uptimeMs`). Quem converte para a hora real é o app (veja [Tempo](#tempo)).

## Visão geral

| Rota | Para quê |
|---|---|
| `GET /api/v1/home` | O estado completo da casa |
| `PUT /api/v1/entities/{id}/value` | Enviar um comando a uma entidade |
| `GET /api/v1/history` | As leituras guardadas |
| `GET /api/v1/events` | Fluxo SSE com mudanças e alertas em tempo real |

## O estado da casa: `GET /api/v1/home`

```json
{
  "apiVersion": 1,
  "bootId": "5f3a9c21",
  "uptimeMs": 183450,
  "areas": [
    {
      "id": "kitchen",
      "name": "Kitchen",
      "devices": [
        {
          "id": "gas-sensor",
          "name": "Gas sensor",
          "entities": [
            { "id": "gas-level", "kind": "sensor", "name": "Gas level", "value": 142, "unit": "adc" },
            { "id": "gas-state", "kind": "sensor", "name": "Gas state", "value": "normal" },
            { "id": "gas-alerts", "kind": "sensor", "name": "Gas alerts since boot", "value": 0 }
          ]
        },
        {
          "id": "gas-valve",
          "name": "Gas valve",
          "entities": [
            { "id": "valve-open", "kind": "binary_sensor", "name": "Valve open", "value": true }
          ]
        }
      ]
    },
    {
      "id": "bedroom",
      "name": "Bedroom",
      "devices": [
        {
          "id": "closet-sensor",
          "name": "Closet sensor",
          "entities": [
            { "id": "temperature", "kind": "sensor", "name": "Temperature", "value": 21.4, "unit": "°C" },
            { "id": "humidity", "kind": "sensor", "name": "Humidity", "value": 74.1, "unit": "%" }
          ]
        },
        {
          "id": "dehumidifier",
          "name": "Dehumidifier",
          "entities": [
            { "id": "dehumidifier-power", "kind": "switch", "name": "Power", "value": false, "commandable": true },
            { "id": "dehumidifier-mode", "kind": "select", "name": "Mode", "value": "automatic",
              "options": ["automatic", "manual"], "commandable": true }
          ]
        }
      ]
    }
  ]
}
```

### Campos

| Campo | Significado |
|---|---|
| `apiVersion` | Versão do contrato, hoje `1` |
| `bootId` | Identificador da partida. **Muda a cada reinício** do ESP32 |
| `uptimeMs` | Milissegundos desde que o ESP32 ligou |
| `areas[].id`, `devices[].id`, `entities[].id` | Texto em `kebab-case`, **único** em toda a casa e estável. É por ele que o app reconhece cada coisa e traduz o nome |
| `name` | Rótulo padrão, em inglês. O app pode mostrar a própria tradução, escolhida pelo `id` |
| `kind` | O tipo da entidade, na tabela abaixo |
| `value` | O valor atual. `null` quando não há leitura válida (por exemplo, o DHT11 não respondeu) |
| `unit` | Unidade, quando faz sentido. `adc` quer dizer a escala de 0 a 1023 do sensor, **não ppm** |
| `options` | As opções de um `select` |
| `commandable` | `true` se aceita comandos. Quando ausente, vale `false` |

### Tipos de entidade

| `kind` | `value` | Aceita comando |
|---|---|---|
| `sensor` | número ou texto | não |
| `binary_sensor` | `true` ou `false` | não |
| `switch` | `true` ou `false` | sim |
| `select` | uma das `options` | sim |

### As entidades da maquete

| Cômodo | Dispositivo | Entidade | Tipo | Valores |
|---|---|---|---|---|
| `kitchen` | `gas-sensor` | `gas-level` | `sensor` | 0 a 1023, unidade `adc` |
| | | `gas-state` | `sensor` | `starting`, `normal` ou `emergency` |
| | | `gas-alerts` | `sensor` | quantos vazamentos desde que o ESP32 ligou |
| | `gas-valve` | `valve-open` | `binary_sensor` | `true` quando aberta. **Somente leitura**: só o botão físico a reabre |
| `bedroom` | `closet-sensor` | `temperature` | `sensor` | graus Celsius, ou `null` |
| | | `humidity` | `sensor` | porcentagem, ou `null` |
| | `dehumidifier` | `dehumidifier-power` | `switch` | ligado ou desligado |
| | | `dehumidifier-mode` | `select` | `automatic` ou `manual` |

Os valores decimais têm uma casa.

## Comandos: `PUT /api/v1/entities/{id}/value`

```http
PUT /api/v1/entities/dehumidifier-power/value HTTP/1.1
Content-Type: application/json

{ "value": true }
```

A resposta `200` traz a entidade **como ficou**, no mesmo formato de `/home`:

```json
{ "id": "dehumidifier-power", "kind": "switch", "name": "Power", "value": true, "commandable": true }
```

### Regras do desumidificador

| Comando | Efeito |
|---|---|
| `dehumidifier-power` = `true` ou `false` | Liga ou desliga **e passa para o modo manual** |
| `dehumidifier-mode` = `"automatic"` | Devolve o controle à regra de umidade. O estado atual vale até a próxima leitura |
| `dehumidifier-mode` = `"manual"` | Passa para manual **mantendo** o estado atual |

Depois de um comando, o novo estado também chega pelo fluxo de eventos.

## Histórico: `GET /api/v1/history`

| Parâmetro | Significado |
|---|---|
| `after` | Opcional. Só as amostras com `uptimeMs` **maior** que este valor. Serve para pedir apenas o que é novo |
| `limit` | Opcional. No máximo esta quantidade, das mais antigas para as mais novas dentro do filtro. Um número maior do que o ESP32 guarda (360) devolve tudo o que há |

Sem `limit`, a resposta traz **no máximo 100 amostras**, para caber na memória do ESP32. Para buscar o resto, o app repete o pedido com `after` igual ao `uptimeMs` da última amostra recebida. Um `after` ou um `limit` que não seja um número inteiro (e um `limit` menor que 1) dá o erro `invalid-query`. Outros parâmetros são ignorados.

```json
{
  "bootId": "5f3a9c21",
  "samples": [
    { "uptimeMs": 160000, "values": { "gas-level": 142, "temperature": 21.4, "humidity": 74.1 } },
    { "uptimeMs": 170000, "values": { "gas-level": 140, "temperature": null, "humidity": null } }
  ]
}
```

- As amostras vêm **da mais antiga para a mais nova**.
- A chave de cada valor é o `id` da entidade. `null` quer dizer que não houve leitura válida.
- O ESP32 guarda um número fixo de amostras na memória. Ao reiniciar, **perde tudo**. O app guarda a sua própria cópia.
- Para saber se já tem uma amostra, o app usa o par (`bootId`, `uptimeMs`).

## Eventos em tempo real: `GET /api/v1/events`

Uma conexão **SSE** (`Content-Type: text/event-stream`) que fica aberta. A requisição precisa do cabeçalho `Accept: text/event-stream`, que qualquer cliente SSE já envia; sem ele a resposta é `406`. O ESP32 envia:

| Evento | Quando | Conteúdo |
|---|---|---|
| `home` | Logo ao conectar, e depois quando algo mudou (no máximo uma vez por segundo) | O mesmo JSON de `GET /api/v1/home` |
| `alert` | Quando começa um vazamento de gás | Veja abaixo |
| `ping` | Depois de 15 segundos sem enviar nada | Vazio. Serve só para o app saber que a conexão está viva, e ele a ignora |

```text
event: home
data: {"apiVersion":1,"bootId":"5f3a9c21", ... }

event: alert
data: {"type":"gas-leak","areaId":"kitchen","entityId":"gas-state","count":1,"uptimeMs":183450}
```

- `count` é o valor de `gas-alerts` naquele momento. O app o usa, junto com o `bootId`, para **não avisar duas vezes** do mesmo vazamento, mesmo que reconecte.
- Se o app estava desconectado quando o vazamento começou, ele vê `gas-state` em `emergency` e `gas-alerts` maior que o último que conhecia, e avisa ao reconectar.
- A cada 15 segundos sem novidade, o ESP32 manda um evento `ping`, sem dados. **Se o app ficar 20 segundos sem receber nada (nem `ping`), deve reconectar.**
- O evento `home` só sai quando algo mudou (o `uptimeMs` sozinho não conta) e, no máximo, **uma vez por segundo**. O `alert` não espera esse intervalo: sai na hora, **antes** do `home` do mesmo instante.
- Quem conecta depois de um vazamento não recebe o `alert` antigo, só o estado atual. Para saber se perdeu algum, compare o `count` de `gas-alerts` com o último que conhecia.
- O ESP32 aceita poucas conexões de eventos ao mesmo tempo. Com o limite atingido, responde `503`.

### Como o app se conecta

1. `GET /api/v1/home`, para ter o estado completo.
2. Abre `GET /api/v1/events`.
3. `GET /api/v1/history?after=...` para buscar só o que falta.
4. Se o `bootId` mudou desde a última vez, o ESP32 reiniciou: o app descarta o que sabia sobre o tempo e começa o histórico dessa partida do zero.

## Tempo

O ESP32 não tem hora certa sem internet. Toda resposta traz `uptimeMs`, e o app calcula a diferença para o relógio do celular:

```text
diferença        = (hora do celular ao receber a resposta) − (uptimeMs da resposta)
hora de amostra  = (uptimeMs da amostra) + diferença
```

Como o `bootId` identifica a partida, o app guarda uma diferença por partida. O erro é de alguns décimos de segundo (o tempo da resposta), o que sobra para um gráfico com leituras a cada 10 segundos.

Limite conhecido: `uptimeMs` é um número de 32 bits em milissegundos e volta a zero depois de uns 49 dias ligado. Se o app vir o `uptimeMs` diminuir com o mesmo `bootId`, deve tratar como um reinício. Para uma demonstração isso não importa.

## Erros

Qualquer resposta de erro tem este formato. A entidade é verificada **antes** do corpo: pedir um comando a uma entidade que não existe ou que é somente leitura dá o mesmo erro, qualquer que seja o corpo.

```json
{ "error": { "code": "not-commandable", "message": "Entity 'valve-open' does not accept commands" } }
```

| HTTP | `code` | Quando |
|---|---|---|
| 400 | `invalid-body` | O corpo não é um JSON válido ou não tem `value` |
| 400 | `invalid-value` | O `value` não serve para essa entidade (por exemplo, um texto em um `switch`) |
| 400 | `invalid-query` | O `after` ou o `limit` do histórico não é um número válido |
| 403 | `not-commandable` | A entidade não aceita comandos |
| 404 | `not-found` | A entidade ou a rota não existe |
| 406 | `not-acceptable` | A rota de eventos foi pedida sem `Accept: text/event-stream` |
| 405 | `method-not-allowed` | A rota existe, mas não aceita esse método |
| 503 | `too-many-clients` | Conexões de eventos demais |

## Testar com `curl`

Conectado à rede do ESP32:

```bash
curl http://192.168.4.1/api/v1/home
```

```bash
curl -X PUT -H 'Content-Type: application/json' -d '{"value": true}' \
  http://192.168.4.1/api/v1/entities/dehumidifier-power/value
```

```bash
curl -N -H 'Accept: text/event-stream' http://192.168.4.1/api/v1/events
```
