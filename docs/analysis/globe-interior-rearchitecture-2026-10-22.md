# Reavaliação do interior do Globe — 22/10/2026

> SUPERADO pela correção 1.5.3. A decisão abaixo de copiar blocos externos para a dimensão era incorreta: o interior é uma sala de construção independente. Essa cópia foi removida. Ver [premissa e correção 1.5.3](2026-10-22-premissa-e-correcao-1.5.3.md). Este documento fica apenas como histórico da regressão.

Alvo: Minecraft 1.21.1 · NeoForge 21.1.259

## Evidência local

- Capturas: [falha da âncora](captures/2026-10-22-interior-base-anchor-regression.png) e [saída da região](captures/2026-10-22-interior-escape-regression.png).
- O shell ocupa `origin + [0,16]` nos três eixos, mas a saída estava em `origin + (8,0,8)`. Ao mesmo tempo, o snapshot de blocos usa posições locais de `1..15` e o render interno subtrai `8` dos três eixos. Isso colocou a origem vertical do terreno no piso, embora a cena devesse ficar centralizada ao redor do jogador. A nova referência usa uma saída no centro `(8,8,8)` e deixa o piso no limite inferior do cubo.
- A implementação original bloqueava quebrar `Barrier` e o bloco `Globe` na dimensão interna e bloqueava colocar itens Barrier ali. O port NeoForge não tinha esses handlers. Os logs mostram o ID 5 iniciando em `z≈8008` e depois chegando a `x<0`, `x>16`, `y<128` e `z<8000` enquanto ainda estava na dimensão interna. A captura mostra o item Barrier selecionado, que ativa os ícones vermelhos vanilla para blocos Barrier. A proteção omitida é a causa concreta mais provável da fuga; a ausência de limite do servidor permitia continuar fora do cubo. A alteração de coordenadas absolutas enviadas com flags `RelativeMovement` também foi removida.
- O renderer atual redesenha até 3.375 estados por quadro no `BlockEntityRenderer`. Além disso, `GlobeRenderView.getBrightness()` consulta o nível atual usando coordenadas locais `1..15`, embora os dados venham de outra região/dimensão. Isso mistura o snapshot com luz de posições não relacionadas.

## Alternativas comparadas

| Estratégia | Vantagens | Custos / riscos | Decisão |
|---|---|---|---|
| Manter o desenho por bloco no `BlockEntityRenderer` | Alteração pequena; funciona como miniatura | Desenha milhares de modelos a cada quadro; difícil acertar luz, tint, culling, fluids, block entities e câmera; já reproduziu as falhas atuais | Não usar para o interior |
| Segunda `LevelRenderer` + framebuffer/portal | Usa render de chunks, entidades, biome e câmera remota | Arquitetura de portal completa: troca de câmera, level, renderer, lightmap, buffers, frustum, shaders e restauração de estado. Immersive Portals documenta stencil/FBO e clipping; sua implementação 1.21 troca vários estados globais e mantém compatibilidade específica com Sodium/Iris | Boa qualidade potencial, desproporcional e arriscada para este mod |
| Copiar o volume capturado para a própria dimensão do globo | Usa o render normal de chunks; reduz pipeline custom; luz, faces ocultas, fluids e culling passam pelo jogo; área limitada a 15³ por globo | Precisa sincronizar estados, decidir como tratar block entities/biomes e proteger as paredes; entidades continuam em uma trilha própria | Caminho escolhido para o interior |

## Novo modelo espacial proposto

1. Manter um volume interno de 17³, com parede/barreira no contorno.
2. Colocar o bloco de saída no centro `(8,8,8)` e o jogador um bloco acima `(8.5,9,8.5)`.
3. Espelhar estados capturados em `origin+(x,y,z)`, para `1≤x,y,z≤15`; o bloco Globe capturado no centro é substituído pelo bloco de saída.
4. Manter o piso visual no fundo do cubo, fora do volume do snapshot, e deixar a vista interna de blocos para o renderizador normal de chunks.
5. Reaplicar apenas estados que mudaram, a cada atualização de snapshot; manter a prévia externa no renderizador de miniatura existente.
6. Restaurar proteção de quebra de Barrier/Globe e colocação de Barrier, e rejeitar no servidor posições de jogador fora do cubo daquela instância. Interagir com o bloco de saída continua sendo o retorno intencional.

## Pesquisa externa

- A documentação técnica do Immersive Portals descreve renderização recursiva com stencil ou framebuffer separado, clipping do plano e troca/restauração do estado de render. [Detalhes da implementação](https://github.com/iPortalTeam/ImmersivePortalsMod/wiki/Implementation-Details/d19625715e0ee61eafa2497630c512e11f0d1c86)
- O código 1.21 do Immersive Portals troca `ClientLevel`, `LevelRenderer`, lightmap, câmera, buffers, frustum e estado Sodium antes de chamar `renderLevel`, e restaura esses componentes depois. [MyGameRenderer.java](https://github.com/iPortalTeam/ImmersivePortalsMod/blob/1.21/src/main/java/qouteall/imm_ptl/core/render/MyGameRenderer.java)
- Uma análise de compatibilidade do pipeline secundário registra dependência de internals como `SectionOcclusionGraph`, `ViewArea` e `SectionRenderDispatcher`, com caminhos específicos para Sodium/Iris. [Plano de compatibilidade](https://github.com/downloadableduck/ImmersivePortals/blob/26.1.x/compat-analysis/COMPAT_PLAN.md)
- A documentação da NeoForge confirma que `BlockEvent.BreakEvent` ocorre no servidor e pode cancelar a quebra. [Javadoc 1.21.1](https://lexxie.dev/neoforge/1.21.1/net/neoforged/neoforge/event/level/BlockEvent.BreakEvent.html)
- A documentação da Fabric descreve BERs como desenho por quadro e modelos de chunk como geometria refeita quando o chunk é atualizado; isso apoia mover geometria estática para o render de chunk. [Renderização dinâmica](https://wiki.fabricmc.net/tutorial:dynamic_block_rendering)

## Limites conhecidos da primeira etapa

Copiar block states permite ao renderer normal desenhar os blocos e fluidos. Dados de block entity, biome original e renderers especiais não são automaticamente transferidos pelo `BlockState`; esses itens devem ser tratados como uma etapa separada, com política explícita, em vez de continuar fingindo que o snapshot é um `Level` completo.
