# 0015. O limite de emergência do gás muda pelo app e fica guardado na placa

**Estado:** aceita

## Contexto

O limite de gás era fixo em 160 ADC. O contrato já oferece entidades `number`, usadas para os limites de umidade.

## Decisão

- Adicionar `gas-alarm-at` ao monitor de gás, reutilizando o controle numérico, o comando `PUT` e a porta `SettingsStore`.
- Aceitar inteiros de 0 a 1022 na escala ADC de 0 a 1023, em passos de 1, com padrão 160. Excluir 1023 porque nenhuma leitura poderia ultrapassá-lo. A placa valida; ADC não é ppm.
- Guardar o valor em uma chave independente na NVS, sem alterar os limites de umidade existentes. Na partida, ignorar valores salvos inválidos.
- Aplicar no próximo ciclo do controlador. A mudança reinicia a contagem de ar limpo; não muda diretamente o estado, a válvula nem o aquecimento. Uma emergência continua exigindo o botão físico após o período sem gás.
- A linha `alarm` de `gas-level` aponta para a nova entidade. O app usa esse valor no gráfico e permite restaurar o padrão, sem copiar a regra do firmware.
- Incluir o limite na comparação do SSE, para outros clientes receberem a mudança mesmo sem variação do sensor.

## Consequências

É necessário atualizar o firmware e o app para apresentar o controle em português e o botão de restaurar. O app continua aceitando placas antigas com o limite fixo no gráfico. A configuração permanece na placa após uma queda de energia.
