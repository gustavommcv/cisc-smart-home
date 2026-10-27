# 0004. Modelo de dados: cômodo, dispositivo e entidade

**Estado:** aceita

## Contexto

O app deve parecer um aplicativo de casa inteligente que controla vários cômodos e dispositivos, mas a maquete tem só dois cômodos com um sistema cada. O modelo de dados precisa descrever o que existe hoje sem travar o que pode existir amanhã.

## Decisão

Adotar a hierarquia do Home Assistant, que é uma referência de mercado:

- **Cômodo** (`Area`): um lugar da casa.
- **Dispositivo** (`Device`): a **unidade** que se compra e se instala num cômodo, com uma ou mais entidades. Não é cada componente: o monitor do armário é um dispositivo, e o sensor DHT11 e o relé são partes dele. Na maquete, um ESP32 roda os dois dispositivos porque o trabalho pede; numa casa real, cada um seria uma placa.
- **Entidade** (`Entity`): uma informação (sensor) ou um controle (chave) de um dispositivo. Um sensor de temperatura do quarto seria outro dispositivo; os sensores e o interruptor do monitor do armário são entidades dele.

Cada entidade informa o tipo e se aceita comandos. A interface é desenhada a partir disso.

No código, o cômodo se chama `Area` para não colidir com o `Room`, o banco de dados do Android.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Telas e campos fixos para gás e umidade | Mais simples hoje, mas cada dispositivo novo obrigaria a mudar o app e a API |
| Um modelo inventado por nós | Sem ganho: o do Home Assistant foi testado por muitos tipos de casa e é conhecido por quem trabalha na área |

## Consequências

- Um erro de leitura desta decisão já aconteceu: a frase "uma peça dentro de um cômodo" levou a separar o sensor, a válvula e o relé em quatro dispositivos. Por isso a definição acima diz o que é e o que **não** é um dispositivo.
- Um dispositivo novo é só mais dados vindos do ESP32. A tela se adapta.
- Os cartões "Adicionar cômodo" e "Adicionar dispositivo" são **desenhados bloqueados** na interface e **não existem no modelo**. Isso evita código morto.
- O modelo é um pouco mais abstrato do que duas telas fixas, mas ainda pequeno: três conceitos.
- Ver a [descrição completa do modelo](../arquitetura.md#modelo-de-dados).
