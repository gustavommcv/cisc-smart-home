# 0003. App Android nativo em Kotlin com Jetpack Compose

**Estado:** aceita

## Contexto

O requisito é um sistema que fale com o celular do usuário. Queremos um aplicativo de verdade, que mostre notificações e se conecte a uma rede Wi-Fi sem internet, e que possa crescer.

## Decisão

Aplicativo **nativo para Android**, em **Kotlin**, com interface em **Jetpack Compose**, seguindo as [recomendações oficiais de arquitetura](https://developer.android.com/topic/architecture/recommendations).

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Flutter ou React Native | Fazem sentido para iOS e Android juntos. Aqui só precisamos do Android, e as partes críticas (rede local e serviço em segundo plano) exigem plugins ou código nativo de qualquer jeito |
| Página web (PWA) | Notificações com o app fechado exigem HTTPS e push, e a rede do ESP32 não tem internet |
| MIT App Inventor | Rápido, mas não gera um repositório organizado e testável |
| Home Assistant com ESPHome | Bom produto e open source, mas esconde o código em arquivos de configuração, e a atividade pede o código do microcontrolador |

## Consequências

- Acesso direto às APIs de rede e de notificação do Android.
- Testes locais no computador (JUnit e testes de interface do Compose), sem emulador.
- O ambiente de desenvolvimento precisa de JDK 21 e do Android SDK. O JDK 27, padrão do sistema, não é usado.
- Só roda no Android. Um porte para iOS seria um projeto novo, com a lógica de domínio como guia.
