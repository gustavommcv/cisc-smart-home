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
├── domain/    modelos e regras, sem Android (Area, Device, Entity e as interfaces)
├── data/      o cliente do ESP32 (REST + SSE), o histórico local e os repositórios
├── ui/        as telas em Compose e um ViewModel por tela
├── service/   o serviço que mantém a conexão e dispara as notificações
└── di/        a montagem das dependências, sem framework
```

Cada pasta nasce junto com o primeiro arquivo que ela guarda. Hoje existe só a `ui/`.

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
