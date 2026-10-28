# 0010. O app entra sozinho na rede da placa, por um link que se pode trocar

**Estado:** aceita

## Contexto

Até aqui, quem usava o app tinha de entrar no Wi-Fi `cisc-smart-home` pelas configurações do celular e **desligar os dados móveis**: o Android prefere o 4G a um Wi-Fi sem internet, e o tráfego para `192.168.4.1` saía pelo caminho errado. Na apresentação isso é frágil, e é um passo a mais para quem só quer abrir o app.

Há também um pedido de projeto: mostrar o app ligado ao Wi-Fi da placa, **e que ele se configure com outro tipo de conexão** (a nuvem, uma VPN) sem ser refeito. A conexão tem de ser a parte fácil de trocar.

## Decisão

- **Uma peça só sabe como se chega à placa: o `BoardLink`** (`data/link`). Ele diz uma **rota** (`BoardRoute`): *entrando* (`Joining`), *aberta* (`Open`, com o endereço e um cliente HTTP que já anda pelo caminho certo) ou *fechada* (`Closed`, com o motivo). O `BoardClient` e o `BoardEventStream` continuam falando só o contrato de [`docs/api.md`](../api.md), para o endereço e com o cliente que recebem. O `RoutedBoardApi` e o `RoutedBoardEvents` os usam pela rota do momento.
- **Dois links hoje.** O `WifiAccessPointLink` pede ao Android o Wi-Fi da placa pelo nome e pela senha (`WifiNetworkSpecifier`) e **prende os soquetes do app a essa rede** (`network.socketFactory`), sem prender o processo todo. O `DirectLink` não faz nada: usa a rede em que o celular já está, a da placa colocada à mão, uma VPN ou outra que a alcance.
- **O usuário escolhe em Ajustes › Conexão** (`LinkKind`), e o `ChosenLink` segue a escolha na hora, sem reiniciar o app. Embaixo da escolha que vale, o cartão diz **se ela funciona** ("Conectado à placa" ou o motivo). A senha do Wi-Fi da placa é digitada uma vez e guardada no celular, e **só se pede de novo quando é preciso**: quando não há senha ou quando o celular não entra na rede. Com a placa ouvida, o cartão oferece "Esquecer a senha", que a joga fora, solta a rede e traz o campo de volta. "Nuvem" e "VPN ou endereço próprio" aparecem como cartões de "Em breve", para mostrar onde as próximas entram.
- **Um caminho novo (nuvem, VPN) é um valor novo em `LinkKind`, um link novo em `data/link` e uma linha no `when` do `AppContainer`.** Nenhuma tela, o serviço ou o repositório muda.
- **Quando a rede cai, o link a pede de novo**, com esperas de 5 s que crescem até 30 s. Cada tentativa tem tempo limite de 40 s.
- **O erro vem em duas palavras**: falta a senha (`NetworkNotSet`) e não deu para entrar (`NetworkNotJoined`). O banner e a notificação dizem o que fazer em cada uma.

## O que se mediu no celular

Moto G85, Android 16, com a placa de verdade:

- **Na primeira vez o Android pergunta** se o app pode entrar na rede. Ele **lembra a resposta** para aquele app e aquela rede: a segunda entrada, ao reabrir o app, não perguntou. Se o usuário "esquecer" a rede nas configurações do Wi-Fi, a pergunta volta.
- **O Android não reconecta sozinho depois de uma queda.** Com a placa 20 s sem energia, o pedido ficou parado (sem rede, sem erro) mesmo com a placa de volta. É por isso que o link solta o pedido e o refaz. Com isso, o app voltou cerca de **1 minuto** depois da queda (20 s de placa desligada, a partida dela e a entrada do celular).
- **A placa às vezes recusa o primeiro aperto de mão** logo depois de o celular ter saído dela: o log do Android diz "senha pode estar incorreta", e ele bloqueia a rede por 10 s antes de tentar de novo. A senha estava certa, e a tentativa seguinte conectou. A entrada levou de 4 s a cerca de 30 s; uma tentativa que trava é refeita aos 40 s.
- **A única permissão necessária é `CHANGE_NETWORK_STATE`**, que o app já tinha. Nem a localização nem "dispositivos próximos" foram pedidas pelo Android 16. **Não foi testado no Android 10 a 15**, onde isso pode mudar.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| `WifiNetworkSuggestion` (o celular entra sozinho, como numa rede salva) | O Android trata a rede como qualquer outra sem internet e não prende o tráfego: o 4G volta a ser o preferido, o problema que se queria resolver |
| `bindProcessToNetwork` (prender o processo todo) | Prende também o que não é da placa, e o que viria da nuvem. Prender os soquetes do `BoardClient` e do `BoardEventStream` basta |
| Só instruir o usuário, como antes | Frágil na apresentação. Fica como **plano B**: a opção "Rede do celular" |
| Escrever a conexão direto no cliente, sem o `BoardLink` | Trocar por nuvem ou VPN exigiria refazer o cliente e quem o usa |

## Nuvem e VPN não são do mesmo tipo

- **A VPN estende a rede**: o celular passa a alcançar o endereço da placa como se estivesse na mesma rede, e quem entrega os pacotes é o aplicativo da VPN (WireGuard, Tailscale), não o nosso. O app fala o **mesmo contrato** (REST e SSE) com o mesmo cliente; só o endereço pode mudar. Dentro do app, então, não é um tipo novo de conexão: é o `DirectLink` com um endereço que o usuário digita. Por isso o cartão fechado se chama **"VPN ou endereço próprio"**, e a escolha "Rede do celular" já serve a quem entra na VPN pelo próprio celular.
- **A nuvem põe um servidor no meio**: a placa publica o estado (por MQTT ou HTTPS, por exemplo) e o app o lê de lá, com conta, autenticação e notificação push. Muda o **protocolo**, ou seja, outra implementação de `BoardApi` e `BoardEvents`, e a decisão [0002](0002-rest-e-sse-em-vez-de-mqtt.md) teria de ser revista. É um tipo à parte, e o único dos dois que pede um link novo.
- **As duas pedem a placa numa rede com roteador** (ela entrando num Wi-Fi que já existe), o contrário do ponto de acesso da decisão [0001](0001-esp32-como-ponto-de-acesso.md). Isso é mudança de firmware e fica fora desta decisão: enquanto o ESP32 for o ponto de acesso, nem a VPN nem a nuvem o alcançam.

## Consequências

- **Enquanto o app está ligado à placa, o Wi-Fi de casa do celular fica de lado**: o rádio é um só. Ao escolher "Rede do celular" ou fechar o app, o Android o devolve.
- **A senha fica num arquivo de preferências do app** (`link-settings`, chave `board-password`), que só ele lê, sem cifra, e que o backup do Android não leva (`allowBackup="false"`). É a senha de uma rede de demonstração, sem internet. Se um dia o link guardar algo mais sensível (um token da nuvem), isso muda: aí vale o Keystore do Android.
- **Na apresentação:** aceite o aviso do Android uma vez antes, e abra o app uns minutos antes de começar, para ele já estar ligado. Se a conexão automática falhar, entre no Wi-Fi da placa pelas configurações e escolha "Rede do celular".
- O nome da rede está escrito no app (`AppContainer.BOARD_SSID`) e no firmware (`WifiConfig.h`). Um teste lê o arquivo do firmware e os compara, então mudar um sem o outro quebra um teste.
