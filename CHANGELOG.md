# Histórico de versões

Cada versão tem uma tag `vX.Y.Z` no Git e uma [página de versões](https://github.com/gustavommcv/cisc-smart-home/releases) com o APK do aplicativo. O texto de cada versão abaixo é o que vai para essa página. Como lançar uma versão: [docs/lancamento.md](docs/lancamento.md).

## Não lançado

### Aplicativo (com o firmware novo)

- O app **acredita no que a placa diz** de cada sensor (a classe e as linhas de limite do gráfico): o limite do alarme do gás vem da placa, e um dispositivo novo com sensores de gás, temperatura ou umidade é desenhado como os outros desses tipos, sem o app saber dele. Com um firmware antigo, que não diz nada, tudo continua como antes.

### Firmware

- O **padrão do desumidificador** passa a ser o ideal para um armário: **liga a 65 % e desliga a 55 %** (era 85 % e 83 %). Uma placa que já guardou os limites continua com os dela; no app, o botão de voltar ao padrão a leva a 65/55, e dá para mudar a qualquer hora pela configuração do dispositivo.
- O JSON da casa passa a dizer, nos sensores, a **classe** (`gas`, `temperature`, `humidity`) e as **linhas de limite** do gráfico (o limite do alarme do gás, que sai da configuração da placa, e os limites do desumidificador, que são as entidades que o usuário muda). É um acréscimo: o app antigo ignora e a placa antiga continua funcionando com o app novo. **Quem quiser isso precisa gravar o firmware de novo.**

### Mudou

- **Cada gráfico do histórico se desenha pelo que o sensor é.** O gás tem a escala fixa de 0 a 400, a linha do limite de alarme e, em vermelho, o que passa dele; no mês e no ano, uma barra por dia com o maior valor. A umidade mostra a faixa em que o desumidificador liga e desliga (os limites que você escolheu). A temperatura nunca fica numa escala menor que 10 °C. Os sensores de dispositivos novos continuam funcionando, como uma linha.

## v0.1.1 — 2026-10-28

Uma correção importante do histórico. Quem usa a v0.1.0 deve atualizar.

### Corrigido

- **O histórico do celular parava de avançar numa conexão estável.** O app só buscava o histórico da placa ao conectar: com a conexão ligada, a tela mostrava a casa em dia, mas o banco e os gráficos não recebiam amostras novas, e depois de cerca de uma hora a placa sobrescrevia as que o app não tinha buscado. Agora o app busca o que é novo de 15 em 15 segundos enquanto está conectado, sem atrasar os alertas, e a limpeza das leituras velhas também roda durante a conexão (antes só ao reconectar).
- Se a placa reinicia sem a conexão cair, o app segue a partida nova na rodada seguinte. Uma falha ao buscar o histórico, ao gravar ou ao limpar só faz a rodada seguinte tentar de novo, sem derrubar o monitoramento.

### Como instalar ou atualizar

- **Aplicativo:** baixe `cisc-casa-0.1.1.apk` (em *Assets*, no fim desta página). Passo a passo e verificação do arquivo: [Como baixar e rodar](https://github.com/gustavommcv/cisc-smart-home/blob/v0.1.1/docs/como-rodar.md#2-instalar-o-aplicativo-no-celular).
- **Atualizando de outra versão:** o APK de cada versão é assinado com uma chave de depuração criada na hora da compilação, então o Android pode recusar instalar por cima de outro APK (erro de assinatura). Se acontecer, **desinstale o app antes**: o histórico guardado no celular é apagado junto, porque ele só existe no celular.
- **Firmware:** continua não vindo pronto, porque a senha do Wi-Fi da maquete é sua. O firmware **não mudou** nesta versão, então quem já gravou a placa não precisa gravar de novo.

## v0.1.0 — 2026-10-28

A primeira versão: o firmware do ESP32 e o aplicativo Android funcionando juntos, com a maquete de gás e umidade.

### O que tem

**Firmware (ESP32)**
- Gás: o MQ-2 acima de 160 (escala de 0 a 1023) fecha a válvula, acende o LED e toca o buzzer. A válvula só reabre pelo botão, depois de 2 s de ar limpo.
- Umidade: o desumidificador liga a partir de 85 % e desliga em 83 %, no modo Automático. Os limites mudam pelo aplicativo (de 40 a 95 %) e ficam guardados na placa. No modo Manual, o aplicativo liga e desliga.
- O buzzer da placa pode ser silenciado pelo aplicativo (o silêncio não fica guardado).
- A placa cria a própria rede Wi-Fi `cisc-smart-home` e oferece a API (REST e eventos em tempo real). O alarme de gás não depende da rede.
- LCD 16×2, com o estado do gás, a umidade e os alertas.

**Aplicativo (Android 10 ou mais novo)**
- Entra sozinho na rede da placa, sem mudar o Wi-Fi do celular.
- Mostra a casa em cômodos e dispositivos; comanda o desumidificador; muda os limites de umidade; silencia o buzzer.
- Aviso de vazamento por cômodo: alarme, notificação ou desligado. Funciona com a tela apagada, por um serviço em primeiro plano, e tem um botão para parar de monitorar.
- Histórico de cada dispositivo em gráficos (1 h, 6 h, 24 h, mês e ano), com a temperatura e a umidade lado a lado. O mês e o ano guardam o menor e o maior valor de cada hora, então um pico não some.
- Nomes e ícones dos cômodos e dispositivos à escolha do usuário; tema claro, escuro ou automático.

### Como instalar

- **Aplicativo:** baixe `cisc-casa-0.1.0.apk` (em *Assets*, no fim desta página) e instale no celular. Passo a passo e verificação do arquivo: [Como baixar e rodar](https://github.com/gustavommcv/cisc-smart-home/blob/v0.1.0/docs/como-rodar.md#2-instalar-o-aplicativo-no-celular).
- **Firmware:** não vem pronto, porque a senha do Wi-Fi da maquete é sua. Grave com o PlatformIO, como no [guia](https://github.com/gustavommcv/cisc-smart-home/blob/v0.1.0/docs/como-rodar.md#4-gravar-o-firmware-no-esp32).
- **Montagem:** [esquemático e lista de materiais](https://github.com/gustavommcv/cisc-smart-home/blob/v0.1.0/hardware/README.md).

### Limites desta versão

- O APK é assinado com a chave de depuração do Android e não está na loja: o Android avisa que o app não é verificado.
- O aviso de vazamento no celular (alarme e notificação) foi testado com testes automáticos e eventos simulados. O teste com um vazamento de verdade, com o isqueiro na maquete, **ainda não foi feito**. O aplicativo foi visto em um Moto G85 com Android 16.
- O servo da válvula precisa de fonte própria de 5 V; dividindo a fonte com o resto, ele só estala.
