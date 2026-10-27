# 0006. Alertas por notificação local, de um serviço em primeiro plano

**Estado:** aceita

## Contexto

O morador deve receber um alerta no Android quando houver vazamento de gás. A rede do ESP32 não tem internet (ver [0001](0001-esp32-como-ponto-de-acesso.md)), e o Android limita o que um app faz em segundo plano.

## Decisão

Um **serviço em primeiro plano** (tipo `connectedDevice`) mantém aberto o fluxo SSE com o ESP32 e dispara uma **notificação local** quando chega o alarme. Não usa Firebase nem servidor externo.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Firebase Cloud Messaging | É o caminho recomendado pelo Android para [apps que precisam de conexão persistente](https://developer.android.com/training/monitoring-device-state/doze-standby), mas exige internet, e a rede do ESP32 não tem |
| ntfy ou Telegram | Também dependem de internet |
| Consulta periódica (WorkManager) | O intervalo mínimo é de 15 minutos, tempo demais para um vazamento |
| Tipo `dataSync` para o serviço | No Android 15, tem [limite de 6 horas por dia](https://developer.android.com/develop/background-work/services/fgs/timeout). O tipo `connectedDevice` cobre conexões de rede |

## Consequências

- Os alertas funcionam sem internet, que é o cenário da apresentação.
- Um serviço em primeiro plano exibe uma **notificação permanente**, e o Android 13 ou superior pede a permissão de notificações ao usuário. As duas coisas fazem parte da interface.
- Se o usuário encerrar o app à força, não há alerta no celular. O alarme sonoro e a válvula continuam no ESP32, porque a segurança não depende do app.
- A conexão precisa se recuperar sozinha de quedas, com espera crescente entre tentativas.
