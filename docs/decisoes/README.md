# Decisões de arquitetura

Cada decisão importante do projeto está registrada em um arquivo curto, com o contexto, a escolha, as alternativas descartadas e as consequências. Assim, quem ler o projeto depois entende **por que** ele é como é, e não só **como** é.

| # | Decisão | Estado |
|---|---|---|
| [0001](0001-esp32-como-ponto-de-acesso.md) | O ESP32 cria a própria rede Wi-Fi | aceita |
| [0002](0002-rest-e-sse-em-vez-de-mqtt.md) | REST para comandos e SSE para eventos, sem MQTT | aceita |
| [0003](0003-app-android-nativo.md) | App Android nativo em Kotlin com Jetpack Compose | aceita |
| [0004](0004-modelo-comodo-dispositivo-entidade.md) | Modelo de dados: cômodo, dispositivo e entidade | aceita |
| [0005](0005-firmware-em-camadas-testavel.md) | Firmware em camadas, com o domínio testável no computador | aceita |
| [0006](0006-alertas-por-notificacao-local.md) | Alertas por notificação local, de um serviço em primeiro plano | aceita |
| [0007](0007-abas-e-escolha-do-tema.md) | Navegação por abas e tema escolhido pelo usuário | aceita |
| [0008](0008-grafico-desenhado-a-mao.md) | O gráfico do histórico é desenhado à mão, sem biblioteca | aceita |
| [0009](0009-nomes-dados-pelo-usuario.md) | Os nomes de cômodos e dispositivos são do usuário, ficam no celular e o alarme os diz | aceita |

Uma decisão aceita não se apaga: se mudar, ganha um novo arquivo que a substitui, e a antiga fica marcada como substituída.

Para o panorama completo, veja a [arquitetura](../arquitetura.md).
