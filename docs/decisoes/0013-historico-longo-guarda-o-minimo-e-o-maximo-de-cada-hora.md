# 0013. O histórico longo guarda o menor e o maior valor de cada hora, e as leituras só por uma semana

**Estado:** aceita

## Contexto

O usuário quer ver o histórico por **mês** e por **ano**, além das últimas horas. A placa faz uma leitura a cada 10 segundos e o app guarda todas: são 8 640 por dia para cada entidade, uns 26 mil por dia com as três do histórico, quase **10 milhões por ano**. Guardar tudo para sempre pesaria no celular (centenas de megabytes) e não serviria de nada: a tela não distingue tanto ponto, e um gráfico de um ano mostra o desenho do ano, não cada leitura.

Mas há uma regra que vem da [decisão 0008](0008-grafico-desenhado-a-mao.md): **um vazamento dura segundos, e o gráfico nunca pode apagá-lo**. Resumir cada hora por uma média faria exatamente isso.

## Decisão

- **Cada hora de cada entidade guarda o seu menor e o seu maior valor**, numa segunda tabela, `reading_hour` (entidade, início da hora, menor, maior). Ela é atualizada **na mesma transação** que grava as leituras, por um `INSERT ... ON CONFLICT DO UPDATE` que só **alarga** a hora: o menor só desce e o maior só sobe. Por isso gravar de novo uma leitura que o app já tinha não muda nada, e as horas não dependem de contar o que foi repetido.
- **As leituras de 10 em 10 segundos ficam por uma semana** (`HomeRepositoryImpl.READINGS_KEPT`). Quando a placa conecta, o app joga fora as mais velhas, no máximo uma vez por hora, para uma placa que cai e volta toda hora não fazer o banco varrer tudo a cada vez. **As horas ficam para sempre**: umas 8 760 linhas por ano para cada entidade.
- **A versão 2 do banco faz as horas a partir das leituras que o celular já tem** (`MIGRATION_1_2`), para um celular que já guardava histórico não perder o que ouviu. O formato novo está em `app/schemas`.
- As horas são horas **do relógio absoluto** (de hora em hora desde a época), não do fuso: agrupar em dias e meses, no fuso do celular, é coisa da tela.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Média por hora | Achata o pico: um vazamento de 20 segundos some numa hora de leituras normais. Vai contra a decisão 0008 |
| Guardar todas as leituras | Quase 10 milhões de linhas por ano para três sensores, para desenhar uma linha que cabe em 150 pontos |
| Calcular o mês na hora, com `GROUP BY` sobre as leituras | Precisa das leituras, e elas não ficam: mesmo que ficassem, seriam 260 mil linhas por entidade a cada consulta |
| Apagar as leituras velhas sem resumir | O mês e o ano deixariam de existir |
| Guardar também a soma e a contagem, para tirar médias | Ninguém pede média ainda; se pedirem, é uma coluna nova e uma migração nova |
| Um índice só em `atMs`, ou um trabalho agendado, para apagar as velhas | Mais uma peça para uma limpeza que roda uma vez por hora; o `DELETE` sem índice varre a tabela, e isso é aceitável nessa frequência |

## Consequências

- O gráfico de **1 hora, 6 horas e 24 horas** continua lendo as leituras; o de **mês** e **ano** lê as horas e mostra, para cada uma, o menor e o maior valor, o que mantém os picos. Ele não mostra a média.
- Quem abrir o app depois de mais de uma semana parado perde o detalhe de 10 segundos do que passou, mas não o desenho do mês: o menor e o maior de cada hora estão lá.
- A semana é uma constante, fácil de mudar, mas **apagar não tem volta**: uma leitura que passou do prazo só existe como parte de uma hora.
- Uma hora em que o app não ouviu a placa não existe na tabela, e o gráfico a deixa vazia, como já faz com os buracos (decisão 0008).
- A migração roda uma vez, no primeiro uso depois da atualização. Foi testada com um banco da versão 1 criado à mão (`HistoryMigrationTest`).
