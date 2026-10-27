# 0001. O ESP32 cria a própria rede Wi-Fi

**Estado:** aceita

## Contexto

O celular precisa falar com o ESP32 durante a apresentação, em um lugar que não controlamos: a rede da sala pode bloquear dispositivos, exigir login ou simplesmente falhar.

## Decisão

O ESP32 funciona como **ponto de acesso** (SoftAP). O celular se conecta à rede do ESP32, que tem sempre o mesmo endereço.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| ESP32 entra no hotspot do celular | Depende de configurar o celular a cada uso, e o endereço do ESP32 muda |
| ESP32 entra no Wi-Fi do local | Depende de uma rede que não controlamos |
| Bluetooth Low Energy | Mais complexo de programar nos dois lados e com pouca vantagem para este volume de dados |

## Consequências

- A demonstração não depende de nenhuma infraestrutura externa.
- A rede do ESP32 não tem internet. O Android costuma abandonar redes assim, então o app precisa **pedir a rede explicitamente e prender o tráfego nela**. Isso é parte do trabalho da fase 6.
- Um ESP32 atende poucos celulares ao mesmo tempo, o que basta para o projeto.
- Serviços na internet (notificações da nuvem, por exemplo) ficam fora do caminho principal. É a razão da [decisão 0006](0006-alertas-por-notificacao-local.md).
