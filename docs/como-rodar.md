# Como baixar e rodar

Este guia leva do zero até a maquete funcionando e o aplicativo mostrando a casa. Escolha até onde ir:

| Quero… | Preciso de… | Seções |
|---|---|---|
| **Só instalar o aplicativo** | Um celular Android 10 ou mais novo | 1 e 2 |
| **Montar a maquete completa** | O celular, um ESP32 DevKit de 38 pinos, as peças da [lista de materiais](../hardware/README.md) e um computador com PlatformIO | 1 a 5 |
| **Mexer no código** | Mais o JDK 21 e o Android SDK, para o app | 6 e 7 |

Sem a placa o aplicativo abre, mas não há casa para mostrar: ele só mostra as telas e o histórico que já tiver guardado.

## 1. Baixar o projeto

Com o Git:

```bash
git clone https://github.com/gustavommcv/cisc-smart-home.git
cd cisc-smart-home
```

Sem o Git, na [página do repositório](https://github.com/gustavommcv/cisc-smart-home), **Code → Download ZIP**, e descompacte.

Para o código de uma versão lançada (e não o mais novo), use a [página de versões](https://github.com/gustavommcv/cisc-smart-home/releases): o arquivo **Source code (zip)** de cada versão.

## 2. Instalar o aplicativo no celular

1. No celular, abra a [última versão](https://github.com/gustavommcv/cisc-smart-home/releases/latest) e baixe o arquivo `cisc-casa-<versão>.apk`.
2. Abra o arquivo baixado. O Android pede para permitir **instalar apps desconhecidos** a partir do navegador (ou do app de arquivos): permita só para esta instalação.
3. Instale. O Android pode avisar que o app não é da loja e que o Play Protect não o conhece. É esperado: o aplicativo **não é publicado na loja** e o APK é assinado com a chave de depuração do Android, que basta para instalar. O projeto não tem assinatura de versão.

Para conferir que o arquivo não foi alterado, baixe também o `.sha256` da mesma página e, no computador:

```bash
sha256sum -c cisc-casa-<versão>.apk.sha256
```

**Atualizando:** o APK de cada versão é assinado com uma chave de depuração criada na hora da compilação. Por isso o Android pode recusar instalar por cima de outro APK, com um erro de assinatura (isso também vale para um app que você compilou no seu computador). Nesse caso, **desinstale o app antes**. O histórico que ele guarda no celular é apagado junto, porque só existe lá. O firmware da placa não precisa ser gravado de novo por causa disso.

Se preferir **compilar e instalar por USB**, veja a seção 6.

## 3. Montar a maquete

Monte pelo [esquemático e pela lista de materiais](../hardware/README.md). O ESP32 é o de **38 pinos**, e os pinos estão em `firmware/src/BoardConfig.h`. Três cuidados que evitam a maior parte dos problemas:

- O MQ-2 trabalha com 5 V e o pino do ESP32 aguenta 3,3 V: o **divisor de tensão** do esquemático é obrigatório.
- O LCD usa 5 V: o SDA e o SCL passam pelo **conversor de nível**.
- O servo tem uma **fonte própria** de 5 V, com o GND ligado ao do resto. Sem isso ele só estala.

## 4. Gravar o firmware no ESP32

### 4.1 O que instalar

O firmware usa o [PlatformIO](https://platformio.org/). Escolha um:

```bash
pip install platformio     # na linha de comando
```

ou a extensão **PlatformIO IDE** do VS Code. Confira com `pio --version`. Na primeira compilação o PlatformIO baixa sozinho a plataforma do ESP32 (núcleo Arduino-ESP32 3.3.12) e as bibliotecas: são algumas centenas de megabytes, só dessa vez.

- **Linux:** para usar a porta USB sem `sudo`, entre no grupo da porta (`dialout` no Debian e no Ubuntu, `uucp` no Arch) e saia e entre de novo na sessão.
- **Windows:** se a placa não aparecer como porta, instale o driver do chip USB dela (CP210x ou CH340, está escrito na placa).
- Use um cabo USB **de dados**: alguns cabos só carregam.

### 4.2 Escolher a senha do Wi-Fi da maquete

A própria placa cria a rede Wi-Fi `cisc-smart-home`. A senha dela fica num arquivo que o Git não guarda:

```bash
cd firmware
cp src/WifiSecrets.example.h src/WifiSecrets.h
```

Abra `src/WifiSecrets.h` e troque o texto de `kAccessPointPassword` por uma senha sua, de **8 a 63 caracteres, sem acentos**. Você vai digitá-la no aplicativo. Se pular este passo, o firmware compila com uma senha de exemplo (`change-me-please`) e avisa: não use essa onde outras pessoas possam chegar à rede.

### 4.3 Gravar e acompanhar

Com o ESP32 na USB:

```bash
cd firmware
pio run -e esp32dev -t upload
pio device monitor
```

Se houver mais de uma placa ou porta, diga qual: `pio run -e esp32dev -t upload --upload-port /dev/ttyUSB0`.

O monitor abre a 115200 e mostra, logo depois de ligar:

```text
lcd: found
wifi: network "cisc-smart-home" at 192.168.4.1, boot id 8c77dd9e
```

e, a cada segundo, uma linha com o estado da casa (`gas=… state=normal valve=open …`). Para sair do monitor, `Ctrl+C`. **Feche o monitor antes de gravar de novo**, porque ele segura a porta.

Para conferir os fios, os sensores e os atuadores um a um, siga o [roteiro de teste da maquete](roteiro-maquete.md).

### Se a gravação falhar

| Mensagem ou sintoma | O que fazer |
|---|---|
| `Failed to connect` / a placa não responde | Segure o botão **BOOT** da placa enquanto o PlatformIO diz `Connecting…`; troque o cabo por um de dados |
| `Invalid head of packet` ou a gravação cai no meio | Baixe a velocidade: em `firmware/platformio.ini`, no `[env:esp32dev]`, acrescente `upload_speed = 115200` |
| `could not open port` / `Permission denied` | Feche o monitor serial; no Linux, veja o grupo da porta na seção 4.1 |
| `wifi: could not start the access point` | A senha em `WifiSecrets.h` não tem de 8 a 63 caracteres. A casa continua funcionando sem a rede |
| `lcd: not found, running without it` | O LCD não respondeu: confira o SDA e o SCL pelo conversor de nível e os 5 V do LCD. Sem LCD o resto funciona |

## 5. Primeira vez no aplicativo

1. Ligue a maquete. Espere uns **10 segundos**: o sensor de gás aquece e a válvula fica fechada nesse tempo.
2. Abra o **CISC Casa** e aceite pedir as notificações (a partir do Android 13): é por elas que o aviso de vazamento chega, também com a tela apagada.
3. Vá em **Ajustes**, na conexão **Wi-Fi da placa**, digite a **senha do Wi-Fi da placa** (a de `WifiSecrets.h`) e toque em **Guardar senha**. Ela fica guardada só neste celular.
4. Volte ao **Início**. O app entra sozinho na rede `cisc-smart-home`, **sem você mexer no Wi-Fi do celular**. O Android mostra um aviso pedindo para conectar a ela: aceite. Aparece a faixa **Conectado à placa** e os dois cômodos, **Cozinha** e **Quarto**.

Se o celular já estiver na rede `cisc-smart-home` (você entrou à mão, ou por uma VPN que chegue até a placa), escolha em Ajustes a conexão **Rede do celular**.

O que dá para fazer, para ver tudo funcionando:

- Em **Quarto**, ligar o desumidificador (passa para o modo manual) e mudar os limites de umidade.
- Em **Cozinha**, escolher como o vazamento avisa (alarme, notificação ou desligado) e silenciar o buzzer.
- Aproximar um isqueiro (sem acender) do MQ-2: a válvula fecha, o buzzer toca e o app avisa. O botão da maquete reabre, depois de uns 2 segundos de ar limpo.
- Na aba **Histórico**, escolher um dispositivo e ver os gráficos, também por mês e por ano.

### Se o app não conectar

| O app diz | O que fazer |
|---|---|
| Falta a senha do Wi-Fi da placa | Digite em Ajustes, na conexão Wi-Fi da placa |
| Não consegui entrar no Wi-Fi cisc-smart-home | A placa está ligada e perto? A senha é a mesma de `WifiSecrets.h`? Você aceitou o aviso do Android? |
| Sem conexão com a placa | O celular precisa estar no Wi-Fi `cisc-smart-home`; se não conectar, desligue os dados móveis e tente de novo |

No ar limpo, o MQ-2 desta maquete costuma ler entre **44 e 60**, parado. Não é defeito: é o piso de leitura do conversor do ESP32. O alarme dispara acima de 160.

## 6. Compilar o aplicativo

Para quem quer o APK saído do código, ou vai mexer nele. Precisa de:

- o **JDK 21** (o Gradle o encontra sozinho; o código é compilado com o 17);
- o **Android SDK** com a plataforma **37** (o Android Studio instala), e o arquivo `android/local.properties` com `sdk.dir=/caminho/do/sdk` (o Android Studio o cria).

```bash
cd android
./gradlew testDebugUnitTest lintDebug assembleDebug
```

O APK fica em `android/app/build/outputs/apk/debug/app-debug.apk`. Para instalá-lo com o celular na USB e a **depuração USB** ligada:

```bash
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
```

## 7. Rodar os testes

Não precisam da placa nem do celular:

```bash
cd firmware && pio test -e native        # as regras e a API do firmware
cd ../android && ./gradlew testDebugUnitTest
```

O teste do firmware precisa de um compilador C++ no computador (`g++` ou `clang`). Mais detalhes das ferramentas estão no [README](../README.md#ferramentas-de-desenvolvimento).
