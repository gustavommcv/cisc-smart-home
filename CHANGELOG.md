# Histórico de versões

Cada versão tem uma tag `vX.Y.Z` no Git e uma [página de versões](https://github.com/gustavommcv/cisc-smart-home/releases) com o APK do aplicativo. O texto de cada versão abaixo é o que vai para essa página. Como lançar uma versão: [docs/lancamento.md](docs/lancamento.md).

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

- **Aplicativo:** baixe `cisc-casa-0.1.0.apk` (em *Assets*, no fim desta página) e instale no celular. Passo a passo e verificação do arquivo: [Como baixar e rodar](https://github.com/gustavommcv/cisc-smart-home/blob/v0.1.0/docs/como-rodar.md#2-instalar-o-aplicativo).
- **Firmware:** não vem pronto, porque a senha do Wi-Fi da maquete é sua. Grave com o PlatformIO, como no [guia](https://github.com/gustavommcv/cisc-smart-home/blob/v0.1.0/docs/como-rodar.md#4-gravar-o-firmware-no-esp32).
- **Montagem:** [esquemático e lista de materiais](https://github.com/gustavommcv/cisc-smart-home/blob/v0.1.0/hardware/README.md).

### Limites desta versão

- O APK é assinado com a chave de depuração do Android e não está na loja: o Android avisa que o app não é verificado.
- O aviso de vazamento no celular (alarme e notificação) foi testado com testes automáticos e eventos simulados. O teste com um vazamento de verdade, com o isqueiro na maquete, **ainda não foi feito**. O aplicativo foi visto em um Moto G85 com Android 16.
- O servo da válvula precisa de fonte própria de 5 V; dividindo a fonte com o resto, ele só estala.
