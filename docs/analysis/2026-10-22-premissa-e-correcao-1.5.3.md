# Chunk In A Globe 1.5.3 — correção da premissa

Alvo confirmado no projeto: Minecraft 1.21.1, NeoForge 21.1.259, Java 21, mappings Mojang/Parchment. Nesta revisão, as APIs foram conferidas pela compilação e pelo source jar local do NeoForge; não houve nova consulta MCP/web.

## Comportamento esperado

Um globo possui uma sala própria na dimensão interna. O jogador constrói nessa sala e, ao sair, vê os blocos e entidades reais da sala em miniatura em um único bloco externo. Carregar e recolocar o mesmo globo mantém o ID e a construção. A área útil atual é 15³, com paredes/fundo/teto num contorno de 17³ e escala externa de 1/16.

O mundo externo pode ser mostrado como cenário visual aumentado ao redor da sala, sem ser copiado para ela. O bloco externo que contém o globo corresponde à sala inteira e deve ser omitido desse cenário. A captura de estados serve somente ao cliente para desenhar; não modifica os blocos da sala.

## Erros corrigidos

- Removido `GlobeWorldMirror` e suas chamadas na entrada e atualização periódica. Essa rotina podia sobrescrever construções reais a cada segundo.
- A saída volta ao centro do piso, `(8,0,8)`. Apenas o antigo bloco de saída experimental em `(8,8,8)`, quando identificado como o mesmo globo, é removido. O restante do volume não é limpo.
- Restaurado o cenário externo de leitura com escala 16. Sua transformação relativa ao bloco de saída é `(-135.5,-128,-135.5) + 16 * posiçãoLocal`. O bloco central local `(8,8,8)` é omitido. Assim os blocos externos vizinhos ficam além dos limites da sala, deixando o volume de construção livre.
- A miniatura colocada usa a iluminação recebida na posição do globo, em vez de consultar posições locais como `(1,1,1)` no mundo externo. O tint de biome também usa a posição de exibição.
- O cenário externo visto de dentro usa luz fixa nesta etapa, pois o payload ainda não transmite iluminação da região externa. Não equivale a reproduzir noite/iluminação remota.
- A geometria de líquidos passa pela matriz da miniatura/cenário. `LiquidBlockRenderer` emite vértices locais da seção sem aplicar PoseStack; era necessário aplicar a transformação explicitamente e evitar somar duas vezes a posição do bloco.
- Mantidas a proteção das paredes e a checagem de limite do servidor. A saída intencional continua sendo clicar no bloco central do piso.

## Dados da build experimental anterior

Os blocos já copiados pela build anterior não podem ser distinguidos com segurança de blocos colocados pelo jogador. A correção interrompe a cópia, mas não apaga esses blocos automaticamente. Um globo realmente novo, sem ID anterior, começa vazio. Recolher e recolocar um globo antigo mantém seu conteúdo, inclusive o terreno já copiado.

## Verificação

- GameTests em pasta isolada `run-gametest`: cinco testes passaram, incluindo sala nova vazia e conservação de blocos construídos/baú com sete esmeraldas ao preparar novamente a sala e alterar o retorno/base.
- O GameTestServer desta versão cria apenas dimensões vanilla. Os dois testes de sala executam o método real de preparação em um ServerLevel real de teste; não provam, por si, o teleporte entre dimensões nem um ciclo de salvar/reabrir mundo.
- Oito testes JUnit passaram: alinhamento externo, contexto de luz/tint, coordenadas dos líquidos e limites da região. Relatórios em `build/test-results/test`.
- A reintrodução temporária dos caminhos defeituosos de escala, luz e líquidos fez cinco dos seis testes selecionados falharem. As fontes foram restauradas; a execução final dos oito testes e o empacotamento passaram. Logs: `regression-failure-capability-1.5.3.log` e `verification-final-1.5.3.log`.
- A captura gráfica depois de carregar o JAR 1.5.3 permanece necessária para confirmar o resultado visual. Testes de geometria e contexto de luz não equivalem à inspeção do quadro final no jogo.
