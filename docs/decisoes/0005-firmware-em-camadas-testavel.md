# 0005. Firmware em camadas, com o domínio testável no computador

**Estado:** aceita

## Contexto

A primeira versão da maquete era um único sketch do Arduino. Funcionou, mas para testar qualquer regra era preciso a placa e, para alguns casos (gás acima do limite), um isqueiro. Queremos desenvolver com testes escritos antes do código.

## Decisão

- Separar o firmware em **domínio**, **aplicação**, **adaptadores** e **API**. A regra de dependência aponta para dentro: o domínio não conhece Arduino, pinos nem rede.
- O hardware entra por **interfaces** pequenas, e `main.cpp` é o único lugar que monta as classes concretas.
- Usar o **PlatformIO**, que organiza o projeto em pastas e roda **testes unitários no computador** (ambiente `native`, com o Unity), sem a placa.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Continuar com sketches e o `arduino-cli` | Funciona para um arquivo, mas não tem testes no computador nem organização de pastas |
| ESP-IDF puro | Mais controle do que precisamos e uma curva de aprendizado maior |
| ESPHome | Esconde o código em YAML. A atividade pede o código do microcontrolador |

## Consequências

- Regras do gás, da umidade, do modo Automático/Manual e do histórico são testadas em segundos, com o computador simulando sensores.
- Só os adaptadores (a parte que fala com os pinos) precisam da placa para testar.
- Há um pouco mais de arquivos do que num sketch único. É o custo de poder explicar e testar cada parte.
- A regra de segurança do gás roda no ESP32 e **não depende do celular**.
- O `Controle.h` da maquete atual é o ponto de partida do domínio.
