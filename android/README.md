# App Android

O aplicativo do CISC Smart Home: mostra a casa, comanda o desumidificador e avisa do vazamento de gás, conversando com o ESP32 pelo contrato de [`docs/api.md`](../docs/api.md). É escrito em Kotlin, com a interface em Jetpack Compose.

> **Estado atual:** o esqueleto. Há uma tela provisória, o teste que protege a arquitetura e o CI. As camadas e as telas chegam nas próximas partes (veja as [fases](../README.md#fases)).

## Ferramentas

- **Android SDK** com a plataforma **37** (usada para compilar) e o build-tools 36. O app mira a API **36**, o Android do celular de teste, e exige **Android 10 (API 29)** ou mais, que é quando um app passou a poder entrar sozinho numa rede Wi-Fi sem internet.
- **JDK 21.** Você não precisa configurar o `JAVA_HOME`: o arquivo [`gradle/gradle-daemon-jvm.properties`](gradle/gradle-daemon-jvm.properties) faz o Gradle achar o JDK 21 por conta própria, mesmo que o Java padrão do seu sistema seja outro. O código em si é compilado com o JDK 17, que o Gradle baixa se faltar.
- **Gradle**, pelo wrapper (`./gradlew`).

Crie o `android/local.properties` (o Git o ignora) apontando para o seu SDK:

```properties
sdk.dir=/caminho/para/o/Android/Sdk
```

## Comandos

Tudo a partir desta pasta:

```bash
./gradlew testDebugUnitTest     # os testes, no computador
./gradlew lintDebug             # a análise estática
./gradlew assembleDebug         # o APK de teste, em app/build/outputs/apk/debug/
```

Instalar no celular, com a depuração USB ligada:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

O APK de teste é assinado com a chave de depuração do Android, que basta para instalar. Não há assinatura de versão.

## Organização

```text
app/src/main/kotlin/io/github/gustavommcv/ciscsmarthome/
├── domain/    modelos e regras, sem Android (Home, Area, Device, Entity, e as regras de tempo e de alerta)
├── data/      o cliente do ESP32 (REST + SSE), o histórico local e os repositórios; hoje, `data/remote` com o cliente REST e o dos eventos
├── ui/        as telas em Compose e um ViewModel por tela
├── service/   o serviço que mantém a conexão e dispara as notificações
└── di/        a montagem das dependências, sem framework
```

Cada pasta nasce junto com o primeiro arquivo que ela guarda. Hoje existem `ui/`, `domain/` e `data/`.

### O domínio

Kotlin puro, testado só com JUnit, sem nenhuma biblioteca:

| Arquivo | O que faz |
|---|---|
| `Home` | A casa como a placa a descreve: cômodos, dispositivos e entidades (`Sensor`, `BinarySensor`, `Switch` e `Select`), com um valor nulo quando não há leitura válida. Acha uma entidade pelo `id` e diz em que dispositivo e cômodo ela está |
| `DeviceClock` | Põe o `uptimeMs` da placa no relógio do celular, como pede a seção "Tempo" de [`docs/api.md`](../docs/api.md). A placa não guarda hora: o celular anota a diferença a cada resposta, uma por partida |
| `AlertTracker` | Decide quando um vazamento é notícia. O par (identificador de partida, contador de vazamentos) dá nome a cada um, então o mesmo vazamento é avisado **uma vez**, chegue pelo evento `alert`, pelo estado da casa ao reconectar, ou pelos dois. Vazamentos que terminaram enquanto o app estava fora não avisam depois. Guarda o que já foi avisado, para não repetir nem depois de o app reiniciar |

O domínio também tem o `HistorySample` e o `HistoryPage` (as leituras guardadas pela placa) o `CommandValue` (o que o usuário pede a uma entidade: ligar ou desligar, ou escolher uma opção) e o `HomeEvent` (o que a placa anuncia sozinha: a casa mudou, ou um vazamento de gás começou). As interfaces do domínio, como a do repositório, nascem junto com o primeiro código que as usa, a partir da 4b4. Criá-las antes seria código sem uso.

### O cliente REST

Em `data/remote`, o lado REST do contrato de [`docs/api.md`](../docs/api.md). Usa OkHttp para a rede e `kotlinx.serialization` para o JSON.

| Arquivo | O que faz |
|---|---|
| `BoardClient` | Lê a casa, manda um comando a uma entidade e busca o histórico (com `after` e `limit`). Cada chamada **devolve** o que deu, em vez de lançar exceção. Não escolhe a rede, o endereço nem os tempos limite: quem o cria os dá, e por isso os testes o apontam para um servidor falso. Se a corrotina for cancelada, a chamada à placa é cancelada junto |
| `BoardResult` e `BoardError` | O resultado: a resposta, ou por que não houve. A placa **não respondeu** (`Unreachable`: sem rede, rede errada, lenta demais), **respondeu com um erro do contrato** (`Rejected`, com o código reconhecido) ou **respondeu o que o app não entende** (`Unexpected`) |
| `BoardDtos` e `BoardMapping` | O JSON como ele viaja, e a tradução para o domínio. O app é **tolerante**, como o contrato pede: ignora campos desconhecidos, descarta entidades de um tipo que não conhece, e um valor que não combina com o tipo conta como "sem leitura válida" |

### Os eventos

Também em `data/remote`, o lado SSE do contrato: o fluxo `GET /api/v1/events`, lido com o `okhttp-sse`.

| Arquivo | O que faz |
|---|---|
| `BoardEventStream` | Ouve o fluxo e **continua ouvindo**: quando a conexão cai, por qualquer motivo, avisa e conecta de novo sozinho. Quem o usa recebe um `Flow` e cancela o `Flow` para parar, o que cancela a chamada à placa |
| `BoardStreamUpdate` | O que o fluxo conta: `Connected` (a placa aceitou), `Received` (a placa anunciou algo) e `Lost` (a conexão caiu, e por quê, com o mesmo `BoardError` do cliente REST) |
| `Backoff` | A espera entre as tentativas: 1 s depois da primeira falha, o dobro a cada falha seguida, no máximo 10 s. Volta a 1 s depois de uma conexão que funcionou |
| `BoardDecoding` | A leitura que o cliente REST e o fluxo dividem: a casa (com a conferência da versão da API) e o erro do contrato. Também lê o `alert` |

Três decisões que não aparecem no código à primeira vista:

- **"20 segundos sem receber nada" é o tempo limite de leitura da conexão.** O contrato manda reconectar depois de 20 s de silêncio, e é exatamente o que o tempo limite de leitura do OkHttp faz: qualquer byte recebido, inclusive um `ping`, zera a contagem. Por isso o fluxo não conta o tempo por conta própria. Em compensação, ele troca o tempo limite de leitura do cliente que recebe. O tempo limite da chamada continua valendo só até a conexão abrir; depois o `okhttp-sse` o desliga, porque cortaria um fluxo feito para durar.
- **O `alert` não diz de qual partida da placa é.** O app usa o `bootId` do `home` que a placa envia logo ao conectar, na mesma conexão. Uma conexão não atravessa um reinício, então esse `bootId` vale para todos os alertas dela. Um `alert` que chegue antes de qualquer `home` é descartado.
- **O que o app não consegue ler é pulado, e o fluxo continua.** Todo `home` traz o estado inteiro, então o próximo conserta o que se perdeu. Até um `alert` perdido não passa em branco: o vazamento aparece no `home` seguinte, por `gas-state` e `gas-alerts`, e o `AlertTracker` avisa por ali.

O app ainda não pede a permissão de internet nem liberar o tráfego HTTP para o endereço da placa: isso entra na 4d, quando os clientes forem ligados ao app.

### A regra de dependência é testada

O teste [`ArchitectureTest`](app/src/test/kotlin/io/github/gustavommcv/ciscsmarthome/architecture/ArchitectureTest.kt) lê os `import` de todos os arquivos e falha se uma camada depender do que não deve:

| Camada | Não pode importar |
|---|---|
| `domain` | Android, `ui`, `data`, `service`, `di` |
| `data` | `ui`, `service`, `di` |
| `ui` | `data`, `service` |
| `service` | `data`, `ui` |

Ou seja, as telas e o serviço só chegam aos dados pelas interfaces do domínio, ligadas pelo `di`. Um arquivo fora dessas pastas também falha: quem cria uma camada nova decide isso de propósito, nas regras.

## Textos da interface

A interface é em português, a língua de quem a usa, e os textos ficam em `res/values/strings.xml`. O código e os comentários são em inglês.
