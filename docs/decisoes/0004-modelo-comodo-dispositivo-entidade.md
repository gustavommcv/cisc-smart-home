# 0004. Modelo de dados: cômodo, dispositivo e entidade

**Estado:** aceita

## Contexto

O app deve parecer um aplicativo de casa inteligente que controla vários cômodos e dispositivos, mas a maquete tem só dois cômodos com um sistema cada. O modelo de dados precisa descrever o que existe hoje sem travar o que pode existir amanhã.

## Decisão

Adotar a hierarquia do Home Assistant, que é uma referência de mercado:

- **Cômodo** (`Area`): um lugar da casa.
- **Dispositivo** (`Device`): uma peça dentro de um cômodo.
- **Entidade** (`Entity`): uma informação (sensor) ou um controle (chave) de um dispositivo.

Cada entidade informa o tipo e se aceita comandos. A interface é desenhada a partir disso.

No código, o cômodo se chama `Area` para não colidir com o `Room`, o banco de dados do Android.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Telas e campos fixos para gás e umidade | Mais simples hoje, mas cada dispositivo novo obrigaria a mudar o app e a API |
| Um modelo inventado por nós | Sem ganho: o do Home Assistant foi testado por muitos tipos de casa e é conhecido por quem trabalha na área |

## Consequências

- Um dispositivo novo é só mais dados vindos do ESP32. A tela se adapta.
- Os cartões "Adicionar cômodo" e "Adicionar dispositivo" são **desenhados bloqueados** na interface e **não existem no modelo**. Isso evita código morto.
- O modelo é um pouco mais abstrato do que duas telas fixas, mas ainda pequeno: três conceitos.
- Ver a [descrição completa do modelo](../arquitetura.md#modelo-de-dados).
