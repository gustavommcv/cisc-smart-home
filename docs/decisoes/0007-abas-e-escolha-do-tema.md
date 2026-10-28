# 0007. Navegação por abas e tema escolhido pelo usuário

**Estado:** aceita

## Contexto

O app deixou de ser uma lista e ganhou lugares de nível alto: a casa, o histórico das leituras e os ajustes. Dentro da casa há um nível a mais, o cômodo. Ao mesmo tempo, o usuário pediu que o **tema seja uma escolha dele**: claro, escuro ou automático (o que o celular estiver usando).

## Decisão

**Três abas numa barra de baixo:** Início, Histórico e Ajustes. Cada aba tem o seu `ViewModel` e guarda o próprio estado, então sair de uma e voltar a encontra como estava (um cômodo que estava aberto continua aberto). A aba aberta é um estado salvo com a tela; o botão de voltar leva do Histórico ou dos Ajustes para o Início, e do Início fecha o cômodo e, depois, o app. A navegação é **feita à mão**, sem biblioteca.

**O tema tem três valores** (`ThemeMode`): `Claro`, `Escuro` e `Automático`. O **padrão é o automático**, que é o que o app já fazia. A escolha fica no celular, num arquivo de preferências com uma palavra (o nome do valor), como a memória dos alertas; é lida ao abrir o app e vale **na mesma hora** em que muda. Uma palavra que o app não conhece (um arquivo de uma versão futura, um arquivo estragado) vale como nenhuma escolha, para nunca impedir o app de abrir.

**As barras do sistema acompanham o tema do app, não o do celular.** Com o app claro num celular escuro, os ícones da barra de status ficariam claros sobre uma barra clara. A janela que aparece antes de o Compose desenhar segue o sistema.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Biblioteca de navegação (Navigation 3 ou Navigation Compose) | São três abas e um nível dentro de uma delas; uma biblioteca só acrescentaria código. Entra quando houver destinos com argumentos ou links de entrada |
| DataStore para guardar o tema | Uma dependência a mais para guardar uma palavra |
| Seguir só o tema do sistema | O usuário pediu para escolher |
| Claro como padrão | O usuário descreveu a escolha como "claro, escuro ou automático (padrão do sistema)", e o automático é o que já valia |

## Consequências

- O `AppDependencies` ganha o `themeSettings`, pelo mesmo caminho do repositório: a tela o lê, a aba de ajustes o muda, e nenhuma importa a outra.
- O arquivo `theme-settings` e a chave `theme` fazem parte do que o usuário já tem no celular: mudar o nome deles apagaria a escolha de quem já escolheu. Um teste os guarda.
- Com o app claro e o celular escuro (ou o contrário), a janela de abertura pode aparecer por um instante na cor do sistema antes de o app se desenhar.
- A aparência da barra e dos ajustes não tem teste automático (o app não usa a biblioteca de teste do Compose); a lógica (as abas, o voltar, o valor do tema, o arquivo) tem.
