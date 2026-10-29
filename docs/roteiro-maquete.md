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

Os limites atuais estão em [`Settings.h`](../firmware/lib/domain/src/Settings.h): gás acima de **160**, umidade liga em **65 %** e desliga em **55 %** (o ideal para um armário: de 40 a 60 %, e acima de uns 65 % nasce mofo). Os dois limites de umidade podem ser mudados pelo app (o monitor do armário) e **ficam guardados na placa**; se o app mudou, os valores de agora são outros.

| # | O que fazer | O que deve acontecer | Se não acontecer |
|---|---|---|---|
| 1 | Ligar o ESP32 | `state=starting` e `valve=closed` por cerca de **10 s**, depois `state=normal` e `valve=open`. O servo gira para a posição aberta. O LCD mostra `Inicio: aguarde` e depois a temperatura | Se o servo não se move, confira a alimentação dele e o GND comum com o ESP32 |
| 2 | Esperar em ar limpo | `gas` baixo e estável, entre **44 e uns 60** com a fiação da maquete. Depois de 2 s, `climate` mostra temperatura e umidade | `gas=44` é o **piso do ADC**: o ESP32 não mede abaixo de uns 140 mV no pino, e em ar limpo a saída do MQ-2 fica por aí. É normal. Só desconfie se a leitura **nunca sobe** com o gás no passo 3: aí confira o VCC, o GND e o divisor de resistores |
| 3 | Aproximar o gás do isqueiro (sem acender) do MQ-2 | `gas` passa de 160, `state=emergency`, `valve=closed`, `alerts` sobe 1. O LED acende, o buzzer toca, o servo fecha e o LCD mostra `ALERTA DE GAS` | Se `gas` sobe mas nada acontece, o problema não é o sensor: avise |
| 4 | Afastar o gás | O buzzer **para** quando `gas` volta a 160 ou menos. O LED continua aceso e `state` continua `emergency`. O LCD mostra `Alarme retido`, e depois de 2 s passa a pedir `Aperte o botao` | |
| 5 | Esperar 2 s com o gás abaixo do limite e **apertar** o botão | `state=normal`, `valve=open`, LED apagado, servo na posição aberta e o LCD de volta à temperatura | Se nada acontece, aperte de novo depois de uns 2 s abaixo do limite. O botão só vale com o ar limpo |
| 6 | Soprar no DHT11 até a umidade passar de 65 % | `dehumidifier=on`. O relé estala, o LED verde da maquete acende e o LCD mostra `LIG` | |
| 7 | Esperar a umidade cair a 55 % ou menos | `dehumidifier=off`, o relé solta e o LED verde apaga, com `OFF` no LCD | Se o Serial diz `off` mas o LED continua aceso, o módulo não está soltando. O firmware já solta o pino do relé (dreno aberto) por causa disso, então confira o fio do IN |
| 8 | (Opcional) Silenciar o buzzer pelo app (monitor de gás, "Buzzer da placa") e repetir o passo 3 | O LED acende, a válvula fecha e o app avisa, **mas o buzzer não toca**; `buzzer=muted` no Serial. Ao reiniciar a placa, o buzzer volta ligado | |
| 9 | (Opcional) Soltar o fio DATA do DHT11 | `climate=n/a` e `dehumidifier=off`. Volte o fio e a leitura retorna | |

O botão deste módulo dá nível alto quando apertado, então **o rearme acontece ao apertar**, e não ao soltar.

## Rede e API

O ESP32 cria o Wi-Fi `cisc-smart-home` (a senha está no seu `firmware/src/WifiSecrets.h`, que o Git não guarda). Logo depois de ligar, o Serial diz:

```text
wifi: network "cisc-smart-home" at 192.168.4.1, boot id 8c77dd9e
```

O `boot id` muda a cada reinício. Se o Serial disser `wifi: could not start the access point`, a casa continua funcionando sem a API: confira se a senha tem de 8 a 63 caracteres.

### Com o celular (só olhar)

1. Entre no Wi-Fi `cisc-smart-home`. O Android avisa que a rede **não tem internet**: escolha **manter conectado**.
2. No navegador, abra `http://192.168.4.1/api/v1/home`. Deve aparecer o JSON da casa, com o gás, a umidade e o desumidificador.
3. Abra `http://192.168.4.1/api/v1/history`. Deve vir uma lista de amostras (vazia nos primeiros 10 s).
4. Abra `http://192.168.4.1/api/v1/events`. O navegador não pede eventos, então deve vir um JSON de erro com `not-acceptable`. É o certo.
5. Com um vazamento (passo 3 do roteiro), recarregue `home`: `gas-state` mostra `emergency` e `gas-alerts` sobe.

### Com o computador (teste completo)

O script [`tools/check_api.py`](../tools/check_api.py) confere as rotas, os erros e o fluxo de eventos contra a placa de verdade, em uns 30 segundos. Ele só fala HTTP. **Mas o computador precisa entrar no Wi-Fi do ESP32, e fica sem internet enquanto isso.** Os três comandos juntos:

