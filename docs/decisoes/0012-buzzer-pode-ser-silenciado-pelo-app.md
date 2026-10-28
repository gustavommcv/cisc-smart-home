# 0012. O buzzer da placa pode ser silenciado pelo app, e o silêncio não fica guardado

**Estado:** aceita

## Contexto

Na maquete, o buzzer apita durante o vazamento e incomoda em testes e demonstrações. O usuário pediu poder ligar e desligar o buzzer físico pelo app, como já pode escolher como o celular avisa (decisão [0006](0006-alertas-por-notificacao-local.md)).

## Decisão

- **O buzzer é um interruptor** (`buzzer-enabled`, do tipo `switch`, que já existe no contrato) no dispositivo `gas-monitor`. Desligado, o buzzer **não soa**; ligado, soa como sempre, no vazamento.
- **Só o buzzer fica mudo.** O LED de alarme, a válvula, o estado de emergência, a contagem de vazamentos e o evento `alert` (e, com ele, o aviso no celular) não dependem do interruptor. A segurança da casa, que é fechar a válvula, continua sendo da placa.
- **Vale na hora**, inclusive no meio de um vazamento: silenciar para o buzzer, ligar de novo o faz soar se o gás ainda está lá.
- **O silêncio não fica guardado**, ao contrário dos limites de umidade ([0011](0011-limites-de-umidade-mudam-pelo-app.md)): se a placa reiniciar, o buzzer volta ligado. Uma queda de energia, ou um reinício, nunca pode deixar a placa muda sem ninguém saber.
- **No app, desligar pede confirmação**, e a linha diz que só o buzzer fica mudo e que ele volta a ligar sozinho se a placa reiniciar.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Guardar o silêncio na placa, como os limites | Uma placa que reinicia sem ninguém ver ficaria sem apito num vazamento |
| Um botão físico de silêncio | Não há espaço na maquete, e o pedido era pelo app |
| Silenciar também o LED e o evento `alert` | O LED e o alerta são o que o resto da casa vê; só o som incomoda |

## Consequências

- O interruptor muda o contrato (uma entidade nova) e exige regravar a placa. Um app antigo mostra o buzzer como um interruptor comum e o ignora na tela de aviso.
- O Serial da placa passa a dizer `buzzer=on` ou `buzzer=muted` em cada linha, para ver o estado sem o app.
- Quem silencia o buzzer e esquece não fica sem o aviso no celular; mas **sem o buzzer, quem está perto da placa e sem o celular não escuta nada**. É por isso que o app pergunta antes.
