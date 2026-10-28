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
- **O alarme de vazamento tem um som só dele**: três bipes agudos e uma pausa, repetidos, e uma vibração de três pulsos fortes. Usava o toque de alarme padrão do celular, que é o do despertador, e o usuário notou que se confundia com ele. Como o Android não deixa mudar o som de um canal que já existe, o canal ganhou outro id (`gas_alarm`), e o antigo é removido ao iniciar. O som é gerado por um script no repositório (`android/tools/make_alarm_sound.py`), sem arquivo de terceiros, e não passa pelo modo "Não perturbe" por conta própria: isso pediria uma permissão especial do usuário, que o app não pede.
- A notificação permanente tem o botão **"Parar de monitorar"**, que fecha o app de vez: para o serviço, devolve o Wi-Fi do celular e tira o app da lista de recentes. É uma escolha do usuário, e por isso o Android não reinicia o serviço; **um aviso de vazamento que está tocando fica como está**. Parar de monitorar deixa a casa sem vigia no celular até o app ser aberto de novo, e um toque sem querer no botão faz isso: o botão tem o nome claro e fica ao lado do estado, e uma confirmação ficou de fora, porque o botão de uma notificação não abre uma pergunta sem abrir o app.
- Se o usuário encerrar o app à força, não há alerta no celular. O alarme sonoro e a válvula continuam no ESP32, porque a segurança não depende do app.
- A conexão precisa se recuperar sozinha de quedas, com espera crescente entre tentativas.
