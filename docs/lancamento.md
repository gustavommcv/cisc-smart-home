# Como lançar uma versão

Uma versão é uma tag `vX.Y.Z` no `main`. Quando a tag é enviada, o workflow [`release.yml`](../.github/workflows/release.yml) roda os testes, compila o APK e cria a página da versão com o APK, o arquivo `.sha256` e o texto do [`CHANGELOG.md`](../CHANGELOG.md).

## Passo a passo

1. **Escolha o número** (`X.Y.Z`). O aplicativo está em `0.x` enquanto a primeira versão estável não sai.
2. **Em um pull request**, mude:
   - `versionName` (para `X.Y.Z`) e `versionCode` (some 1) em [`android/app/build.gradle.kts`](../android/app/build.gradle.kts);
   - o [`CHANGELOG.md`](../CHANGELOG.md): uma seção nova, **`## vX.Y.Z — AAAA-MM-DD`**, no topo da lista, com o texto que vai para a página da versão.
3. **Mescle o PR** e confira que o CI do `main` passou.
4. **Crie e envie a tag**, no commit do `main` que você quer lançar:

   ```bash
   git switch main && git pull
   git tag -a vX.Y.Z -m "vX.Y.Z"
   git push origin vX.Y.Z
   ```

5. **Acompanhe** o workflow *Release* na aba Actions. No fim, a versão aparece em [Releases](https://github.com/gustavommcv/cisc-smart-home/releases).

O workflow **recusa a tag** se ela não for igual a `v` mais o `versionName` do aplicativo, ou se o `CHANGELOG.md` não tiver a seção dela. Nesse caso, nada é publicado.

## Testar o workflow sem lançar

Em Actions, *Release*, **Run workflow** (sem tag): ele compila e guarda o APK como arquivo do workflow, mas **não cria** nenhuma versão.

## Se algo deu errado

Uma versão publicada por engano se apaga com `gh release delete vX.Y.Z --cleanup-tag`, e a tag local com `git tag -d vX.Y.Z`. Corrija e lance de novo. Quem já baixou o APK continua com ele: por isso o número de uma versão **não se reaproveita** depois de divulgada; lance a seguinte.

## O que a versão não traz

- **O firmware pronto:** a senha do Wi-Fi da maquete é de cada um, então o firmware é gravado a partir do código, como no [guia](como-rodar.md).
- **Um APK assinado para a loja, nem uma assinatura que se mantenha entre versões:** o APK é assinado com a chave de depuração do Android, que o Gradle cria **a cada compilação** no servidor. Por isso o Android pode recusar instalar uma versão por cima da outra, e quem atualiza tem de desinstalar antes (o histórico do celular se perde). Uma chave fixa resolveria, mas ela precisaria ficar guardada fora do repositório (ou ser uma chave de depuração pública, aceitando que qualquer um pode assinar um APK igual), e isso fica para quando o projeto for publicado de verdade.
