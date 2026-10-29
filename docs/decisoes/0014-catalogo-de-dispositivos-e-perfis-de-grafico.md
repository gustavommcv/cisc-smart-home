# 0014. O que o app sabe de cada dispositivo fica num catálogo, e o gráfico é escolhido pelo tipo do sensor

**Estado:** aceita

## Contexto

O app mostrava o mesmo gráfico para tudo: uma linha na escala do que o sensor leu. Para o gás isso engana: o ar limpo, que oscila de 44 a 60, ocupa a tela inteira e parece um drama, e o limite de 160, a única coisa que importa, nem aparece. A umidade importa em relação aos limites do desumidificador, e a temperatura, em relação a quanto ela de fato variou.

Além disso, o que o app sabe de cada dispositivo estava espalhado em `when (id)` em seis arquivos (o ícone, o que se diz embaixo do nome, a lista da maquete no histórico, quem é temperatura e umidade para o gráfico que as compara). Um dispositivo novo pedia mexer em todos eles, e o gráfico de cada tipo de sensor não tinha onde morar.

## Decisão

- **Um catálogo no domínio (`DeviceCatalog`)** guarda, para cada dispositivo conhecido, o cômodo, o ícone, o que ele diz embaixo do nome e os seus sensores. Cada sensor tem um **tipo** (`SensorKind`: gás, temperatura, umidade, genérico), uma unidade e um **`ChartProfile`**.
- **O perfil do gráfico é só dado**: a escala (um intervalo que sempre aparece e cresce se a leitura passar, e uma largura mínima), os limites (um valor fixo ou o valor de uma entidade da casa, que o usuário pode mudar), se a área sob a linha é pintada e o que se desenha para um mês ou um ano (a faixa de mínimo e máximo de cada hora, ou o pico de cada dia). As telas **só leem o perfil**; quem desenha não sabe de qual dispositivo se trata.
- **O que se desenha vem do tipo do sensor, não do dispositivo.** Um dispositivo novo com um sensor de temperatura é desenhado como o de temperatura. Um sensor que o catálogo não conhece cai no gráfico genérico, o de antes, e funciona sem nada a mais.
- **Os limites que o usuário muda são lidos da casa**, não copiados: a faixa do desumidificador acompanha os valores de `dehumidifier-on-at` e `dehumidifier-off-at`.
- **A apresentação escolhida:** o gás com a escala fixa de 0 a 400, a linha do limite de alarme, a área sob a linha e o que passa do limite em vermelho, e barras do pico de cada dia no mês e no ano; a umidade com a faixa entre o que o desumidificador liga e desliga; a temperatura numa escala nunca menor que 10 °C, para o ruído do sensor não parecer drama.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Continuar com `when (id)` em cada tela | Cada dispositivo novo toca seis arquivos, e o desenho de cada sensor continua sem lugar |
| Deixar a apresentação só no firmware (a placa diz como desenhar) | É o destino, mas pede mudar o contrato e a placa; o catálogo no app já resolve a maquete e é a forma que o contrato vai preencher depois |
| Um tipo de gráfico por dispositivo | Dois dispositivos com temperatura teriam dois desenhos; o tipo do sensor é o que decide |
| Uma biblioteca de gráficos com tudo isso | A decisão 0008 já explica: os vazios e os picos são regra de segurança nossa |

## Consequências

- **Um dispositivo novo é uma entrada no catálogo** e as palavras dele nos recursos de texto; ou, sem entrada nenhuma, funciona com o gráfico genérico.
- O **limite de alarme do gás (160)** é hoje uma constante do app, igual à do firmware (`Settings.h`). Se mudar lá, tem de mudar aqui. É o único ponto frágil desta etapa.
- **O que fica para depois**, em duas etapas: (2) o contrato (`docs/api.md`) passa a trazer, em cada sensor, a **classe** e as dicas do gráfico (escala e limites), e o firmware as declara, no modelo do Home Assistant; o catálogo vira a tabela que traduz classe em apresentação, e o 160 deixa de estar duplicado; (3) o histórico passa a guardar também os **estados** (o desumidificador ligado, o estado do gás), o que permite desenhar *quando* o desumidificador ficou ligado e marcar os vazamentos no gráfico. Hoje o histórico só tem as três leituras numéricas.
- O desenho em si (as linhas, as barras, as cores) não tem teste automático, como o resto das telas; o que decide o que se desenha (a escala, os limites, as barras, o catálogo) tem.
