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
- **Etapa 2 (feita): a placa diz.** O contrato ganhou, nos sensores, a `class` e as `limits` (docs/api.md), o firmware as declara, e o app as prefere ao catálogo. O **limite de alarme do gás** sai da configuração da placa, não de uma cópia: o `160` do catálogo do app ficou só como **reserva para uma placa antiga** que não declara nada. Um dispositivo novo com sensores que dizem a classe é desenhado pelo tipo, sem entrada no catálogo.
- **Etapa 3 (feita): o histórico guarda os estados.** Cada amostra da placa traz o estado do gás e se o desumidificador estava ligado (`states` em `GET /history`), o app os guarda numa tabela própria (banco versão 3) e o gráfico os desenha: uma **faixa** sob a umidade com o tempo em que o desumidificador ficou ligado, e **faixas vermelhas** sobre o gás onde houve vazamento. Quais estados cada gráfico desenha vem do **tipo do sensor** (`ChartProfile.states`), então qualquer dispositivo com um sensor de gás ou de umidade ganha isso. Os estados saem **junto com as leituras**, depois de alguns dias, por isso só os gráficos de 1 h, 6 h e 24 h os têm; o mês e o ano, feitos de horas, não. Um trecho nunca se estende por um silêncio do app: o tempo em que o app não ouviu a placa não é dito "ligado".
- **O que não está feito:** a placa não declara **quais** estados vão no gráfico (vêm do catálogo, pelo tipo do sensor). É o mesmo passo que a etapa 2 deu para a classe e os limites, e fica para quando houver outro dispositivo que o peça.
- O desenho em si (as linhas, as barras, as cores) não tem teste automático, como o resto das telas; o que decide o que se desenha (a escala, os limites, as barras, o catálogo) tem.
