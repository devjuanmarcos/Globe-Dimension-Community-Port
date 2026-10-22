# Pesquisa: renderização interna do Globe e Porting Dead Libs

Data: 22/10/2026
Alvo: Minecraft 1.21.1, NeoForge 21.1.259

## Resumo executivo

O Porting Dead Libs (PDL) não é uma biblioteca geral de portabilidade nem uma solução para a renderização do interior do Chunk In A Globe. É uma biblioteca de suporte para mods do grupo Porting Dead Mods. A versão `1.1.8-1.21.1` existe para NeoForge, mas sua API pública documentada se concentra em multiblocos, wrappers de BlockEntity/menu/tela, fluidos, widgets e utilitários. A pesquisa do código da versão 1.21.1 encontrou helpers de renderização de preview que recorrem a `renderSingleBlock`; isso não fornece a visão contextual de mundo que falta ao renderer do Globe. Adicionar PDL como dependência não resolveria o problema identificado e criaria uma dependência adicional.

A direção mais promissora continua sendo renderizar o snapshot do Globe como uma seção contextual local: estados vizinhos, posição correta, biome/tint, luz, fluidos e, em etapa própria, dados/model data de BlockEntities. Antes de atribuir o crash à visualização interna, precisamos de um crash report gerado dentro dela: os arquivos recebidos até agora comprovam outro crash, no renderer do item no inventário.

## O que os arquivos comprovam

O crash de 21/10/2026 às 18:32 é `Rendering item`. O item `globedimension:globe` usava `base_block=securitycraft:reinforced_white_wool`; a exceção acontece no color handler do SecurityCraft, que tenta buscar uma BlockEntity com `level=null`, através de `BlockColors.getColor` e `BlockRenderDispatcher.renderSingleBlock`. Isso comprova a falha do desenho da base do item sem contexto de mundo. Não comprova um crash ao entrar no interior.

O renderer atual do interior usa `GlobeRenderView`, que expõe os estados capturados e vizinhos para a tesselação, mas retorna `null` em `getBlockEntity`. A tesselação chama `renderBatched` com `ModelData.EMPTY`; entidades são encaminhadas aos renderers registrados pelos mods. Portanto, ainda é possível que algum bloco ou renderer modificado dependa de BlockEntity/model data ou de um mundo real. Isso é uma hipótese de risco baseada no código, não uma causa confirmada do crash interno.

Também há uma divergência concreta para revisar: a análise anterior registra que o deslocamento central extra `(-7.5, 0, -7.5)` foi removido, mas o código atual ainda fornece esses offsets em `GlobeRenderTransform.forInterior()` e os aplica antes da escala na cena. O renderer também aplica `(-7.5, 0, -7.5)` separadamente ao desenhar a base ampliada. Isso pode explicar geometria deslocada, mas não é evidência de crash. Deve ser conferido contra a posição da saída e a convenção das coordenadas antes de qualquer remoção, para não alterar o alinhamento às cegas.

## Porting Dead Libs: utilidade para este port

- CurseForge lista PDL como biblioteca para mods do próprio Porting Dead Mods, cliente/servidor, apenas NeoForge, com suporte a 1.21.1. O arquivo indicado para essa versão é `portingdeadlibs-1.1.8-1.21.1.jar`.
- O README/código do projeto é orientado a APIs compartilhadas pelos mods do autor, incluindo multiblocos, wrappers de BlockEntity/menu/tela, fluidos, widgets e dados salvos.
- O código consultado de preview de multiblocos e widget usa renderização de bloco isolado/`renderSingleBlock`, com contexto limitado. Não oferece um `BlockAndTintGetter` baseado no snapshot, armazenamento de BlockEntity do Globe ou tesselação completa de uma seção capturada.
- Assim, PDL pode ser útil se futuramente o Globe reutilizar diretamente uma API específica sua; não ajuda de forma material a corrigir este renderer hoje. Recomendação: não adicionar a dependência ao projeto para este problema.
- Há diferença entre a licença informada nos metadados do CurseForge (MIT) e a licença no arquivo `LICENSE` da tag 1.1.8 no repositório (Common Sense License / CSL). Isso não impede por si só o uso normal do JAR como dependência, mas a licença deve ser esclarecida antes de copiar ou incorporar código-fonte do PDL.

## Hipóteses de investigação, em ordem

