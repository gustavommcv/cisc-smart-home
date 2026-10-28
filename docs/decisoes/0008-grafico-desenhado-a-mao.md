# 0008. O gráfico do histórico é desenhado à mão, sem biblioteca

**Estado:** aceita

## Contexto

A aba Histórico mostra uma linha por sensor (gás, temperatura, umidade). A placa faz uma leitura a cada 10 segundos e o app guarda todas no celular, então um dia de histórico são **8 640 pontos** por sensor, mais do que uma tela tem pixels para distinguir. Duas coisas pesam mais que o desenho:

- **Um vazamento dura segundos.** Num gráfico de 24 horas, suavizar ou tirar a média dos pontos apagaria justamente o que mais importa.
- **O app não ouve a placa o tempo todo.** Ligar com uma reta o último ponto antes de um buraco ao primeiro depois diria que nada aconteceu naquele tempo.

## Decisão

Desenhar no `Canvas` do Compose: **uma linha** sobre três linhas de grade, em cerca de 50 linhas de código. O que decide o que se vê fica em funções puras, testadas sem celular (`chartOf`):

- **Os vazios ficam vazios.** Um silêncio de mais de **2 minutos** entre duas leituras quebra a linha; o tempo sem leitura continua em branco, e uma leitura sem valor (`null`) também conta como tempo sem leitura.
- **A redução guarda os picos.** Um trecho com mais leituras do que a tela mostra é cortado em 150 partes, e de cada parte ficam **a menor e a maior** leitura, na ordem do tempo. A média nunca é usada.
- **A janela anda com o tempo.** Ela é pedida de novo a cada 30 segundos, e termina na hora em que o gráfico é feito, então a leitura que acabou de chegar entra.
- **O histórico se lê sem a placa.** Ele está no celular; sem a casa carregada, a aba oferece os três sensores da maquete.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Vico (biblioteca de gráficos para Compose) | Uma dependência a mais, e a ser mantida, para desenhar uma linha. O que fazer com os vazios e com a redução é regra de segurança do nosso caso, e uma biblioteca decide isso por nós |
| MPAndroidChart | Feita para as `View`s antigas, fora do Compose, e sem manutenção ativa |
| Média por trecho em vez de mínimo e máximo | Mais simples, mas achata o pico do gás |

## Consequências

- Não há toque nem zoom num ponto: o gráfico mostra o menor e o maior valor, a janela e os vazios. Se o app precisar de mais (comparar sensores, marcar vazamentos), a decisão de biblioteca volta a ser avaliada.
- A regra do gráfico tem testes que quebram se um pico sumir, se um vazio for ligado ou se a janela não andar. A aparência do desenho não tem teste automático (o app não usa a biblioteca de teste do Compose) e foi conferida no celular.
- O app guarda tudo por enquanto. Quando for preciso limpar o que é muito antigo, o limite deve seguir o maior período que o gráfico mostra (24 horas).