```bash
nmcli device wifi connect cisc-smart-home password 'SUA-SENHA'
python3 tools/check_api.py | tee ~/cisc-api-check.txt
nmcli connection up SUA-REDE-DE-CASA
```

O último volta para a sua rede de casa (troque `SUA-REDE-DE-CASA` pelo nome dela). Para o computador não entrar sozinho no Wi-Fi do ESP32 no futuro, apague o perfil: `nmcli connection delete cisc-smart-home`.

O script escuta o fluxo de eventos por uns 18 s para ver que ele não fica mudo. O `ping` só aparece quando a maquete fica 15 s sem mudar nada, e o clima do DHT11 costuma variar nesse tempo, então é normal só chegarem eventos `home`: eles também provam que a conexão está viva. Se algo falhar, o script diz qual verificação e o que recebeu, e avisa se a placa reiniciou no meio.

### O que observar no Serial durante o teste

- A leitura do gás (`gas=`) deve continuar em **44 a uns 60** com o Wi-Fi ligado e clientes conectados. O rádio pode somar ruído ao sensor analógico; se `gas` pular para perto de 160, me avise.
- O LCD e o alarme continuam respondendo normalmente.

## O histórico no celular (sem reconectar)

Confere que o gráfico **avança sozinho** enquanto o app está conectado. A placa guarda só uma hora de leituras, e o app busca o que é novo de 15 em 15 segundos (veja [Como o app se conecta](api.md#como-o-app-se-conecta)).

1. Abra o app e espere a faixa **Conectado à placa**. **Não feche o app nem reconecte** durante o teste.
2. Vá em **Histórico**, abra o **Monitor de gás** e escolha **1 h**. Deixe a tela ligada.
3. Espere uns 2 minutos. A ponta direita da linha **anda**, com pontos novos de uns 15 em 15 segundos. (Antes da correção, ela parava onde estava quando o app conectou.)
4. Com o app ainda aberto, aproxime o isqueiro (sem acender) do MQ-2 por alguns segundos e afaste. Em até 30 segundos o **pico** aparece no gráfico, sem reconectar.
5. Abra o **Monitor do armário** e confira que a temperatura e a umidade também avançam.
6. Aperte o botão **EN** da placa (reinicia) com o app aberto. O app mostra que perdeu a conexão e volta a **Conectado à placa**; o histórico antigo continua, e a linha recomeça de onde a placa voltou.

Se quiser ver pelo computador, com o celular em depuração USB, a leitura mais recente do banco do app (o resultado deve avançar uns 10 s a cada 15 s):

```bash
adb exec-out run-as io.github.gustavommcv.ciscsmarthome cat databases/history.db > /tmp/h.db
adb exec-out run-as io.github.gustavommcv.ciscsmarthome cat databases/history.db-wal > /tmp/h.db-wal
python3 -c "import sqlite3,time;c=sqlite3.connect('/tmp/h.db');t=c.execute(\"select max(atMs) from reading where entityId='gas-level'\").fetchone()[0];print(time.strftime('%H:%M:%S',time.localtime(t/1000)))"
```

Repita o comando depois de um minuto, sem tocar no app.

## Encher o histórico para uma apresentação

O gráfico de mês e de ano só fica cheio depois de meses de uso. Para mostrar o app antes disso, o script [`tools/seed_history.py`](../tools/seed_history.py) **preenche o passado** do histórico no celular com até um ano de horas inventadas dos três sensores: a temperatura segue o dia e a estação, a umidade sobe e desce com o desumidificador, e o gás fica quieto, com os picos de cozinhar e uns poucos vazamentos.

```bash
python3 tools/seed_history.py            # preenche um ano para trás
python3 tools/seed_history.py --dry-run  # só diz o que escreveria, sem mexer em nada
```

- Precisa do celular com **depuração USB** e do app de depuração (o script abre o banco do app com `run-as`). O app é fechado enquanto o banco muda e aberto de novo no fim.
- **Nada que o celular ouviu de verdade é tocado.** Só se preenche o tempo **anterior à primeira hora real**, e uma hora que já tem leitura nunca é sobrescrita.
- Para **tirar tudo** o que foi inventado, rode o comando que o script imprime no fim, no formato `python3 tools/seed_history.py --remove-before <número>`. Funciona porque nada que a placa diga depois pode ser mais antigo que a primeira hora real.
- As **linhas do gráfico de umidade** (quando o desumidificador liga e desliga) são as que a placa tem **agora**, não as da época: mude-as na tela do dispositivo se quiser que combinem com o que se vê.
- O mesmo número (`--seed`) gera sempre o mesmo passado.

## Anotar

Se algum passo falhar, anote o número do passo e copie 3 ou 4 linhas do monitor perto do problema. Com isso dá para saber se a causa é o fio, o sensor ou o código.