1. **Bloco capturado depende de BlockEntity/model data.** Falsificável: o stack trace deve apontar para `getBlockEntity`, `getModelData`, renderer/modelo de um bloco identificado, ou o problema deve desaparecer ao substituir esse bloco por um bloco vanilla equivalente. O snapshot atual não transporta BEs e fornece `ModelData.EMPTY`.
2. **Renderer de entidade capturada ou reentrada recursiva.** Falsificável: o stack deve conter o renderer da entidade, `EntityRenderDispatcher`, o BER do Globe ou ultrapassar a guarda `renderDepth`. O código chama renderers de entidades dentro do BER e usa uma guarda de profundidade; não há evidência atual ligando isso ao crash recebido.
3. **Mapa de posição de biome/luz está incorreto.** O `GlobeRenderView` consulta tint e brilho diretamente na posição local do snapshot na dimensão real, sem traduzir para a origem do snapshot. Isso tende a gerar cores/iluminação erradas, especialmente em biomas, e é hipótese de defeito visual, não de crash.
4. **Offsets da cena não correspondem à saída.** O transform atual aplica deslocamentos extras de centralização. Comparar coordenadas da saída, origem capturada e âncora do BER em uma cena conhecida antes de alterar; provável defeito de alinhamento, não explicação para exceção.

## Caminho recomendado

1. Reproduzir entrando no interior e guardar `latest.log` completo e o `crash-...-client.txt` correspondente. Registrar também o bloco/entidade mais recentemente colocado na área exibida e os mods gráficos ativos.
2. Usar a primeira classe/mod do stack trace para isolar o gatilho. Não desabilitar renderers ou mods por suposição.
3. Completar o contexto do snapshot: mapear posição local para a origem capturada ao consultar tint/biome/luz; validar faces vizinhas e layers; tratar FluidState explicitamente.
4. Para blocos com BlockEntity, decidir um formato limitado e seguro de sincronização dos dados visuais necessários e obter `ModelData` com a visão contextual. Nem todo NBT de BlockEntity deve ser transmitido sem filtro.
5. Avaliar entidades capturadas separadamente, inclusive o estado interpolado e as condições esperadas pelos renderers de terceiros.
6. Corrigir a transformação de coordenadas somente depois de documentar o mapeamento origem → local → coordenada do BER e comparar com a saída física.

A documentação NeoForge 1.21.1 confirma que modelos podem consumir `ModelData` derivado de BlockEntity ou de contexto de bloco e que BERs podem invocar modelos repetidamente por frame. Isso sustenta a necessidade de fornecer contexto coerente e evitar fazer o renderer de miniature agir como uma renderização de item isolado.

## Fontes

- [Porting Dead Libs no CurseForge](https://www.curseforge.com/minecraft/mc-mods/porting-dead-libs/files/all) — loader, versões, arquivo 1.21.1 e metadados de licença.
- [Código-fonte do Porting Dead Libs, branch 1.21.1](https://github.com/Porting-Dead-Mods/Porting-Dead-Libs/tree/1.21.1) — escopo das APIs e helpers de preview.
- [Licença na tag 1.1.8](https://github.com/Porting-Dead-Mods/Porting-Dead-Libs/blob/1.1.8/LICENSE) — diverge dos metadados de CurseForge.
- [NeoForge 1.21.1: Baked Models](https://docs.neoforged.net/docs/1.21.1/resources/client/models/bakedmodel/) — `ModelData`, contexto de modelo e chamadas a partir de BERs/renderers de entidades.
- [NeoForge 1.21.1: BlockEntityRenderer e BEWLR](https://docs.neoforged.net/docs/1.21.1/blockentities/ber/) — distinção entre renderização no mundo e renderização de item sem Level.
- Relatórios locais: `analysis/crash-2026-10-21-18-32.md`, `analysis/render-risk-audit-2026-10-21.md`.
- Código local: `GlobeRenderView`, `GlobeBlockEntityRenderer`, `GlobeRenderTransform` e `GlobeBlockModelRenderer` no port NeoForge 1.21.1.

## Limite desta rodada

Esta entrega é pesquisa e diagnóstico estático. Não houve alteração de código nem execução de testes. O crash do interior permanece sem stack trace específico; logo, as causas acima estão classificadas como hipóteses até recebermos o relatório correto.
