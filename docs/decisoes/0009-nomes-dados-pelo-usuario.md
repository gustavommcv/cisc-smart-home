# 0009. Os nomes de cômodos e dispositivos são do usuário, ficam no celular e o alarme os diz

**Estado:** aceita

## Contexto

A placa dá nomes em inglês ("Kitchen", "Gas monitor") e o app tem os seus, em português, para os dois cômodos e os dois dispositivos da maquete. Numa casa de verdade, quem usa o app chama as coisas pelo nome que conhece: "Cozinha da vó", "Detector da pia". A decisão [0004](0004-modelo-comodo-dispositivo-entidade.md) já dizia que o usuário daria nome ao que instala.

Um nome que o usuário deu também aparece no **alarme de vazamento**, e ali ele importa mais do que em qualquer tela: quem ouve o alarme precisa ler o lugar com a palavra que usa para ele.

## Decisão

- **Cômodos e dispositivos podem ser renomeados**; as leituras ("Temperatura", "Nível de gás") não. O lápis fica no cabeçalho do cômodo e no cartão de cada dispositivo.
- **O nome que vale é, nesta ordem:** o que o usuário digitou, o nome em português do app (para o que a maquete tem) e, por último, o da placa.
- **Os nomes ficam no celular, por tipo e id** (`area:kitchen`, `device:gas-monitor`), num arquivo de preferências, como a memória dos alertas. A placa não guarda nomes.
- **Até 30 caracteres.** Sem espaço sobrando nas pontas; **um nome em branco volta ao padrão**, e há também o botão "Voltar ao nome padrão".
- **O alarme diz o nome do usuário** ("Local: Cozinha da vó"). Ele é lido na hora de montar o aviso, então um nome dado um instante antes já vale.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Guardar os nomes na placa | O contrato (`docs/api.md`) não tem como escrevê-los, e a placa perde tudo ao reiniciar |
| Renomear também as leituras | Quem instala um dispositivo dá nome ao dispositivo, não a cada leitura dele; e os textos das leituras são o vocabulário do app |
| Sincronizar os nomes entre celulares | Não há servidor (decisão [0001](0001-esp32-como-ponto-de-acesso.md)) |
| Deixar o alarme com o nome do app | Em português, "Cozinha" é certo para a maquete e errado para quem chamou o cômodo de outra coisa |

## Consequências

- O arquivo `user-names` e as chaves `area:<id>` e `device:<id>` são o que o usuário já terá no celular: mudar o nome deles apagaria os nomes. Um teste os guarda.
- Os nomes são do aparelho: um segundo celular não os vê. Se a placa mudar o id de um cômodo, o nome dado a ele deixa de valer, porque o nome está ligado ao id.
- O limite de 30 caracteres vale para o alarme também: o nome cabe numa linha de notificação.
- A aparência da janela de renomear não tem teste automático (o app não usa a biblioteca de teste do Compose); a regra (o que é um nome, qual vale, onde fica guardado, o que o alarme diz) tem.
