# 0011. Os limites de umidade do desumidificador mudam pelo app e ficam guardados na placa

**Estado:** aceita

## Contexto

O desumidificador do monitor do armário liga a partir de uma umidade e desliga abaixo de outra, e esses dois valores estavam escritos no firmware (`Settings.h`): para mudá-los era preciso editar o código e gravar a placa de novo. Na apresentação, e em casa, quem usa o app quer ajustar quanto de umidade é demais.

## Decisão

- **Os dois limites viram entidades que o app pode mudar**, de um tipo novo do contrato, o **`number`**: `dehumidifier-on-at` ("liga a partir de") e `dehumidifier-off-at` ("desliga até"). Um `number` traz `min`, `max`, `step` e `default`, então o app desenha o controle e sabe o caminho de volta **sem conhecer a regra**. O tipo é genérico: qualquer número que se possa ajustar (um limite de gás, por exemplo) será um `number`.
- **O que é permitido** fica no domínio do firmware (`isValid`): números **inteiros**, de **40 a 95 %**, com o de desligar **pelo menos 2 pontos abaixo** do de ligar. A placa recusa o que não serve (`400 invalid-value`), sem mudar nada. Os dois limites se mudam um de cada vez, e por isso a ordem importa (subir: o de ligar primeiro; descer: o de desligar primeiro).
- **A placa guarda o que o usuário escolheu** na NVS, atrás de uma porta (`SettingsStore`), e ao ligar o que estava guardado vale no lugar do valor de fábrica, se ainda for permitido. Uma queda de energia não devolve a placa a 85/83.
- **O padrão mudou de 85/84 para 85/83.** O DHT11 lê porcentagens inteiras, e com 1 ponto de diferença o relé seguiria cada oscilação entre duas leituras (85 liga, 84 desliga). Com 2 pontos, a leitura tem de cair de verdade.
- **Mudar um limite não mexe no modo nem no relé:** o novo valor é o que a próxima leitura (em até 2 s) usa.

## No app

O monitor do armário mostra os dois limites, cada um com "−" e "+" de um passo, e só oferece o passo que a placa vai aceitar (a regra do intervalo está também no domínio do app, só para não oferecer o que seria recusado; **quem decide continua sendo a placa**). "Restaurar padrão" aparece quando os limites não são os de fábrica e manda o `default` de cada um **na ordem em que a placa os aceita**: se o limite de ligar de fábrica está longe o bastante do de desligar de agora, ele vai primeiro, senão vai o de desligar. Um app que não conhece um `number` simplesmente o ignora.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Deixar os limites fixos no firmware | Obriga a regravar a placa para cada ajuste |
| Um comando só, com os dois limites | Complica o contrato (um valor composto) e o app teria de mandar um objeto, em vez de um número por entidade |
| Guardar os limites só no app | O app não decide o relé: quem decide é a placa, que tem de lembrar do valor mesmo sem o celular |
| Não guardar (voltam ao padrão ao reiniciar) | Uma queda de energia mudaria o comportamento sem ninguém saber |
| Aceitar decimais | O sensor lê inteiros, e os decimais só dariam a impressão de uma precisão que não existe |

## Consequências

- **É preciso regravar a placa** uma vez para ganhar o recurso, e os 2 novos valores de umidade aparecem no app.
- Um app antigo **ignora** as entidades novas (o app descarta o que não conhece), então nada quebra.
- A memória NVS tem um número limitado de gravações, mas cada toque do usuário grava dois bytes, e ninguém muda um limite milhares de vezes.
- O número 2 do intervalo mínimo e a faixa de 40 a 95 % valem para a maquete. Outro sensor pediria outros valores, que moram num lugar só (`Settings.h`).
